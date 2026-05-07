package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.AutoSerializableBase;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("AutoHook")
@AutoSerializable
public class AutoHookRequest extends AutoSerializableBase {

    @ResultField(name = "targetClass", description = "目标类名")
    private String targetClass;

    @ResultField(name = "targetMethod", description = "目标方法名")
    private String targetMethod;

    @ResultField(name = "hookType", description = "Hook 类型")
    private String hookType;

    @ResultField(name = "outputPath", description = "输出路径")
    private String outputPath;

    @ResultField(name = "debugMode", description = "调试模式")
    private boolean debugMode;

    @ResultField(name = "threadSafe", description = "线程安全")
    private boolean threadSafe;

    @ResultField(name = "timeout", description = "超时时间(ms)")
    private int timeout;

    @ResultField(name = "maxRetries", description = "最大重试次数")
    private int maxRetries;

    @ResultField(name = "description", description = "描述信息")
    private String description;

    public AutoHookRequest() {
        this.hookType = "before";
        this.timeout = 0;
        this.maxRetries = 0;
    }

    public String getTargetClass() { return targetClass; }
    public String getTargetMethod() { return targetMethod; }
    public String getHookType() { return hookType; }
    public String getOutputPath() { return outputPath; }
    public boolean isDebugMode() { return debugMode; }
    public boolean isThreadSafe() { return threadSafe; }
    public int getTimeout() { return timeout; }
    public int getMaxRetries() { return maxRetries; }
    public String getDescription() { return description; }

    public void setTargetClass(String targetClass) { this.targetClass = targetClass; }
    public void setTargetMethod(String targetMethod) { this.targetMethod = targetMethod; }
    public void setHookType(String hookType) { this.hookType = hookType; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    public void setThreadSafe(boolean threadSafe) { this.threadSafe = threadSafe; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public void setDescription(String description) { this.description = description; }

    public CommandRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(ComplexHookRequest.class, args);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AutoHookRequest)) return false;
        AutoHookRequest other = (AutoHookRequest) obj;
        return (targetClass == null ? other.targetClass == null : targetClass.equals(other.targetClass))
            && (targetMethod == null ? other.targetMethod == null : targetMethod.equals(other.targetMethod))
            && hookType.equals(other.hookType)
            && debugMode == other.debugMode
            && threadSafe == other.threadSafe
            && timeout == other.timeout
            && maxRetries == other.maxRetries;
    }
}
