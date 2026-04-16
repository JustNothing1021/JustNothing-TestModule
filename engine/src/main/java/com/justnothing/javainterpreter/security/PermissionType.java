package com.justnothing.javainterpreter.security;

public enum PermissionType {
    CLASS_ACCESS("class.access", "访问类"),
    CLASS_LOAD("class.load", "动态加载类"),
    
    METHOD_CALL("method.call", "调用方法"),
    METHOD_REFLECTION("method.reflection", "反射调用方法"),
    
    FIELD_READ("field.read", "读取字段"),
    FIELD_WRITE("field.write", "修改字段"),
    FIELD_REFLECTION("field.reflection", "反射访问字段"),
    
    NEW_INSTANCE("newInstance", "创建新实例"),
    ARRAY_CREATE("array.create", "创建数组"),
    
    REFLECTION("reflection", "反射操作"),
    
    FILE_READ("file.read", "读取文件"),
    FILE_WRITE("file.write", "写入文件"),
    FILE_DELETE("file.delete", "删除文件"),
    
    NETWORK("network", "网络操作"),
    
    EXEC("exec", "执行外部命令"),
    
    THREAD_CREATE("thread.create", "创建线程"),
    THREAD_MODIFY("thread.modify", "修改线程"),
    
    SYSTEM_EXIT("system.exit", "系统退出"),
    SYSTEM_PROPERTY("system.property", "访问系统属性"),
    SYSTEM_ENV("system.env", "访问环境变量"),
    
    CLASS_LOADER("classLoader", "访问类加载器"),
    SECURITY_MANAGER("securityManager", "访问安全管理器"),
    
    UNSAFE("unsafe", "Unsafe操作"),
    
    NATIVE("native", "调用Native方法");

    private final String id;
    private final String description;

    PermissionType(String id, String description) {
        this.id = id;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }
}
