package com.justnothing.javainterpreter.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class BasicPermissionChecker implements IPermissionChecker {
    
    private final Set<PermissionType> allowedPermissions;
    private final Set<PermissionType> deniedPermissions;
    private final Set<String> allowedClasses;
    private final Set<String> deniedClasses;
    private final Set<String> allowedMethods;
    private final Set<String> deniedMethods;
    private final Set<String> allowedFields;
    private final Set<String> deniedFields;
    private final boolean defaultAllow;
    
    private BasicPermissionChecker(Builder builder) {
        this.allowedPermissions = Set.copyOf(builder.allowedPermissions);
        this.deniedPermissions = Set.copyOf(builder.deniedPermissions);
        this.allowedClasses = Set.copyOf(builder.allowedClasses);
        this.deniedClasses = Set.copyOf(builder.deniedClasses);
        this.allowedMethods = Set.copyOf(builder.allowedMethods);
        this.deniedMethods = Set.copyOf(builder.deniedMethods);
        this.allowedFields = Set.copyOf(builder.allowedFields);
        this.deniedFields = Set.copyOf(builder.deniedFields);
        this.defaultAllow = builder.defaultAllow;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static IPermissionChecker permissive() {
        return PERMISSIVE_INSTANCE;
    }
    
    public static IPermissionChecker restrictive() {
        return RESTRICTIVE_INSTANCE;
    }
    
    private static final IPermissionChecker PERMISSIVE_INSTANCE = new IPermissionChecker() {
        @Override
        public boolean hasPermission(PermissionType type) { return true; }
        @Override
        public boolean hasPermission(PermissionType type, String target) { return true; }
        @Override
        public boolean hasClassAccess(String className) { return true; }
        @Override
        public boolean hasMethodAccess(String className, String methodName, String signature) { return true; }
        @Override
        public boolean hasFieldAccess(String className, String fieldName) { return true; }
        @Override
        public boolean hasNewInstanceAccess(String className) { return true; }
        @Override
        public void checkPermission(PermissionType type) {}
        @Override
        public void checkPermission(PermissionType type, String target) {}
        @Override
        public void checkClassAccess(String className) {}
        @Override
        public void checkMethodAccess(String className, String methodName, String signature) {}
        @Override
        public void checkFieldAccess(String className, String fieldName) {}
        @Override
        public void checkNewInstance(String className) {}
    };
    
    private static final IPermissionChecker RESTRICTIVE_INSTANCE = new IPermissionChecker() {
        @Override
        public boolean hasPermission(PermissionType type) { return false; }
        @Override
        public boolean hasPermission(PermissionType type, String target) { return false; }
        @Override
        public boolean hasClassAccess(String className) { return false; }
        @Override
        public boolean hasMethodAccess(String className, String methodName, String signature) { return false; }
        @Override
        public boolean hasFieldAccess(String className, String fieldName) { return false; }
        @Override
        public boolean hasNewInstanceAccess(String className) { return false; }
        @Override
        public void checkPermission(PermissionType type) {
            throw new SecurityException("Permission denied: " + type.getDescription());
        }
        @Override
        public void checkPermission(PermissionType type, String target) {
            throw new SecurityException("Permission denied: " + type.getDescription() + " on " + target);
        }
        @Override
        public void checkClassAccess(String className) {
            throw new SecurityException("Class access denied: " + className);
        }
        @Override
        public void checkMethodAccess(String className, String methodName, String signature) {
            throw new SecurityException("Method access denied: " + className + "." + methodName);
        }
        @Override
        public void checkFieldAccess(String className, String fieldName) {
            throw new SecurityException("Field access denied: " + className + "." + fieldName);
        }
        @Override
        public void checkNewInstance(String className) {
            throw new SecurityException("New instance denied: " + className);
        }
    };
    
    public static BasicPermissionChecker createMinimal() {
        return builder()
                .defaultDeny()
                .allowPermission(PermissionType.CLASS_ACCESS)
                .allowPermission(PermissionType.FIELD_READ)
                .allowPermission(PermissionType.METHOD_CALL)
                .allowClass("java.lang.*")
                .allowClass("java.util.*")
                .allowClass("java.math.*")
                .denyClass("java.lang.Runtime")
                .denyClass("java.lang.ProcessBuilder")
                .denyClass("java.lang.System")
                .denyClass("java.lang.ClassLoader")
                .denyClass("java.lang.Thread")
                .denyClass("java.io.*")
                .denyClass("java.net.*")
                .denyClass("java.nio.*")
                .denyClass("java.lang.reflect.*")
                .denyClass("java.security.*")
                .denyClass("sun.*")
                .denyClass("com.android.*")
                .denyClass("android.*")
                .denyMethod("java.lang.Object.getClass")
                .denyMethod("java.lang.Class.getClassLoader")
                .denyMethod("java.lang.Class.forName")
                .denyMethod("java.lang.Class.getConstructor")
                .denyMethod("java.lang.Class.getMethod")
                .denyMethod("java.lang.Class.getField")
                .denyMethod("java.lang.Class.getDeclared*")
                .build();
    }
    
    public static BasicPermissionChecker createExpressionOnly() {
        return builder()
                .defaultDeny()
                .allowPermission(PermissionType.CLASS_ACCESS)
                .allowPermission(PermissionType.FIELD_READ)
                .allowPermission(PermissionType.METHOD_CALL)
                .allowPermission(PermissionType.NEW_INSTANCE)
                .allowPermission(PermissionType.ARRAY_CREATE)
                .allowClass("java.lang.*")
                .allowClass("java.util.*")
                .allowClass("java.math.*")
                .denyClass("java.lang.Runtime")
                .denyClass("java.lang.ProcessBuilder")
                .denyClass("java.lang.System")
                .denyClass("java.lang.ClassLoader")
                .denyClass("java.lang.Thread")
                .denyClass("java.io.*")
                .denyClass("java.net.*")
                .denyClass("java.nio.*")
                .denyClass("java.lang.reflect.*")
                .denyClass("java.security.*")
                .denyClass("sun.*")
                .denyClass("com.android.*")
                .denyClass("android.*")
                .denyPermission(PermissionType.FILE_READ)
                .denyPermission(PermissionType.FILE_WRITE)
                .denyPermission(PermissionType.EXEC)
                .denyPermission(PermissionType.NETWORK)
                .denyPermission(PermissionType.THREAD_CREATE)
                .denyPermission(PermissionType.SYSTEM_EXIT)
                .denyPermission(PermissionType.REFLECTION)
                .denyMethod("java.lang.Object.getClass")
                .denyMethod("java.lang.Class.getClassLoader")
                .denyMethod("java.lang.Class.forName")
                .denyMethod("java.lang.Class.getConstructor")
                .denyMethod("java.lang.Class.getMethod")
                .denyMethod("java.lang.Class.getField")
                .denyMethod("java.lang.Class.getDeclared*")
                .build();
    }
    
    public static BasicPermissionChecker createSandbox() {
        return builder()
                .defaultDeny()
                .allowPermission(PermissionType.CLASS_ACCESS)
                .allowPermission(PermissionType.FIELD_READ)
                .allowPermission(PermissionType.METHOD_CALL)
                .allowPermission(PermissionType.NEW_INSTANCE)
                .allowPermission(PermissionType.ARRAY_CREATE)
                .allowClass("java.lang.*")
                .allowClass("java.util.*")
                .allowClass("java.math.*")
                .allowClass("java.text.*")
                .denyClass("java.lang.Runtime")
                .denyClass("java.lang.ProcessBuilder")
                .denyClass("java.lang.System")
                .denyClass("java.lang.ClassLoader")
                .denyClass("java.lang.Thread")
                .denyClass("java.io.*")
                .denyClass("java.net.*")
                .denyClass("java.nio.*")
                .denyClass("java.lang.reflect.*")
                .denyClass("java.security.*")
                .denyClass("sun.*")
                .denyClass("com.android.*")
                .denyClass("android.*")
                .denyMethod("java.lang.Object.getClass")
                .denyMethod("java.lang.Class.getClassLoader")
                .denyMethod("java.lang.Class.forName")
                .denyMethod("java.lang.Class.getConstructor")
                .denyMethod("java.lang.Class.getMethod")
                .denyMethod("java.lang.Class.getField")
                .denyMethod("java.lang.Class.getDeclared*")
                .build();
    }

    public static IPermissionChecker createNoRestrictions() {
        return builder().defaultAllow().build();
    }

    private boolean matchesPattern(String name, Set<String> patterns) {
        for (String pattern : patterns) {
            if (pattern.endsWith(".*")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                if (name.startsWith(prefix)) {
                    return true;
                }
            } else if (pattern.equals(name)) {
                return true;
            } else if (pattern.contains("*")) {
                String regex = pattern.replace(".", "\\.").replace("*", ".*");
                if (name.matches(regex)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    @Override
    public boolean hasPermission(PermissionType type) {
        if (deniedPermissions.contains(type)) {
            return false;
        }
        if (allowedPermissions.contains(type)) {
            return true;
        }
        return defaultAllow;
    }
    
    @Override
    public boolean hasPermission(PermissionType type, String target) {
        return hasPermission(type);
    }
    
    @Override
    public boolean hasClassAccess(String className) {
        if (matchesPattern(className, deniedClasses)) {
            return false;
        }
        if (matchesPattern(className, allowedClasses)) {
            return true;
        }
        return defaultAllow;
    }
    
    @Override
    public boolean hasMethodAccess(String className, String methodName, String signature) {
        String fullMethodName = className + "." + methodName;
        if (matchesPattern(fullMethodName, deniedMethods)) {
            return false;
        }
        if (matchesPattern(fullMethodName, allowedMethods)) {
            return true;
        }
        return hasClassAccess(className) && hasPermission(PermissionType.METHOD_CALL);
    }
    
    @Override
    public boolean hasFieldAccess(String className, String fieldName) {
        String fullFieldName = className + "." + fieldName;
        if (matchesPattern(fullFieldName, deniedFields)) {
            return false;
        }
        if (matchesPattern(fullFieldName, allowedFields)) {
            return true;
        }
        return hasClassAccess(className) && hasPermission(PermissionType.FIELD_READ);
    }
    
    @Override
    public boolean hasNewInstanceAccess(String className) {
        return hasClassAccess(className) && hasPermission(PermissionType.NEW_INSTANCE);
    }
    
    @Override
    public void checkPermission(PermissionType type) throws SecurityException {
        if (!hasPermission(type)) {
            throw new SecurityException("Permission denied: " + type.getDescription());
        }
    }
    
    @Override
    public void checkPermission(PermissionType type, String target) throws SecurityException {
        if (!hasPermission(type, target)) {
            throw new SecurityException("Permission denied: " + type.getDescription() + " on " + target);
        }
    }
    
    @Override
    public void checkClassAccess(String className) throws SecurityException {
        if (!hasClassAccess(className)) {
            throw new SecurityException("Class access denied: " + className);
        }
    }
    
    @Override
    public void checkMethodAccess(String className, String methodName, String signature) throws SecurityException {
        if (!hasMethodAccess(className, methodName, signature)) {
            throw new SecurityException("Method access denied: " + className + "." + methodName);
        }
    }
    
    @Override
    public void checkFieldAccess(String className, String fieldName) throws SecurityException {
        if (!hasFieldAccess(className, fieldName)) {
            throw new SecurityException("Field access denied: " + className + "." + fieldName);
        }
    }
    
    @Override
    public void checkNewInstance(String className) throws SecurityException {
        if (!hasNewInstanceAccess(className)) {
            throw new SecurityException("New instance denied: " + className);
        }
    }
    
    public static class Builder {
        private final Set<PermissionType> allowedPermissions = new HashSet<>();
        private final Set<PermissionType> deniedPermissions = new HashSet<>();
        private final Set<String> allowedClasses = new HashSet<>();
        private final Set<String> deniedClasses = new HashSet<>();
        private final Set<String> allowedMethods = new HashSet<>();
        private final Set<String> deniedMethods = new HashSet<>();
        private final Set<String> allowedFields = new HashSet<>();
        private final Set<String> deniedFields = new HashSet<>();
        private boolean defaultAllow = true;
        
        public Builder defaultAllow() {
            this.defaultAllow = true;
            return this;
        }
        
        public Builder defaultDeny() {
            this.defaultAllow = false;
            return this;
        }
        
        public Builder allowPermission(PermissionType... types) {
            allowedPermissions.addAll(Arrays.asList(types));
            return this;
        }
        
        public Builder denyPermission(PermissionType... types) {
            deniedPermissions.addAll(Arrays.asList(types));
            return this;
        }
        
        public Builder allowClass(String... classNames) {
            allowedClasses.addAll(Arrays.asList(classNames));
            return this;
        }
        
        public Builder denyClass(String... classNames) {
            deniedClasses.addAll(Arrays.asList(classNames));
            return this;
        }
        
        public Builder allowMethod(String... methodNames) {
            allowedMethods.addAll(Arrays.asList(methodNames));
            return this;
        }
        
        public Builder denyMethod(String... methodNames) {
            deniedMethods.addAll(Arrays.asList(methodNames));
            return this;
        }
        
        public Builder allowField(String... fieldNames) {
            allowedFields.addAll(Arrays.asList(fieldNames));
            return this;
        }
        
        public Builder denyField(String... fieldNames) {
            deniedFields.addAll(Arrays.asList(fieldNames));
            return this;
        }
        
        public BasicPermissionChecker build() {
            return new BasicPermissionChecker(this);
        }
    }
}
