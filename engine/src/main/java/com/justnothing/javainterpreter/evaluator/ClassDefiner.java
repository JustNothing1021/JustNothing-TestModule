package com.justnothing.javainterpreter.evaluator;

public interface ClassDefiner {
    Class<?> defineClass(String name, byte[] bytecode, ClassLoader parent) throws Exception;
}
