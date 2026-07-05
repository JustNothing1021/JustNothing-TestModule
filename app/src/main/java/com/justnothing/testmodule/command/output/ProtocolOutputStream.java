package com.justnothing.testmodule.command.output;

import com.justnothing.testmodule.command.protocol.InteractiveProtocol;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * 将 OutputStream 的写入操作适配为 InteractiveProtocol TYPE_SERVER_OUTPUT 帧。
 *
 * <p>ExternalTerminal.writer() 写出的 ANSI 文本会被 flush 时打包成
 * TYPE_SERVER_OUTPUT 协议帧发送给客户端。</p>
 *
 * <p>使用 ByteArrayOutputStream 缓冲原始字节，避免 UTF-8 多字节字符
 * 被 write(int b) 拆碎导致乱码。</p>
 */
public class ProtocolOutputStream extends OutputStream {

    private static final int FLUSH_THRESHOLD = 8192;

    private final OutputStream socketOutput;
    private final Object writeLock;
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    /**
     * @param socketOutput 底层 Socket 输出流
     * @param writeLock    写锁（与 InteractiveOutputHandler 共享）
     */
    public ProtocolOutputStream(OutputStream socketOutput, Object writeLock) {
        this.socketOutput = socketOutput;
        this.writeLock = writeLock;
    }

    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
        if (buffer.size() >= FLUSH_THRESHOLD) {
            flush();
        }
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.write(b, off, len);
        if (buffer.size() >= FLUSH_THRESHOLD) {
            flush();
        }
    }

    @Override
    public void flush() throws IOException {
        if (buffer.size() == 0) return;
        byte[] data = buffer.toByteArray();
        buffer.reset();
        synchronized (writeLock) {
            InteractiveProtocol.writeMessage(socketOutput,
                    InteractiveProtocol.TYPE_SERVER_OUTPUT,
                    data);
        }
    }

    @Override
    public void close() throws IOException {
        flush();
    }
}
