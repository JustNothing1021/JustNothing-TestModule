package com.justnothing.testmodule.command.functions.script_new.exception;


public class ReturnException extends RuntimeException {
    private final Object value;
    
    public ReturnException(Object value) {
        super("Return statement");
        this.value = value;
    }
    
    public Object getValue() {
        return value;
    }
}
