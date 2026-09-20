package com.justnothing.testmodule.command.framework.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Arrays;

/**
 * 从字节流里"凑"出完整的协议帧。
 *
 * <p>存在的理由：{@link InteractiveProtocol} 的帧是「9 字节包头 + 数据体 + 4 字节包尾」，
 * 而 TCP 只保证字节流、不保证消息边界。旧实现 {@code InteractiveProtocol.readMessage} 用
 * 一个静态方法分三段阻塞读取，进度保存在<b>局部变量</b>里——一旦中途抛
 * {@link java.net.SocketTimeoutException}（读超时），已经读到的包头和部分数据就随异常一起丢了，
 * 流里的位置却已经前进，下次再读就把剩余字节当成新包头，必然对不上标记。</p>
 *
 * <p>本类的做法是"凑够再消费"：</p>
 * <ul>
 *   <li>内部持有一个<b>跨调用存活</b>的缓冲区，读到的字节先追加进来；</li>
 *   <li>只有在<b>确认整帧都到齐</b>（包头、长度、包尾都校验通过）之后，才把字节从缓冲区移走；</li>
 *   <li>数据没到齐（包括读超时）就返回 {@code null}，此时缓冲区与解析进度<b>原封不动</b>，
 *       下次调用可以接着来。</li>
 * </ul>
 * <p>因此读超时从"会破坏协议状态"变成了"这次没读到新数据而已"，完全无害。</p>
 *
 * <p>本类<b>不是线程安全</b>的：一个连接一个实例，只在读取线程里使用。</p>
 */
public final class FrameReader {

    /** 一帧数据体的长度上限，与旧实现保持一致（1MB）。 */
    private static final int MAX_FRAME_PAYLOAD = 1024 * 1024;

    /** 包头：起始标记(4) + 类型(1) + 数据长度(4)。 */
    private static final int HEADER_SIZE = 9;

    /** 包尾：结束标记(4)。 */
    private static final int TRAILER_SIZE = 4;

    /** 缓冲区容量上限 = 最长可能的一帧。 */
    private static final int MAX_BUFFERED = HEADER_SIZE + MAX_FRAME_PAYLOAD + TRAILER_SIZE;

    /** 首次分配与每次扩容的步长。 */
    private static final int INITIAL_BUFFER_SIZE = 8192;

    private final InputStream input;

    private byte[] buffer = new byte[INITIAL_BUFFER_SIZE];

    /** 缓冲区里已填充的字节数。 */
    private int size;

    /** 底层流是否已经读到结尾。 */
    private boolean endOfStream;

    public FrameReader(InputStream input) {
        this.input = input;
    }

    /**
     * 尝试取出一帧。
     *
     * <p>缓冲区里已有完整帧时直接返回，不会再去阻塞读，因此连续多帧不会被人为拖慢。</p>
     *
     * @return {@code [Byte type, byte[] data]}；数据还没到齐时返回 {@code null}（可继续调用）
     * @throws IOException 流在帧中间被截断，或标记 / 长度非法
     */
    public Object[] tryReadFrame() throws IOException {
        Object[] frame = extractBufferedFrame();
        if (frame != null) {
            return frame;
        }
        if (isEndOfStream()) {
            return null;
        }

        fillFromStream();
        return extractBufferedFrame();
    }

    /**
     * 底层流是否已在帧边界处干净结束。
     *
     * <p>注意：如果流是在<b>帧中间</b>结束的，{@link #tryReadFrame()} 会抛 IOException，
     * 而不是走到这里。</p>
     */
    public boolean isEndOfStream() {
        return endOfStream && size == 0;
    }

    /** 缓冲区里是否还留着没消费完的字节（正常情况下应为 false）。 */
    public int bufferedBytes() {
        return size;
    }

    /**
     * 只从缓冲区里尝试解出一帧，不做任何读取。
     * 整帧到齐才消费字节；不完整就原样留着。
     */
    private Object[] extractBufferedFrame() throws IOException {
        if (size < HEADER_SIZE) {
            if (endOfStream && size > 0) {
                throw new IOException("帧被截断：流已结束，但只收到 " + size + " 字节包头（需要 " + HEADER_SIZE + "）");
            }
            return null;
        }

        for (int i = 0; i < 4; i++) {
            if (buffer[i] != InteractiveProtocol.START_MARKER[i]) {
                throw new IOException("无效的起始标记: " + toHex(buffer, 0, 4));
            }
        }

        byte type = buffer[4];
        int dataLength = ((buffer[5] & 0xFF) << 24)
                | ((buffer[6] & 0xFF) << 16)
                | ((buffer[7] & 0xFF) << 8)
                | (buffer[8] & 0xFF);

        if (dataLength < 0 || dataLength > MAX_FRAME_PAYLOAD) {
            throw new IOException("无效的数据长度: " + dataLength);
        }

        int frameLength = HEADER_SIZE + dataLength + TRAILER_SIZE;
        if (size < frameLength) {
            if (endOfStream) {
                throw new IOException("帧被截断：期望 " + frameLength + " 字节，流结束时只有 " + size);
            }
            return null;
        }

        int endMarkerStart = HEADER_SIZE + dataLength;
        for (int i = 0; i < 4; i++) {
            if (buffer[endMarkerStart + i] != InteractiveProtocol.END_MARKER[i]) {
                throw new IOException("无效的结束标记: " + toHex(buffer, endMarkerStart, 4));
            }
        }

        byte[] data = dataLength > 0
                ? Arrays.copyOfRange(buffer, HEADER_SIZE, HEADER_SIZE + dataLength)
                : null;
        consume(frameLength);
        return new Object[]{type, data};
    }

    /**
     * 从底层流里读一次，尽量多读。
     *
     * <p>读超时不算错误——它只表示"此刻没有更多数据"，已有缓冲区与解析进度全部保留。</p>
     */
    private void fillFromStream() throws IOException {
        if (endOfStream) {
            return;
        }

        if (size == buffer.length) {
            if (buffer.length >= MAX_BUFFERED) {
                throw new IOException("缓冲区已满(" + size + " 字节)仍凑不出完整帧，帧长度超过上限");
            }
            buffer = Arrays.copyOf(buffer, Math.min(buffer.length * 2, MAX_BUFFERED));
        }

        try {
            int n = input.read(buffer, size, buffer.length - size);
            if (n == -1) {
                endOfStream = true;
            } else {
                size += n;
            }
        } catch (InterruptedIOException e) {
            // SocketTimeoutException 也走这里：本次没有新数据，不是错误
        }
    }

    /** 把已消费的 {@code n} 字节从缓冲区头部移走，剩余字节前移。 */
    private void consume(int n) {
        if (n >= size) {
            size = 0;
            return;
        }
        System.arraycopy(buffer, n, buffer, 0, size - n);
        size -= n;
    }

    private static String toHex(byte[] bytes, int offset, int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = offset; i < offset + length && i < bytes.length; i++) {
            sb.append(String.format("%02X ", bytes[i]));
        }
        return sb.toString().trim();
    }
}
