package com.justnothing.testmodule.command.framework.output;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 把命令执行期间写往 {@link System#out} / {@link System#err} 的内容转发给当前命令的输出处理器。
 *
 * <p>三个关键点（前两个修掉了旧实现的并发缺陷）：</p>
 * <ul>
 *   <li><b>全局流只安装一次</b>（引用计数），不会出现"命令 A 先恢复原始流、命令 B 再把它恢复成
 *       A 那个已关闭的重定向流"，导致全局 stdout 永久失效、输出静默消失。</li>
 *   <li><b>转发目标按线程绑定</b>（{@link ThreadLocal} 栈），并发执行的命令各写各的，不会串台；
 *       用栈而非单值是为了支持同一线程里嵌套执行命令。</li>
 *   <li>没有绑定目标的线程（例如命令内部起的后台线程）仍然写回被替换前的原始流，不会丢输出。</li>
 * </ul>
 *
 * <p>另外每个绑定各自保留尾部不完整的 UTF-8 字节，避免多字节字符被 {@code write} 边界拆碎。</p>
 */
public class SystemOutputRedirector {

    private static final Object INSTALL_LOCK = new Object();

    /** 已安装的全局重定向层数；归零时才恢复原始流。 */
    private static int installCount;
    private static PrintStream realOut;
    private static PrintStream realErr;
    private static PrintStream installedOut;
    private static PrintStream installedErr;

    /** 当前线程绑定的输出目标栈；栈空表示输出直接走原始流。 */
    private static final ThreadLocal<Deque<Binding>> BINDINGS = new ThreadLocal<>();

    /** 一次绑定的输出目标 + 各自的不完整 UTF-8 残字节缓冲。 */
    private static final class Binding {
        final ICommandOutputHandler out;
        final ICommandOutputHandler err;
        final ByteArrayOutputStream outTail = new ByteArrayOutputStream(8);
        final ByteArrayOutputStream errTail = new ByteArrayOutputStream(8);

        Binding(ICommandOutputHandler out, ICommandOutputHandler err) {
            this.out = out;
            this.err = err;
        }
    }

    private final ICommandOutputHandler outputHandler;
    private final ICommandOutputHandler errorHandler;

    public SystemOutputRedirector(ICommandOutputHandler output) {
        this(output, output);
    }

    public SystemOutputRedirector(ICommandOutputHandler output, ICommandOutputHandler error) {
        this.outputHandler = output;
        this.errorHandler = error;
    }

    /** 安装全局重定向，并把当前线程绑定到本次的输出目标。可重入。 */
    public void startRedirect() {
        synchronized (INSTALL_LOCK) {
            if (installCount == 0) {
                realOut = System.out;
                realErr = System.err;
                installedOut = new PrintStream(new DispatchingStream(true), true);
                installedErr = new PrintStream(new DispatchingStream(false), true);
                System.setOut(installedOut);
                System.setErr(installedErr);
            }
            installCount++;
        }

        Deque<Binding> stack = BINDINGS.get();
        if (stack == null) {
            stack = new ArrayDeque<>();
            BINDINGS.set(stack);
        }
        stack.push(new Binding(outputHandler, errorHandler));
    }

    /** 解绑当前线程的输出目标；所有层都退出后恢复原始流。 */
    public void stopRedirect() {
        Deque<Binding> stack = BINDINGS.get();
        if (stack != null) {
            Binding binding = stack.poll();
            if (binding != null) {
                flushBinding(binding);
            }
            if (stack.isEmpty()) {
                BINDINGS.remove();
            }
        }

        synchronized (INSTALL_LOCK) {
            if (installCount > 0 && --installCount == 0) {
                if (installedOut != null) installedOut.flush();
                if (installedErr != null) installedErr.flush();
                System.setOut(realOut);
                System.setErr(realErr);
                installedOut = null;
                installedErr = null;
            }
        }
    }

    private static Binding currentBinding() {
        Deque<Binding> stack = BINDINGS.get();
        return stack == null ? null : stack.peek();
    }

    /** 把该绑定残留的字节（可能是不完整 UTF-8）与缓冲都冲刷出去。 */
    private static void flushBinding(Binding binding) {
        flushTail(binding, true);
        flushTail(binding, false);
        if (binding.out != null) binding.out.flush();
        if (binding.err != null) binding.err.flush();
    }

    private static void flushTail(Binding binding, boolean stdout) {
        ByteArrayOutputStream tail = stdout ? binding.outTail : binding.errTail;
        if (tail.size() == 0) return;
        ICommandOutputHandler handler = stdout ? binding.out : binding.err;
        if (handler != null) {
            handler.print(new String(tail.toByteArray(), StandardCharsets.UTF_8));
        }
        tail.reset();
    }

    /**
     * 全局流：按当前线程绑定的目标转发；没有绑定就写回原始流。
     */
    private static final class DispatchingStream extends OutputStream {

        private final boolean stdout;

        DispatchingStream(boolean stdout) {
            this.stdout = stdout;
        }

        @Override
        public void write(int b) {
            Binding binding = currentBinding();
            ICommandOutputHandler handler = binding == null ? null : (stdout ? binding.out : binding.err);
            if (handler == null) {
                writeToOriginal(b);
                return;
            }
            ByteArrayOutputStream tail = stdout ? binding.outTail : binding.errTail;
            tail.write(b);
            drain(tail, handler);
        }

        @Override
        public void write(byte[] b, int off, int len) {
            Binding binding = currentBinding();
            ICommandOutputHandler handler = binding == null ? null : (stdout ? binding.out : binding.err);
            if (handler == null) {
                writeToOriginal(b, off, len);
                return;
            }
            ByteArrayOutputStream tail = stdout ? binding.outTail : binding.errTail;
            tail.write(b, off, len);
            drain(tail, handler);
        }

        @Override
        public void flush() {
            Binding binding = currentBinding();
            ICommandOutputHandler handler = binding == null ? null : (stdout ? binding.out : binding.err);
            if (handler == null) {
                PrintStream original = stdout ? realOut : realErr;
                if (original != null) original.flush();
                return;
            }
            flushTail(binding, stdout);
            handler.flush();
        }

        private void writeToOriginal(int b) {
            PrintStream original = stdout ? realOut : realErr;
            if (original != null) {
                original.write(b);
                original.flush();
            }
        }

        private void writeToOriginal(byte[] b, int off, int len) {
            PrintStream original = stdout ? realOut : realErr;
            if (original != null) {
                original.write(b, off, len);
                original.flush();
            }
        }
    }

    /**
     * 解码并发送缓冲区中完整的 UTF-8 序列，保留尾部不完整字节等下次写入拼接。
     * 每次 write 后立即调用，避免无换行符时输出卡在缓冲区。
     */
    private static void drain(ByteArrayOutputStream buffer, ICommandOutputHandler handler) {
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
}
