package com.justnothing.javainterpreter.exception;

public class BreakException extends RuntimeException {
    public BreakException() {
        super("Break statement");
    }
}
