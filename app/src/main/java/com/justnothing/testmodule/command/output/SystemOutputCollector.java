package com.justnothing.testmodule.command.output;


import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

public class SystemOutputCollector implements IOutputHandler {
    private final StringBuilder sb = new StringBuilder();
    private final PrintStream stream;
    private final InputStream inStream;
    private boolean closed = false;

    public SystemOutputCollector(PrintStream stream) {
        this.stream = stream;
        this.inStream = null;
    }

    public SystemOutputCollector(PrintStream stream, InputStream inputStream) {
        this.stream = stream;
        this.inStream = inputStream;
    }

    @Override
    public void println(String line) {
        stream.println(line);
        sb.append(line).append("\n");
    }

    @Override
    public void print(String text) {
        stream.print(text);
        sb.append(text);
    }

    @Override
    public void printf(String format, Object... args) {
        String s = String.format(Locale.getDefault(), format, args);
        stream.print(s);
        sb.append(s);
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
    public void flush() {
        stream.flush();
    }

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
        if (inStream == null)
            throw new RuntimeException(getClass().getName() + "并不支持readLine...");
        print(prompt);
        StringBuilder sb = new StringBuilder();
        char next;
        try {
            while ((next = (char) inStream.read()) != '\n') {
                sb.append(next);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    @Override
    public String readPasswordFromClient(String prompt) {
        return readLineFromClient(prompt);
    }

    @Override
    public boolean isInteractive() {
        return false;
    }


}