package com.justnothing.testmodule.utils.sandbox;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class SeccompNotifHandler {
    private static final String TAG = "SeccompNotifHandler";
    
    private static final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final AtomicBoolean supported = new AtomicBoolean(false);
    private static final Map<Integer, Boolean> threadPermissions = new ConcurrentHashMap<>();
    
    static {
        try {
            System.loadLibrary("seccomp_notif");
            Log.i(TAG, "Native library loaded");
        } catch (UnsatisfiedLinkError e) {
            Log.e(TAG, "Failed to load native library: " + e.getMessage());
        }
    }
    
    public static boolean isSupported() {
        if (!initialized.get()) {
            try {
                supported.set(nativeIsSupported());
                Log.i(TAG, "USER_NOTIF supported: " + supported.get());
            } catch (Throwable e) {
                Log.e(TAG, "Error checking support: " + e.getMessage());
                supported.set(false);
            }
        }
        return supported.get();
    }
    
    public static synchronized boolean init() {
        if (initialized.get()) {
            return true;
        }
        
        if (!isSupported()) {
            Log.e(TAG, "USER_NOTIF not supported on this device");
            return false;
        }
        
        try {
            boolean success = nativeInit();
            if (success) {
                initialized.set(true);
                Log.i(TAG, "SeccompNotifHandler initialized successfully");
            }
            return success;
        } catch (Throwable e) {
            Log.e(TAG, "Failed to initialize: " + e.getMessage());
            return false;
        }
    }
    
    public static void shutdown() {
        if (initialized.get()) {
            nativeShutdown();
            initialized.set(false);
            threadPermissions.clear();
            Log.i(TAG, "SeccompNotifHandler shutdown");
        }
    }
    
    public static boolean isInitialized() {
        return initialized.get();
    }
    
    public static int getCurrentTid() {
        return nativeGetTid();
    }
    
    public static void registerCurrentThread(boolean allowProcessCreate, boolean allowThreadCreate) {
        int tid = getCurrentTid();
        boolean allowed = allowProcessCreate && allowThreadCreate;
        threadPermissions.put(tid, allowed);
        nativeRegisterThread(tid, allowed);
        Log.d(TAG, "Registered thread " + tid + ": allowed=" + allowed);
    }
    
    public static void registerCurrentThread(boolean allowed) {
        int tid = getCurrentTid();
        threadPermissions.put(tid, allowed);
        nativeRegisterThread(tid, allowed);
        Log.d(TAG, "Registered thread " + tid + ": allowed=" + allowed);
    }
    
    public static void unregisterCurrentThread() {
        int tid = getCurrentTid();
        threadPermissions.remove(tid);
        nativeUnregisterThread(tid);
        Log.d(TAG, "Unregistered thread " + tid);
    }
    
    public static boolean installFilterForCurrentThread() {
        if (!initialized.get()) {
            Log.e(TAG, "Handler not initialized");
            return false;
        }
        return nativeInstallFilter();
    }
    
    public static String getErrorString(int errno) {
        return nativeStrerror(errno);
    }
    
    public static Boolean getThreadPermission(int tid) {
        return threadPermissions.get(tid);
    }
    
    public static boolean installErrnoFilter(boolean blockProcess, boolean blockThread) {
        return nativeInstallErrnoFilter(blockProcess, blockThread);
    }
    
    public static boolean isSeccompAvailable() {
        try {
            return nativeIsSeccompAvailable();
        } catch (Throwable e) {
            Log.e(TAG, "Error checking seccomp availability: " + e.getMessage());
            return false;
        }
    }
    
    // Native methods
    private static native boolean nativeInit();
    private static native void nativeShutdown();
    private static native int nativeGetTid();
    private static native void nativeRegisterThread(int tid, boolean allowed);
    private static native void nativeUnregisterThread(int tid);
    private static native boolean nativeInstallFilter();
    private static native boolean nativeIsSupported();
    private static native String nativeStrerror(int errno);
    private static native boolean nativeInstallErrnoFilter(boolean blockProcess, boolean blockThread);
    private static native boolean nativeIsSeccompAvailable();
}
