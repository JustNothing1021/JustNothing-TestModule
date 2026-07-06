package com.justnothing.testmodule.command.framework.output;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;


public class SystemOutputRedirector {
    private final ICommandOutputHandler outputHandler;
    private final ICommandOutputHandler errorHandler;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private PrintStream redirectedOut;
    private PrintStream redirectedErr;

    public SystemOutputRedirector(ICommandOutputHandler output) {
        this.outputHandler = output;
        this.errorHandler = output;
    }

    public SystemOutputRedirector(ICommandOutputHandler output, ICommandOutputHandler error) {
        this.outputHandler = output;
        this.errorHandler = error;
    }


    public void startRedirect() {
        originalOut = System.out;
        originalErr = System.err;

        redirectedOut = createRedirectedStream(outputHandler);
        redirectedErr = createRedirectedStream(errorHandler);

        System.setOut(redirectedOut);
        System.setErr(redirectedErr);
    }

    /**
     * 创建一个 PrintStream，缓冲字节直到 flush() 时才用 UTF-8 解码为字符串。
     * 避免多字节 UTF-8 字符在 write(byte[]) 边界被拆碎产生乱码。
     */
    private static PrintStream createRedirectedStream(ICommandOutputHandler handler) {
        return new PrintStream(new BufferedUtf8OutputStream(handler), true);
    }

    /**
     * 缓冲所有写入的字节，仅在 flush() 时用 UTF-8 解码为字符串。
     * 解码时检测尾部不完整的 UTF-8 序列，保留在缓冲区中等待下次写入拼接，
     * 确保多字节字符不会被拆碎。
     */
    private static class BufferedUtf8OutputStream extends OutputStream {
        private final ICommandOutputHandler handler;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream(512);

        BufferedUtf8OutputStream(ICommandOutputHandler handler) {
            this.handler = handler;
        }

        @Override
        public void write(int b) {
            buffer.write(b);
            flushInternal();
        }

        @Override
        public void write(byte[] b, int off, int len) {
            buffer.write(b, off, len);
            flushInternal();
        }

        @Override
        public void flush() {
            flushInternal();
            handler.flush();
        }

        /**
         * 解码并发送缓冲区中完整的 UTF-8 序列，保留尾部不完整字节。
         * 每次 write 后立即调用，避免无换行符时输出卡在缓冲区。
         */
        private void flushInternal() {
            if (buffer.size() == 0) return;
            byte[] data = buffer.toByteArray();
            int incomplete = trailingIncompleteUtf8Bytes(data);

            if (incomplete == 0) {
                // 全部完整，直接解码发送
                handler.print(new String(data, StandardCharsets.UTF_8));
                buffer.reset();
            } else if (incomplete < data.length) {
                // 尾部有不完整序列：只解码完整部分，保留残余字节
                int completeLen = data.length - incomplete;
                handler.print(new String(data, 0, completeLen, StandardCharsets.UTF_8));
                buffer.reset();
                buffer.write(data, completeLen, incomplete);
            }
            // incomplete == data.length：整个缓冲都是不完整序列，等待更多数据
        }
    }

    /**
     * 检查字节数组末尾有多少字节属于不完整的 UTF-8 序列。
     * UTF-8 编码规则：
     * - 0xxxxxxx (1字节, 0x00-0x7F)
     * - 110xxxxx 10xxxxxx (2字节, 起始 0xC0-0xDF)
     * - 1110xxxx 10xxxxxx 10xxxxxx (3字节, 起始 0xE0-0xEF)
     * - 11110xxx 10xxxxxx 10xxxxxx 10xxxxxx (4字节, 起始 0xF0-0xF7)
     *
     * @return 末尾不完整序列的字节数（0 表示全部完整）
     */
    private static int trailingIncompleteUtf8Bytes(byte[] data) {
        if (data.length == 0) return 0;
        int n = data.length;
        int last = data[n - 1] & 0xFF;

        // 末尾是 ASCII：完整
        if (last < 0x80) return 0;

        // 末尾是起始字节但没有续字节：不完整
        if (last >= 0xF0) return 1; // 4字节序列起始，缺3个续字节
        if (last >= 0xE0) return 1; // 3字节序列起始，缺2个续字节
        if (last >= 0xC0) return 1; // 2字节序列起始，缺1个续字节

        // 末尾是续字节 (0x80-0xBF)：向前查找起始字节
        int expected = 0;
        int pos = n - 1;
        while (pos > 0 && (data[pos] & 0xC0) == 0x80 && (n - pos) < 4) {
            pos--;
        }
        int startByte = data[pos] & 0xFF;
        if (startByte >= 0xF0) expected = 4;
        else if (startByte >= 0xE0) expected = 3;
        else if (startByte >= 0xC0) expected = 2;
        else return 0; // 没找到有效起始字节，交给 UTF-8 解码器处理

        int actual = n - pos;
        return actual < expected ? actual : 0;
    }

    /**
     * 停止重定向，恢复原始输出
     */
    public void stopRedirect() {
        if (originalOut != null) {
            System.setOut(originalOut);
        }
        if (originalErr != null) {
            System.setErr(originalErr);
        }

        if (redirectedOut != null) {
            redirectedOut.close();
        }

        if (redirectedErr != null) {
            redirectedErr.close();
        }
    }
}
