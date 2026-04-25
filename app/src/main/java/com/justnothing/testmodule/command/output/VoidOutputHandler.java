package com.justnothing.testmodule.command.output;

public class VoidOutputHandler implements ICommandOutputHandler {
    @Override
    public void print(String text) {

    }

    @Override
    public void println(String line) {

    }

    @Override
    public void printf(String format, Object... args) {

    }

    @Override
    public void printError(String text) {

    }

    @Override
    public void printlnError(String text) {

    }

    @Override
    public void printStackTrace(Throwable t) {

    }

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public void clear() {

    }

    @Override
    public String getString() {
        return "";
    }

    @Override
    public boolean isInteractive() {
        return false;
    }
}
