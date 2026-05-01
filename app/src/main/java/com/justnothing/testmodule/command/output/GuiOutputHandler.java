package com.justnothing.testmodule.command.output;

public class GuiOutputHandler implements ICommandOutputHandler {
    private final ICommandOutputHandler delegate;
    private boolean closed = false;

    public GuiOutputHandler(ICommandOutputHandler delegate) {
        this.delegate = delegate;
    }

    /**
     * 写入最终结果（唯一不被吞掉的输出）
     */
    public void writeFinalResult(String json) {
        if (delegate != null) {
            delegate.println(json);
        }
    }

    @Override
    public void print(String text) {
        // GUI模式：吞掉CLI格式的print输出
    }

    @Override
    public void println(String line) {
        // GUI模式：吞掉CLI格式的println输出
    }

    @Override
    public void printf(String format, Object... args) {
        // GUI模式：吞掉printf输出
    }

    @Override
    public void printError(String text) {
        // GUI模式：吞掉错误输出
    }

    @Override
    public void printlnError(String text) {
        // GUI模式：吞掉错误输出
    }

    @Override
    public void printStackTrace(Throwable t) {
        // GUI模式：吞掉堆栈追踪
    }

    @Override
    public void flush() {
        if (delegate != null) delegate.flush();
    }

    @Override
    public void close() {
        closed = true;
        if (delegate != null) delegate.close();
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void clear() {
        if (delegate != null) delegate.clear();
    }

    @Override
    public String getString() {
        return delegate != null ? delegate.getString() : "";
    }

    @Override
    public boolean isInteractive() {
        return false;
    }
}
