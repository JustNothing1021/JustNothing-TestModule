package com.justnothing.testmodule.command.functions.hook;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class HookInfo {
    private static final AtomicInteger idCounter = new AtomicInteger(0);

    private final String id;
    private final String className;
    private final String methodName;
    private final String signature;
    private final String beforeCode;
    private final String afterCode;
    private final String replaceCode;
    private final String beforeCodebase;
    private final String afterCodebase;
    private final String replaceCodebase;
    private final long createTime;
    private final ClassLoader classLoader;
    private final AtomicInteger callCount;
    private volatile boolean active;
    private volatile boolean enabled;

    public HookInfo(String className, String methodName, String signature, 
                   String beforeCode, String afterCode, String replaceCode,
                   String beforeCodebase, String afterCodebase, String replaceCodebase,
                   ClassLoader classLoader) {
        this.id = "hook_" + idCounter.incrementAndGet();
        this.className = className;
        this.methodName = methodName;
        this.signature = signature;
        this.beforeCode = beforeCode;
        this.afterCode = afterCode;
        this.replaceCode = replaceCode;
        this.beforeCodebase = beforeCodebase;
        this.afterCodebase = afterCodebase;
        this.replaceCodebase = replaceCodebase;
        this.classLoader = classLoader;
        this.createTime = System.currentTimeMillis();
        this.callCount = new AtomicInteger(0);
        this.active = true;
        this.enabled = true;
    }

    public String getId() {
        return id;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getSignature() {
        return signature;
    }

    public String getBeforeCode() {
        return beforeCode;
    }

    public String getAfterCode() {
        return afterCode;
    }

    public String getReplaceCode() {
        return replaceCode;
    }

    public String getBeforeCodebase() {
        return beforeCodebase;
    }

    public String getAfterCodebase() {
        return afterCodebase;
    }

    public String getReplaceCodebase() {
        return replaceCodebase;
    }

    public long getCreateTime() {
        return createTime;
    }

    public int getCallCount() {
        return callCount.get();
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public void incrementCallCount() {
        callCount.incrementAndGet();
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean hasBefore() {
        return (beforeCode != null && !beforeCode.isEmpty()) || 
               (beforeCodebase != null && !beforeCodebase.isEmpty());
    }

    public boolean hasAfter() {
        return (afterCode != null && !afterCode.isEmpty()) || 
               (afterCodebase != null && !afterCodebase.isEmpty());
    }

    public boolean hasReplace() {
        return (replaceCode != null && !replaceCode.isEmpty()) || 
               (replaceCodebase != null && !replaceCodebase.isEmpty());
    }

    public String getDisplayInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("ID: ").append(id).append("\n");
        sb.append("类名: ").append(className).append("\n");
        sb.append("方法名: ").append(methodName).append("\n");
        if (signature != null && !signature.isEmpty()) {
            sb.append("签名: ").append(signature).append("\n");
        }
        sb.append("创建时间: ").append(formatTime(createTime)).append("\n");
        sb.append("调用次数: ").append(callCount.get()).append("\n");
        sb.append("状态: ").append(active ? "活跃" : "非活跃").append("\n");
        sb.append("启用: ").append(enabled ? "是" : "否").append("\n");
        
        if (beforeCode != null && !beforeCode.isEmpty()) {
            sb.append("Before Code: ").append(truncateCode(beforeCode)).append("\n");
        }
        if (beforeCodebase != null && !beforeCodebase.isEmpty()) {
            sb.append("Before Codebase: ").append(beforeCodebase).append("\n");
        }
        if (afterCode != null && !afterCode.isEmpty()) {
            sb.append("After Code: ").append(truncateCode(afterCode)).append("\n");
        }
        if (afterCodebase != null && !afterCodebase.isEmpty()) {
            sb.append("After Codebase: ").append(afterCodebase).append("\n");
        }
        if (replaceCode != null && !replaceCode.isEmpty()) {
            sb.append("Replace Code: ").append(truncateCode(replaceCode)).append("\n");
        }
        if (replaceCodebase != null && !replaceCodebase.isEmpty()) {
            sb.append("Replace Codebase: ").append(replaceCodebase).append("\n");
        }
        
        return sb.toString();
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date(timestamp));
    }

    private String truncateCode(String code) {
        if (code == null) return "";
        if (code.length() <= 50) return code;
        return code.substring(0, 47) + "...";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("className", className);
        map.put("methodName", methodName);
        map.put("signature", signature);
        map.put("beforeCode", beforeCode);
        map.put("afterCode", afterCode);
        map.put("replaceCode", replaceCode);
        map.put("beforeCodebase", beforeCodebase);
        map.put("afterCodebase", afterCodebase);
        map.put("replaceCodebase", replaceCodebase);
        map.put("createTime", createTime);
        map.put("callCount", callCount.get());
        map.put("active", active);
        map.put("enabled", enabled);
        return map;
    }
}
