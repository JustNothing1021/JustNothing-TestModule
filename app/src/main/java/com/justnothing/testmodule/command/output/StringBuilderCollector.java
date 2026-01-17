package com.justnothing.testmodule.command.output;


import java.io.PrintWriter;
import java.io.StringWriter;

public class StringBuilderCollector implements IOutputHandler {
    private final StringBuilder sb = new StringBuilder();
    private boolean closed = false;

    @Override
    public void println(String line) {
        sb.append(line).append("\n");
    }

    @Override
    public void print(String text) {
        sb.append(text);
    }

    @Override
    public void printf(String format, Object... args) {
        sb.append(String.format(format, args));
    }

    @Override
    public void printStackTrace(Throwable t) {
        if (t != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            println(sw.toString());
        }
    }

    @Override
    public void flush() {}

    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void clear() {
        sb.setLength(0);
    }

    @Override
    public String getString() {
        return sb.toString();
    }

    @Override
    public String readLineFromClient(String prompt) {
        throw new RuntimeException(getClass().getName() + "并不支持readLine...");
    }

    @Override
    public String readPasswordFromClient(String prompt) {
        throw new RuntimeException(getClass().getName() + "并不支持readPassword...");
    }

    @Override
    public boolean isInteractive() {
        return false;
    }


}