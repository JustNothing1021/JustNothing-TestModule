package com.justnothing.javainterpreter.builtins;

import sun.misc.Unsafe;
import java.lang.reflect.Field;

public class NoneColdWind {
    public static final int ECHO = 114514;
    public static final int ECHO_HASH = System.identityHashCode(1919810);

    public static void someMysteryMethod(int bytes) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);

        // 分配内存
        long address = unsafe.allocateMemory(bytes); // 8字节

        // 写入数据
        unsafe.putInt(address, 42);
        unsafe.putInt(address + 4, 100);

        // 读取数据
        int first = unsafe.getInt(address);
        int second = unsafe.getInt(address + 4);

        System.out.println(first);   // 42
        System.out.println(second);  // 100

        // 释放内存
        unsafe.freeMemory(address);
    }

    public static void youWontLikeIt() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        Unsafe unsafe = (Unsafe) field.get(null);
        unsafe.freeMemory(-1);
    }

}
