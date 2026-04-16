package com.justnothing.javainterpreter.security;


public class SandboxConfig {

    // 系统级别权限
    private final boolean diskReadAllowed;
    private final boolean diskWriteAllowed;
    private final boolean fileDeleteAllowed;
    private final boolean networkAllowed;
    private final boolean localSocketAllowed;
    private final boolean threadCreateAllowed;
    private final boolean threadModifyAllowed;
    private final boolean processCreateAllowed;
    private final boolean reflectionAllowed;
    private final boolean systemExitAllowed;
    private final boolean systemPropertyAllowed;
    private final boolean systemEnvAllowed;
    private final boolean classLoaderAllowed;
    private final boolean unsafeAllowed;
    private final boolean nativeAllowed;
    
    // 解释器级别权限
    private final IPermissionChecker permissionChecker;
    
    public static final SandboxConfig DEFAULT = builder()
            .denyDiskRead()
            .denyDiskWrite()
            .denyFileDelete()
            .denyNetwork()
            .allowLocalSocket()
            .denyThreadCreate()
            .denyThreadModify()
            .denyProcessCreate()
            .denyReflection()
            .denySystemExit()
            .denySystemProperty()
            .denySystemEnv()
            .denyClassLoader()
            .denyUnsafe()
            .denyNative()
            .build();
    
    public static final SandboxConfig SANDBOX = builder()
            .denyDiskRead()
            .denyDiskWrite()
            .denyFileDelete()
            .denyNetwork()
            .denyLocalSocket()
            .denyThreadCreate()
            .denyThreadModify()
            .denyProcessCreate()
            .denyReflection()
            .denySystemExit()
            .denySystemProperty()
            .denySystemEnv()
            .denyClassLoader()
            .denyUnsafe()
            .denyNative()
            .permissionChecker(BasicPermissionChecker.createSandbox())
            .build();
    
    public static final SandboxConfig EXPRESSION_ONLY = builder()
            .denyDiskRead()
            .denyDiskWrite()
            .denyFileDelete()
            .denyNetwork()
            .denyLocalSocket()
            .denyThreadCreate()
            .denyThreadModify()
            .denyProcessCreate()
            .denyReflection()
            .denySystemExit()
            .denySystemProperty()
            .denySystemEnv()
            .denyClassLoader()
            .denyUnsafe()
            .denyNative()
            .build();
    
    public static final SandboxConfig MINIMAL = builder()
            .allowDiskRead()
            .denyDiskWrite()
            .denyFileDelete()
            .denyNetwork()
            .allowLocalSocket()
            .denyThreadCreate()
            .denyThreadModify()
            .denyProcessCreate()
            .denyReflection()
            .denySystemExit()
            .denySystemProperty()
            .denySystemEnv()
            .denyClassLoader()
            .denyUnsafe()
            .denyNative()
            .build();
    
    public static final SandboxConfig FULL = builder()
            .allowDiskRead()
            .allowDiskWrite()
            .allowFileDelete()
            .allowNetwork()
            .allowLocalSocket()
            .allowThreadCreate()
            .allowThreadModify()
            .allowProcessCreate()
            .allowReflection()
            .allowSystemExit()
            .allowSystemProperty()
            .allowSystemEnv()
            .allowClassLoader()
            .allowUnsafe()
            .allowNative()
            .build();
    
    private SandboxConfig(Builder builder) {
        this.diskReadAllowed = builder.diskReadAllowed;
        this.diskWriteAllowed = builder.diskWriteAllowed;
        this.fileDeleteAllowed = builder.fileDeleteAllowed;
        this.networkAllowed = builder.networkAllowed;
        this.localSocketAllowed = builder.localSocketAllowed;
        this.threadCreateAllowed = builder.threadCreateAllowed;
        this.threadModifyAllowed = builder.threadModifyAllowed;
        this.processCreateAllowed = builder.processCreateAllowed;
        this.reflectionAllowed = builder.reflectionAllowed;
        this.systemExitAllowed = builder.systemExitAllowed;
        this.systemPropertyAllowed = builder.systemPropertyAllowed;
        this.systemEnvAllowed = builder.systemEnvAllowed;
        this.classLoaderAllowed = builder.classLoaderAllowed;
        this.unsafeAllowed = builder.unsafeAllowed;
        this.nativeAllowed = builder.nativeAllowed;
        this.permissionChecker = builder.permissionChecker;
    }
    
    public boolean isDiskReadAllowed() {
        return diskReadAllowed;
    }
    
    public boolean isDiskWriteAllowed() {
        return diskWriteAllowed;
    }
    
    public boolean isFileDeleteAllowed() {
        return fileDeleteAllowed;
    }
    
    public boolean isNetworkAllowed() {
        return networkAllowed;
    }
    
    public boolean isLocalSocketAllowed() {
        return localSocketAllowed;
    }
    
    public boolean isThreadCreateAllowed() {
        return threadCreateAllowed;
    }
    
    public boolean isThreadModifyAllowed() {
        return threadModifyAllowed;
    }
    
    public boolean isProcessCreateAllowed() {
        return processCreateAllowed;
    }
    
    public boolean isReflectionAllowed() {
        return reflectionAllowed;
    }
    
    public boolean isSystemExitAllowed() {
        return systemExitAllowed;
    }
    
    public boolean isSystemPropertyAllowed() {
        return systemPropertyAllowed;
    }
    
    public boolean isSystemEnvAllowed() {
        return systemEnvAllowed;
    }
    
    public boolean isClassLoaderAllowed() {
        return classLoaderAllowed;
    }
    
    public boolean isUnsafeAllowed() {
        return unsafeAllowed;
    }
    
    public boolean isNativeAllowed() {
        return nativeAllowed;
    }
    
    public IPermissionChecker getPermissionChecker() {
        return permissionChecker;
    }
    
    public IPermissionChecker getAstPermissionChecker() {
        return permissionChecker;
    }
    
    public static Builder builder() {
        return new Builder();
    }
    
    public static final class Builder {
        private boolean diskReadAllowed = false;
        private boolean diskWriteAllowed = false;
        private boolean fileDeleteAllowed = false;
        private boolean networkAllowed = false;
        private boolean localSocketAllowed = true;
        private boolean threadCreateAllowed = false;
        private boolean threadModifyAllowed = false;
        private boolean processCreateAllowed = false;
        private boolean reflectionAllowed = false;
        private boolean systemExitAllowed = false;
        private boolean systemPropertyAllowed = false;
        private boolean systemEnvAllowed = false;
        private boolean classLoaderAllowed = false;
        private boolean unsafeAllowed = false;
        private boolean nativeAllowed = false;
        private IPermissionChecker permissionChecker = createDefaultChecker();
        
        public Builder allowDiskRead() {
            this.diskReadAllowed = true;
            return this;
        }
        
        public Builder denyDiskRead() {
            this.diskReadAllowed = false;
            return this;
        }
        
        public Builder allowDiskWrite() {
            this.diskWriteAllowed = true;
            return this;
        }
        
        public Builder denyDiskWrite() {
            this.diskWriteAllowed = false;
            return this;
        }
        
        public Builder allowFileDelete() {
            this.fileDeleteAllowed = true;
            return this;
        }
        
        public Builder denyFileDelete() {
            this.fileDeleteAllowed = false;
            return this;
        }
        
        public Builder allowNetwork() {
            this.networkAllowed = true;
            return this;
        }
        
        public Builder denyNetwork() {
            this.networkAllowed = false;
            return this;
        }
        
        public Builder allowLocalSocket() {
            this.localSocketAllowed = true;
            return this;
        }
        
        public Builder denyLocalSocket() {
            this.localSocketAllowed = false;
            return this;
        }
        
        public Builder allowThreadCreate() {
            this.threadCreateAllowed = true;
            return this;
        }
        
        public Builder denyThreadCreate() {
            this.threadCreateAllowed = false;
            return this;
        }
        
        public Builder allowThreadModify() {
            this.threadModifyAllowed = true;
            return this;
        }
        
        public Builder denyThreadModify() {
            this.threadModifyAllowed = false;
            return this;
        }
        
        public Builder allowProcessCreate() {
            this.processCreateAllowed = true;
            return this;
        }
        
        public Builder denyProcessCreate() {
            this.processCreateAllowed = false;
            return this;
        }
        
        public Builder allowReflection() {
            this.reflectionAllowed = true;
            return this;
        }
        
        public Builder denyReflection() {
            this.reflectionAllowed = false;
            return this;
        }
        
        public Builder allowSystemExit() {
            this.systemExitAllowed = true;
            return this;
        }
        
        public Builder denySystemExit() {
            this.systemExitAllowed = false;
            return this;
        }
        
        public Builder allowSystemProperty() {
            this.systemPropertyAllowed = true;
            return this;
        }
        
        public Builder denySystemProperty() {
            this.systemPropertyAllowed = false;
            return this;
        }
        
        public Builder allowSystemEnv() {
            this.systemEnvAllowed = true;
            return this;
        }
        
        public Builder denySystemEnv() {
            this.systemEnvAllowed = false;
            return this;
        }
        
        public Builder allowClassLoader() {
            this.classLoaderAllowed = true;
            return this;
        }
        
        public Builder denyClassLoader() {
            this.classLoaderAllowed = false;
            return this;
        }
        
        public Builder allowUnsafe() {
            this.unsafeAllowed = true;
            return this;
        }
        
        public Builder denyUnsafe() {
            this.unsafeAllowed = false;
            return this;
        }
        
        public Builder allowNative() {
            this.nativeAllowed = true;
            return this;
        }
        
        public Builder denyNative() {
            this.nativeAllowed = false;
            return this;
        }
        
        public Builder permissionChecker(IPermissionChecker checker) {
            this.permissionChecker = checker;
            return this;
        }
        
        public SandboxConfig build() {
            return new SandboxConfig(this);
        }
        
        private static IPermissionChecker createDefaultChecker() {
            return BasicPermissionChecker.builder()
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
                    .denyClass("java.lang.Thread")
                    .denyClass("java.lang.System")
                    .denyClass("java.io.*")
                    .denyClass("java.net.*")
                    .denyClass("java.nio.*")
                    .denyClass("java.lang.reflect.*")
                    .denyClass("java.security.*")
                    .denyClass("sun.misc.Unsafe")
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
    }
}
