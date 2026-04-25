package com.justnothing.testmodule.hooks;

import com.justnothing.testmodule.utils.logging.Logger;


/**
 * 测试类, 用来研究Hook的
 */
@SuppressWarnings("unused")
public class TestClass {
    public static final String TAG = "TestClass";
    private static final Logger logger = Logger.getLoggerForName(TAG);

    public static Object publicStaticField;
    protected static Object protectedStaticField;
    private static Object privateStaticField;

    public Object publicInstanceField;
    protected Object protectedInstanceField;
    private Object privateInstanceField;

    public TestClass() {
        this(114, 514, 1919);
    }

    private TestClass(int publicVal, int protectedVal, int privateVal) {
        publicInstanceField = publicVal;
        protectedInstanceField = protectedVal;
        privateInstanceField = privateVal;
    }

    protected static void protectedStaticMethod() {
        System.out.println("protectedStaticMethod被调用了");
        logger.info("protectedStaticMethod被调用了");
    }

    private static void privateStaticMethod() {
        System.out.println("privateStaticMethod被调用了");
        logger.info("privateStaticMethod被调用了");
    }

    private void privateMethod() {
        System.out.println("privateMethod被调用了");
        logger.info("privateMethod被调用了");
    }

    protected void protectedMethod() {
        System.out.println("protectedMethod被调用了");
        logger.info("protectedMethod被调用了");
    }

    public void instanceMethod() {
        System.out.println("instanceMethod被调用了");
        logger.info("instanceMethod被调用了");
    }

    public static Object getPublicStaticField() {
        return publicStaticField;
    }

    public static Object getProtectedStaticField() {
        return protectedStaticField;
    }

    public static Object getPrivateStaticField() {
        return privateStaticField;
    }

    public Object getPublicInstanceField() {
        return publicInstanceField;
    }

    public Object getProtectedInstanceField() {
        return protectedInstanceField;
    }

    public Object getPrivateInstanceField() {
        return privateInstanceField;
    }

    public static void setPublicStaticField(Object val) {
        publicStaticField = val;
    }

    public static void setProtectedStaticField(Object val) {
        protectedStaticField = val;
    }

    public static void setPrivateStaticField(Object val) {
        privateStaticField = val;
    }

    public void setPublicInstanceField(Object val) {
        publicInstanceField = val;
    }

    public void setProtectedInstanceField(Object val) {
        protectedInstanceField = val;
    }

    public void setPrivateInstanceField(Object val) {
        privateInstanceField = val;
    }

    public int returnMagicNumber() {
        return 114514;
    }
}
