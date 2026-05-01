package com.justnothing.testmodule.command.base;

public class IllegalCommandLineArgumentException extends IllegalArgumentException {
    public IllegalCommandLineArgumentException(String message) {
        super(message);
    }
}
