package com.justnothing.testmodule.command.framework.base;

public class IllegalCommandLineArgumentException extends IllegalArgumentException {
    public IllegalCommandLineArgumentException(String message) {
        super(message);
    }
}
