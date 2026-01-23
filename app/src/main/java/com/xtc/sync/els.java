package com.xtc.sync;

import android.annotation.SuppressLint;

import com.justnothing.testmodule.utils.functions.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.Objects;

/* compiled from: SystemPropertyUtil.java */
/* loaded from: classes.dex */
public class els {

    public static Logger logger = new Logger() {

        @Override
        public String getTag() {
            return "xtc.sync.els";
        }
    };

    /* renamed from: a, reason: collision with root package name */
    @Deprecated
    public static final long f26089a = -1;

    /* renamed from: a, reason: collision with other field name */
    private static final String f8964a = "SystemPropertyUtil";

    @Deprecated
    public static void a(String str, boolean z) {
        try {
            @SuppressLint("PrivateApi") Class<?> cls = Class.forName("android.os.SystemProperties");
            cls.getMethod("set", String.class, String.class).invoke(cls, str, String.valueOf(z));
            logger.info("设置系统属性：" + str + "=" + z);
        } catch (ClassNotFoundException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            logger.error("设置系统属性" + str + "失败", e);
        }
    }

    @Deprecated
    /* renamed from: a, reason: collision with other method in class */
    public static void m4919a(String str, long j) {
        try {
            @SuppressLint("PrivateApi") Class<?> cls = Class.forName("android.os.SystemProperties");
            cls.getMethod("set", String.class, String.class).invoke(cls, str, String.valueOf(j));
            logger.info("设置系统属性：" + str + "=" + j);
        } catch (ClassNotFoundException | IllegalAccessException | 
                    NoSuchMethodException | InvocationTargetException e) {
            logger.error("设置系统属性" + str + "失败", e);
        }
    }

    @Deprecated
    /* renamed from: a, reason: collision with other method in class */
    public static boolean m4921a(String str, boolean z) {
        try {
            @SuppressLint("PrivateApi") Class<?> cls = Class.forName("android.os.SystemProperties");
            z = (Boolean) Objects.requireNonNull(cls.getMethod("getBoolean", String.class, Boolean.TYPE).invoke(cls, str, z));
            logger.info("获取系统属性：" + str + "=" + z);
        } catch (ClassNotFoundException | IllegalAccessException | NullPointerException |
                    NoSuchMethodException | InvocationTargetException e) {
            logger.error("获取系统属性" + str + "失败", e);
        }
        return z;
    }

    @Deprecated
    public static long a(String str, long j) {
        try {
            @SuppressLint("PrivateApi") Class<?> cls = Class.forName("android.os.SystemProperties");
            j = (Long) Objects.requireNonNull(cls.getMethod("getLong", String.class, Long.TYPE).invoke(cls, str, j));
            logger.info("获取系统属性：" + str + "=" + j);
        } catch (ClassNotFoundException | IllegalAccessException | NullPointerException |
                    NoSuchMethodException | InvocationTargetException e) {
            logger.error("获取系统属性" + str + "失败", e);
        }
        return j;
    }

    /* renamed from: a, reason: collision with other method in class */
    @SuppressLint("PrivateApi")
    public static void m4920a(String str, String str2) {
        try {
            Class.forName("android.os.SystemProperties")
                    .getMethod("set", String.class, String.class)
                    .invoke(null, str, str2);
        } catch (Exception e) {
            logger.error("设置系统属性" + str + "失败", e);
        }
    }

    public static void b(String str, boolean z) {
        m4920a(str, String.valueOf(z));
    }


    @SuppressLint("PrivateApi")
    public static String a(String str, String str2) {
        try {
            return (String) Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class, String.class)
                    .invoke(null, str, str2);
        } catch (Exception e) {
            logger.error("获取系统属性" + str + "失败", e);
            return str2;
        }
    }

    /* renamed from: b, reason: collision with other method in class */
    public static boolean m4923b(String str, boolean z) {
        try {
            @SuppressLint("PrivateApi") Boolean bool =
                    (Boolean) Class.forName("android.os.SystemProperties")
                                .getMethod("getBoolean", String.class, Boolean.TYPE)
                                .invoke(null, str, z);
            return bool == null ? z : bool;
        } catch (Exception e) {
            logger.error("获取系统属性" + str + "失败", e);
            return z;
        }
    }

    public static int a(String str, int i) {
        try {
            @SuppressLint("PrivateApi") Integer num =
                    (Integer) Class.forName("android.os.SystemProperties")
                                .getMethod("getInt", String.class, Integer.TYPE)
                                .invoke(null, str, i);
            return num == null ? i : num;
        } catch (Exception e) {
            logger.error("获取系统属性" + str + "失败", e);
            return i;
        }
    }

    public static long b(String str, long j) {
        try {
            @SuppressLint("PrivateApi") Long l =
                    (Long) Class.forName("android.os.SystemProperties")
                            .getMethod("getLong", String.class, Long.TYPE)
                            .invoke(null, str, j);
            return l == null ? j : l;
        } catch (Exception e) {
            logger.error("获取系统属性" + str + "失败", e);
            return j;
        }
    }
}