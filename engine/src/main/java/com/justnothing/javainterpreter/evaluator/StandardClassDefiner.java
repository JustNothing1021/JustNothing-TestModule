package com.justnothing.javainterpreter.evaluator;

public class StandardClassDefiner implements ClassDefiner {

    @Override
    public Class<?> defineClass(String name, byte[] bytecode, ClassLoader parent) throws Exception {
        DynamicClassLoader loader = new DynamicClassLoader(parent);
        return loader.defineClass(name, bytecode);
    }

    private static class DynamicClassLoader extends ClassLoader {
        DynamicClassLoader(ClassLoader parent) {
            super(parent);
        }

        public Class<?> defineClass(String name, byte[] bytecode) {
            try {
                return defineClass(name, bytecode, 0, bytecode.length);
            } catch (LinkageError e) {
                Class<?> existing = findLoadedClass(name);
                if (existing != null) {
                    return existing;
                }
                throw e;
            }
        }
    }
}
