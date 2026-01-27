package com.justnothing.testmodule.command.output;

import com.justnothing.testmodule.utils.functions.Logger;

public class OutputHandler implements IOutputHandler {
    private final Logger logger;
    private final String prefix;
    private boolean closed = false;

    public OutputHandler(Logger logger) {
        this(logger, "");
    }

    public OutputHandler(Logger logger, String prefix) {
        this.logger = logger;
        this.prefix = prefix;
    }

    @Override
    public void println(String line) {
        logger.info(prefix + line);
    }

    @Override
    public void print(String text) {
        logger.info(prefix + text);
    }

    @Override
    public void printf(String format, Object... args) {
        logger.info(prefix + String.format(format, args));
    }

    @Override
    public void printStackTrace(Throwable t) {
        if (t != null) {
            logger.error(prefix, t);
        }
    }

    @Override
    public void flush() {
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
    }

    @Override
    public String getString() {
        return "";
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
