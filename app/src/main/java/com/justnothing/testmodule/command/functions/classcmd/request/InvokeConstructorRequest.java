package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;
import com.justnothing.testmodule.command.utils.CustomCommandLineParser;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.utils.ParamStringUtils;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("class:constructor")
public class InvokeConstructorRequest extends ClassCommandRequest implements CustomCommandLineParser {

    @CmdParam(
        name = "class",
        description = "类名",
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    /**
     * 构造函数参数（统一数据结构，CLI 和 GUI 共用）
     * <p>
     * CLI 路径: 由 customParse() 将位置2及之后的参数填入此数组
     * GUI 路径: 直接 setParamsRaw() 或 setParams()
     */
    private String[] paramsRaw;

    // --- 非注解字段（运行时解析结果） ---
    private String signature;
    private List<String> params;
    private List<String> paramTypes;

    @CmdParam(
        name = "--free",
        description = "自由模式（使用表达式语法）",
        aliases = {"-f"},
        serializedName = "freeMode"
    )
    private boolean freeMode = false;

    public InvokeConstructorRequest() {
        super();
        this.params = new ArrayList<>();
        this.paramTypes = new ArrayList<>();
    }

    // ========== Getters & Setters ==========

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String[] getParamsRaw() { return paramsRaw; }
    public void setParamsRaw(String[] paramsRaw) {
        this.paramsRaw = paramsRaw;
        rebuildParamsFromRaw();
    }

    public List<String> getParams() {
        if (params == null) params = new ArrayList<>();
        return params;
    }
    public void setParams(List<String> params) { this.params = params; }

    public List<String> getParamTypes() {
        if (paramTypes == null) paramTypes = new ArrayList<>();
        return paramTypes;
    }
    public void setParamTypes(List<String> paramTypes) { this.paramTypes = paramTypes; }

    public boolean isFreeMode() { return freeMode; }
    public void setFreeMode(boolean freeMode) { this.freeMode = freeMode; }

    // ========== CustomCommandLineParser 实现 ==========

    @Override
    public CommandRequest customParse(CustomCommandLineParser.ParseContext context) throws IllegalCommandLineArgumentException {
        return fromCommandLine(context.originalArgs());
    }

    /**
     * CLI 解析入口:
     * 1. 用 ParamParser 解析 className, 标志选项
     * 2. 手动收集位置2及之后的剩余参数 → paramsRaw (String[])
     * 3. 解析每个参数 token → params + paramTypes 列表
     */
    @Override
    public InvokeConstructorRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        InvokeConstructorRequest parsed = ParamParser.parse(InvokeConstructorRequest.class, args);

        // 从原始参数中提取位置2之后的剩余参数（跳过选项）
        List<String> remaining = collectRemainingPositionalArgs(args, 2);
        if (!remaining.isEmpty()) {
            parsed.paramsRaw = remaining.toArray(new String[0]);
            parsed.rebuildParamsFromRaw();
        }

        return parsed;
    }

    // ========== 内部方法 ==========

    /**
     * 从 paramsRaw[] 重建 params + paramTypes 结构化列表
     * <p>
     * 统一入口：CLI (fromCommandLine) 和 GUI (setParamsRaw) 都调用此方法
     */
    private void rebuildParamsFromRaw() {
        this.params.clear();
        this.paramTypes.clear();

        if (paramsRaw == null) return;

        for (String rawParam : paramsRaw) {
            List<ParamStringUtils.ParamToken> tokens = ParamStringUtils.parseParams(rawParam);
            for (ParamStringUtils.ParamToken token : tokens) {
                this.params.add(token.value());
                this.paramTypes.add(token.typeHint());
            }
        }
    }

    /**
     * 从原始参数中收集指定位置之后的非选项参数
     *
     * @param args 原始命令行参数
     * @param startPos 起始位置（1-based，前N个已被位置参数消费）
     * @return 剩余的位置参数列表
     */
    private static List<String> collectRemainingPositionalArgs(String[] args, int startPos) {
        List<String> result = new ArrayList<>();
        int positionalCount = 0;

        for (String arg : args) {
            if (arg.startsWith("-")) continue;  // 跳过选项
            positionalCount++;
            if (positionalCount > startPos) {
                result.add(arg);
            }
        }
        return result;
    }
}
