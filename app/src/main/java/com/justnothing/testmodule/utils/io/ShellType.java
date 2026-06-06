package com.justnothing.testmodule.utils.io;

/**
 * Shell 执行器类型枚举。
 *
 * <p>用于标识当前使用的是哪种 Shell 后端，替代之前用 String 传递类型的方式，
 * 提供编译期类型安全，避免拼写错误，也便于 switch/case 分支。</p>
 */
public enum ShellType {

    /** Root Shell（通过 su / RootProcessPool） */
    ROOT("root", "Root Shell (su)"),

    /** 本地 Shell（/system/bin/sh，无需 root） */
    LOCAL("local", "Local Shell (/system/bin/sh)"),

    /** Mock Shell（测试用，不执行真实命令） */
    MOCK("mock", "Mock Shell (test)"),

    /** ADB Shell（通过 adb bridge，预留） */
    ADB("adb", "ADB Shell (bridge)");

    private final String code;
    private final String description;

    ShellType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取短代码标识（如 "root", "local"）
     */
    public String getCode() {
        return code;
    }

    /**
     * 获取类型的中文描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据短代码查找对应的枚举值，未找到返回 null
     */
    public static ShellType fromCode(String code) {
        if (code == null) return null;
        for (ShellType type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        return null;
    }
}
