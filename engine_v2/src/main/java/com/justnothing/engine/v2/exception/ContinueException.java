package com.justnothing.engine.v2.exception;

public class ContinueException extends RuntimeException {
    public ContinueException() {
        super("Continue statement");
    }
}
