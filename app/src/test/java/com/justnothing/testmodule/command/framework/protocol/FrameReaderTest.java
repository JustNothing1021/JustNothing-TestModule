package com.justnothing.testmodule.command.framework.protocol;

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * {@link FrameReader} 单测（纯 Java，无需 Android 运行时）。
 *
 * <p>核心回归点：TCP 是字节流、没有消息边界，且读超时会在"帧读到一半"时发生。
 * 旧实现把读取进度放在局部变量里，一次读超时就会丢失进度并导致后续字节错位。
 * 这里用两种畸形流把该场景固化成测试：</p>
 * <ul>
 *   <li>每次只喂 1 个字节（极端分片）</li>
 *   <li>在帧中间抛一次 {@link SocketTimeoutException}（模拟 1 秒读超时）</li>
 * </ul>
 */
public class FrameReaderTest {

    private static final byte TYPE = InteractiveProtocol.TYPE_RPC;

    private static byte[] frame(String payload) {
        return InteractiveProtocol.encodeMessage(TYPE, payload.getBytes(StandardCharsets.UTF_8));
    }

    private static String payloadOf(Object[] frame) {
        assertNotNull("本应解出一帧，实际得到 null", frame);
        assertEquals("帧类型不符", TYPE, (byte) frame[0]);
        byte[] data = (byte[]) frame[1];
        return data == null ? "" : new String(data, StandardCharsets.UTF_8);
    }

    @Test
    public void 一次读完一帧() throws IOException {
        FrameReader reader = new FrameReader(new FragmentedInputStream(frame("hello"), Integer.MAX_VALUE, -1));

        assertEquals("hello", payloadOf(reader.tryReadFrame()));
        // 取完一帧时还没观察到流结尾，要再取一次让 reader 读到 EOF
        assertNull(reader.tryReadFrame());
        assertTrue("读到流结尾后应处于干净结束状态", reader.isEndOfStream());
        assertNull(reader.tryReadFrame());
    }

    @Test
    public void 每次只喂一个字节也能凑齐() throws IOException {
        FrameReader reader = new FrameReader(new FragmentedInputStream(frame("逐字节喂入"), 1, -1));

        Object[] packet = null;
        // 字节是分片到达的，中途必须返回 null 而不是抛异常或解出半帧
        for (int i = 0; i < 200 && packet == null; i++) {
            packet = reader.tryReadFrame();
        }

        assertEquals("逐字节喂入", payloadOf(packet));
    }

    @Test
    public void 读到一半读超时不会破坏解析状态() throws IOException {
        byte[] bytes = frame("超时不该丢进度");
        // 先喂 7 字节（连包头 9 字节都没喂完），然后抛一次读超时，之后正常喂完
        FrameReader reader = new FrameReader(new FragmentedInputStream(bytes, 7, 7));

        // 前几次调用应该吃下超时并返回 null —— 这是旧实现会在这里丢进度的位置
        Object[] packet = null;
        for (int i = 0; i < 100 && packet == null; i++) {
            packet = reader.tryReadFrame();
        }

        assertEquals("超时不该丢进度", payloadOf(packet));
    }

    @Test
    public void 粘包时能连续取出多帧() throws IOException {
        ByteArrayOutputStream joined = new ByteArrayOutputStream();
        joined.write(frame("frame-1"));
        joined.write(frame("frame-2"));
        joined.write(frame("frame-3"));

        FrameReader reader = new FrameReader(
                new FragmentedInputStream(joined.toByteArray(), Integer.MAX_VALUE, -1));

        assertEquals("frame-1", payloadOf(reader.tryReadFrame()));
        assertEquals("frame-2", payloadOf(reader.tryReadFrame()));
        assertEquals("frame-3", payloadOf(reader.tryReadFrame()));
        assertNull(reader.tryReadFrame());
        assertTrue(reader.isEndOfStream());
    }

    @Test
    public void 帧中间流结束应该报错而不是当作正常结束() {
        byte[] bytes = frame("这帧会被截断");
        byte[] truncated = Arrays.copyOf(bytes, bytes.length - 5);

        FrameReader reader = new FrameReader(new FragmentedInputStream(truncated, Integer.MAX_VALUE, -1));

        // 旧实现在这里返回 null，调用方会误判成"服务器已正常关闭连接"。
        // tryReadFrame 不阻塞，所以要多调几次才会观察到流结尾。
        assertThrows(IOException.class, () -> {
            for (int i = 0; i < 5; i++) {
                if (reader.tryReadFrame() == null && reader.isEndOfStream()) {
                    fail("流在帧中间结束时应当抛 IOException，而不是被当作干净结束");
                }
            }
        });
    }

    @Test
    public void 起始标记非法应该报错() {
        byte[] bytes = frame("payload");
        bytes[0] = (byte) 0xFF; // 破坏起始标记

        FrameReader reader = new FrameReader(new FragmentedInputStream(bytes, Integer.MAX_VALUE, -1));

        assertThrows(IOException.class, reader::tryReadFrame);
    }

    @Test
    public void 空数据体的帧也能正确解出() throws IOException {
        FrameReader reader = new FrameReader(new FragmentedInputStream(frame(""), Integer.MAX_VALUE, -1));

        Object[] packet = reader.tryReadFrame();
        assertNotNull(packet);
        assertEquals(TYPE, (byte) packet[0]);
        assertNull("空载荷应解出 null 数据体", packet[1]);
        assertNull(reader.tryReadFrame());
        assertTrue(reader.isEndOfStream());
    }

    @Test
    public void 尚未到齐时不应消费任何字节() throws IOException {
        byte[] bytes = frame("半帧");
        // 只喂一半
        FrameReader reader = new FrameReader(new FragmentedInputStream(bytes, bytes.length / 2, -1));

        assertNull(reader.tryReadFrame());
        int buffered = reader.bufferedBytes();
        assertTrue("不完整的帧必须原样留在缓冲区里", buffered > 0 && buffered < bytes.length);
        assertFalse(reader.isEndOfStream());
    }

    /**
     * 可精确控制分片粒度、并能在指定位置模拟一次读超时的假流。
     *
     * @param chunkSize 每次 {@code read} 最多交付多少字节
     * @param timeoutAt 已交付字节数达到该值后，下一次 read 抛一次 SocketTimeoutException；-1 表示不模拟
     */
    private static final class FragmentedInputStream extends InputStream {

        private final byte[] data;
        private final int chunkSize;
        private final int timeoutAt;
        private int pos;
        private boolean timeoutFired;

        FragmentedInputStream(byte[] data, int chunkSize, int timeoutAt) {
            this.data = data;
            this.chunkSize = chunkSize;
            this.timeoutAt = timeoutAt;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (!timeoutFired && timeoutAt >= 0 && pos >= timeoutAt) {
                timeoutFired = true;
                throw new SocketTimeoutException("模拟读超时");
            }
            if (pos >= data.length) {
                return -1;
            }
            int n = Math.min(Math.min(chunkSize, len), data.length - pos);
            System.arraycopy(data, pos, b, off, n);
            pos += n;
            return n;
        }

        @Override
        public int read() throws IOException {
            byte[] one = new byte[1];
            int n = read(one, 0, 1);
            return n < 0 ? -1 : (one[0] & 0xFF);
        }
    }
}
