package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.parser.KeywordParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

@SerializeKeyName("ComplexHook")
@AutoSerializable
public class ComplexHookRequest extends CommandRequest {

    @PositionalParam(order = 1, name = "目标类名")
    private String targetClass;

    @PositionalParam(order = 2, name = "目标方法名")
    private String targetMethod;

    @PositionalParam(order = 3, name = "Hook 类型", required = false, defaultValue = "before")
    private String hookType;

    @PositionalParam(order = 4, name = "输出文件路径", required = false, varArgs = true)
    private String outputPath;

    @FlagParam(names = {"-d", "--debug"}, description = "启用调试模式")
    private boolean debugMode;

    @FlagParam(names = {"-t", "--thread-safe"}, description = "线程安全模式")
    private boolean threadSafe;

    @KeywordParam(name = "timeout", description = "超时时间(毫秒)")
    private int timeout;

    @KeywordParam(name = "max-retries", description = "最大重试次数")
    private int maxRetries;

    @KeywordParam(name = "description", description = "Hook 描述信息")
    private String description;

    @KeywordParam(name = "tags", description = "标签列表(逗号分隔)")
    private String tags;

    public String getTargetClass() { return targetClass; }
    public String getTargetMethod() { return targetMethod; }
    public String getHookType() { return hookType; }
    public String getOutputPath() { return outputPath; }
    public boolean isDebugMode() { return debugMode; }
    public boolean isThreadSafe() { return threadSafe; }
    public int getTimeout() { return timeout; }
    public int getMaxRetries() { return maxRetries; }
    public String getDescription() { return description; }
    public String getTags() { return tags; }

    public void setTargetClass(String targetClass) { this.targetClass = targetClass; }
    public void setTargetMethod(String targetMethod) { this.targetMethod = targetMethod; }
    public void setHookType(String hookType) { this.hookType = hookType; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }
    public void setDebugMode(boolean debugMode) { this.debugMode = debugMode; }
    public void setThreadSafe(boolean threadSafe) { this.threadSafe = threadSafe; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public void setDescription(String description) { this.description = description; }
    public void setTags(String tags) { this.tags = tags; }

    @Override
    public String getCommandType() {
        return "complex-hook";
    }



    @Override
    public CommandRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(ComplexHookRequest.class, args);
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof ComplexHookRequest other)) return false;
        return targetClass.equals(other.targetClass)
            && targetMethod.equals(other.targetMethod)
            && hookType.equals(other.hookType)
            && (Objects.equals(outputPath, other.outputPath))
            && debugMode == other.debugMode
            && threadSafe == other.threadSafe
            && timeout == other.timeout
            && maxRetries == other.maxRetries
            && (Objects.equals(description, other.description));
    }
}
