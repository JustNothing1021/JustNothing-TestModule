package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;
import com.justnothing.testmodule.command.utils.CustomCommandLineParser;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.utils.ParamStringUtils;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("class:invoke")
public class InvokeMethodRequest extends ClassCommandRequest implements CustomCommandLineParser {

    @CmdParam(
        name = "class",
        description = "类名",
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "method",
        description = "方法名",
        position = 2,
        required = true,
        serializedName = "methodName"
    )
    private String methodName;

    /**
     * 方法参数（统一数据结构，CLI 和 GUI 共用）
     * <p>
     * CLI 路径: 由 customParse() 将位置3及之后的参数填入此数组
     * GUI 路径: 由 MethodDetailViewModel 直接 setParamsRaw() 或 setParams()
     */
    private String[] paramsRaw;

    @CmdParam(
        name = "--static",
        description = "静态方法",
        aliases = {"-s"},
        serializedName = "static"
    )
    private boolean isStatic = false;

    @CmdParam(
        name = "--free",
        description = "自由模式",
        aliases = {"-f"},
        serializedName = "freeMode"
    )
    private boolean freeMode = false;

    @CmdParam(
        name = "--super",
        description = "访问父类成员",
        serializedName = "accessSuper"
    )
    private boolean accessSuper = false;

    @CmdParam(
        name = "--interfaces",
        description = "访问接口成员",
        serializedName = "accessInterfaces"
    )
    private boolean accessInterfaces = false;

    @CmdParam(
        name = "--instance",
        description = "实例表达式（用于非静态方法的实例）",
        aliases = {"-i"},
        serializedName = "targetInstance"
    )
    private String targetInstance;

    // --- 非注解字段（运行时解析结果） ---
    private String signature;
    private List<String> params;
    private List<String> paramTypes;

    public InvokeMethodRequest() {
        super();
        this.params = new ArrayList<>();
        this.paramTypes = new ArrayList<>();
    }

    // ========== Getters & Setters ==========

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }

    public String[] getParamsRaw() { return paramsRaw; }
    public void setParamsRaw(String[] paramsRaw) {
        this.paramsRaw = paramsRaw;
        // 自动同步到结构化列表
        rebuildParamsFromRaw();
    }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getTargetInstance() { return targetInstance; }
    public void setTargetInstance(String targetInstance) { this.targetInstance = targetInstance; }

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

    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }

    public boolean isAccessSuper() { return accessSuper; }
    public void setAccessSuper(boolean accessSuper) { this.accessSuper = accessSuper; }

    public boolean isAccessInterfaces() { return accessInterfaces; }
    public void setAccessInterfaces(boolean accessInterfaces) { this.accessInterfaces = accessInterfaces; }

    // ========== CustomCommandLineParser 实现 ==========

    @Override
    public CommandRequest customParse(CustomCommandLineParser.ParseContext context) throws IllegalCommandLineArgumentException {
        return fromCommandLine(context.originalArgs());
    }

    /**
     * CLI 解析入口:
     * 1. 预处理：合并反引号包裹的参数（支持带空格的表达式）
     * 2. 用 ParamParser 解析 className, methodName, 标志选项（含 --instance/-i）
     * 3. 手动收集位置3及之后的剩余参数 → paramsRaw (String[])
     * 4. 解析每个参数 token → params + paramTypes 列表
     */
    @Override
    public InvokeMethodRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        // 预处理：合并反引号包裹的参数（如 `new String[2] {"114514", "1919810"}`）
        args = preprocessBacktickArgs(args);

        InvokeMethodRequest parsed = ParamParser.parse(InvokeMethodRequest.class, args);

        // 从原始参数中提取位置3之后的剩余参数（跳过选项及其值）
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

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (isStringOption(arg)) {
                    i++; // 跳过选项的值（如 -i "114514" 中的 "114514"）
                }
                continue;
            }
            positionalCount++;
            if (positionalCount > startPos) {
                result.add(arg);
            }
        }
        return result;
    }

    private static boolean isStringOption(String arg) {
        String lower = arg.toLowerCase();
        return lower.equals("-i") || lower.equals("--instance");
    }

    /**
     * 将反引号包裹的参数序列合并为单个参数（如 `new String[2] {"114514", "1919810"}`）
     * 使得包含空格的表达式在 re-tokenization 后不被拆散
     */
    private static String[] preprocessBacktickArgs(String[] args) {
        List<String> result = new ArrayList<>();
        StringBuilder backtickBuf = null;

        for (String arg : args) {
            if (backtickBuf != null) {
                // 正在累积反引号表达式
                if (backtickBuf.length() > 0) backtickBuf.append(" ");
                backtickBuf.append(arg);
                if (arg.endsWith("`")) {
                    // 反引号闭合
                    String merged = backtickBuf.toString();
                    // 去掉首尾反引号
                    merged = merged.substring(1, merged.length() - 1);
                    result.add(merged);
                    backtickBuf = null;
                }
            } else if (arg.startsWith("`")) {
                // 遇反引号起始
                if (arg.endsWith("`")) {
                    // 单个参数内完成（如 `foo`）
                    result.add(arg.substring(1, arg.length() - 1));
                } else {
                    backtickBuf = new StringBuilder(arg);
                }
            } else {
                result.add(arg);
            }
        }
        // 未闭合的处理：原样保留
        if (backtickBuf != null) result.add(backtickBuf.toString());

        return result.toArray(new String[0]);
    }
}
