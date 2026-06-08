package com.justnothing.testmodule.utils.reflect;

import java.util.ArrayList;
import java.util.List;

public class CompositeClassLoader extends ClassLoader {
    private final List<ClassLoader> delegates = new ArrayList<>();

    public void addClassLoader(ClassLoader classLoader) {
        if (classLoader != null) {
            delegates.add(0, classLoader);
        }
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) 
            throws ClassNotFoundException {
        Class<?> loadedClass = findLoadedClass(name);
        if (loadedClass != null) {
            return resolveClass(loadedClass, resolve);
        }

        for (ClassLoader loader : delegates) {
            try {
                loadedClass = loader.loadClass(name);
                if (loadedClass != null) {
                    return resolveClass(loadedClass, resolve);
                }
            } catch (ClassNotFoundException ignore) {
                // 当前类加载器无法加载，继续尝试下一个
            }
        }

        // 所有委托都失败，抛出异常
        throw new ClassNotFoundException("Class not found: " + name);
    }

    private Class<?> resolveClass(Class<?> clazz, boolean resolve) {
        if (resolve) {
            resolveClass(clazz);
        }
        return clazz;
    }
}