package com.justnothing.javainterpreter.security;

public interface IPermissionChecker {
    
    boolean hasPermission(PermissionType type);
    
    boolean hasPermission(PermissionType type, String target);
    
    boolean hasClassAccess(String className);
    
    boolean hasMethodAccess(String className, String methodName, String signature);
    
    boolean hasFieldAccess(String className, String fieldName);
    
    boolean hasNewInstanceAccess(String className);
    
    void checkPermission(PermissionType type) throws SecurityException;
    
    void checkPermission(PermissionType type, String target) throws SecurityException;
    
    void checkClassAccess(String className) throws SecurityException;
    
    void checkMethodAccess(String className, String methodName, String signature) throws SecurityException;
    
    void checkFieldAccess(String className, String fieldName) throws SecurityException;
    
    void checkNewInstance(String className) throws SecurityException;
    
    static IPermissionChecker getPermissive() {
        return BasicPermissionChecker.permissive();
    }
    
    static IPermissionChecker getRestrictive() {
        return BasicPermissionChecker.restrictive();
    }
}
