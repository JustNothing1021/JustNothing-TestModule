package com.justnothing.testmodule.command.output;

import com.justnothing.testmodule.utils.functions.Logger;

public class HookOutputHandler extends StringBuilderCollector {
    private final Logger logger;
    private final String prefix;
    private boolean closed = false;

    public HookOutputHandler(Logger logger, String prefix) {
        this.logger = logger;
        this.prefix = prefix;
    }

    @Override
    public void println(String line) {
        super.println(line);
        logger.info(prefix + line);
    }

    @Override
    public void print(String text) {
        super.print(text);
        logger.info(prefix + text);
    }

    @Override
    public void printf(String format, Object... args) {
        super.printf(format, args);
        logger.info(prefix + String.format(format, args));
    }

    @Override
    public void printStackTrace(Throwable t) {
        super.printStackTrace(t);
        if (t != null) {
            logger.error(prefix, t);
        }
    }

    @Override
    public void printError(String text) {
        super.printError(text);
        logger.error(prefix + text);
    }

    @Override
    public void printlnError(String text) {
        super.printlnError(text);
        logger.error(prefix + text);
    }


    @Override
    public void close() {
        closed = true;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }


}
