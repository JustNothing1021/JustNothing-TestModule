package com.justnothing.javainterpreter.exception;

public class ContinueException extends RuntimeException {
    public ContinueException() {
        super("Continue statement");
    }
}
