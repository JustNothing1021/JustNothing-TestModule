package com.justnothing.testmodule.command.framework.error;

public class IllegalCommandLineArgumentException extends IllegalArgumentException {
    public IllegalCommandLineArgumentException(String message) {
        super(message);
    }
}
