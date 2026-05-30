package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("class:field")
public class FieldRequest extends ClassCommandRequest {

    // ========== 通用参数（不属于任何操作符）==========

    @CmdParam(
        name = "--class",
        description = "目标类名",
        position = 1,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--instance",
        description = "目标实例表达式 (用于操作非静态字段, 支持代码表达式)",
        aliases = {"-i"},
        required = false,
        readMode = CmdParam.ReadMode.PRESERVED,
        serializedName = "targetInstance"
    )
    private String targetInstance;

    // ========== 显示选项（不属于任何操作符）==========

    @CmdParam(
        name = "--value",
        description = "显示字段值",
        aliases = {"-v"},
        serializedName = "showValue"
    )
    private boolean showValue;

    @CmdParam(
        name = "--type",
        description = "显示字段类型",
        aliases = {"-t"},
        serializedName = "showType"
    )
    private boolean showType;

    @CmdParam(
        name = "--modifiers",
        description = "显示修饰符",
        aliases = {"-m"},
        serializedName = "showModifiers"
    )
    private boolean showModifiers;

    @CmdParam(
        name = "--all",
        description = "显示所有信息 (默认)",
        aliases = {"-a"},
        serializedName = "showAll"
    )
    private boolean showAll;

    // ========== 访问控制选项（不属于任何操作符）==========

    @CmdParam(
        name = "--super",
        description = "访问父类字段",
        serializedName = "accessSuper"
    )
    private boolean accessSuper;

    @CmdParam(
        name = "--interfaces",
        description = "访问接口字段",
        serializedName = "accessInterfaces"
    )
    private boolean accessInterfaces;

    @CmdParam(
        name = "--static",
        description = "仅静态字段",
        aliases = {"--static-only"},
        serializedName = "staticOnly"
    )
    private boolean staticOnly;

    // ========== get 操作符组 (分离存储) ==========

    @CmdParam(
        name = "--get",
        description = "获取字段值 (支持: --get name / get name / -g name)",
        aliases = {"-g", "get"},
        isOperator = true,
        operatorArgs = 1,
        mutexWith = {"--set"},
        belongsToOperator = "get",
        operatorIndex = 0,  // 操作符标志本身
        serializedName = "useGet"
    )
    private boolean useGet;

    @CmdParam(
        name = "--get-target",
        description = "要获取的字段名",
        belongsToOperator = "get",
        operatorIndex = 1,  // get 的第1个参数
        required = false,
        serializedName = "fieldName"
    )
    private String getTargetFieldName;

    // ========== set 操作符组 (分离存储) ==========

    @CmdParam(
        name = "--set",
        description = "设置字段值 (支持: --set name val / set name val / -s name val)",
        aliases = {"-s", "set"},
        isOperator = true,
        operatorArgs = 2,
        mutexWith = {"--get"},
        belongsToOperator = "set",
        operatorIndex = 0,  // 操作符标志本身
        serializedName = "useSet"
    )
    private boolean useSet;

    @CmdParam(
        name = "--set-target",
        description = "要设置的字段名",
        belongsToOperator = "set",
        operatorIndex = 1,  // set 的第1个参数
        required = false,
        serializedName = "setFieldName"
    )
    private String setTargetFieldName;

    @CmdParam(
        name = "--set-value",
        description = "要设置的值",
        readMode = CmdParam.ReadMode.PRESERVED,
        belongsToOperator = "set",
        operatorIndex = 2,  // set 的第2个参数
        required = false,
        serializedName = "setValueToSet"
    )
    private String setValueToSet;

    // ========== 构造函数和访问器 ==========

    public FieldRequest() {
        super();
        this.showAll = true;
    }

    // --- 通用参数 ---
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getTargetInstance() { return targetInstance; }
    public void setTargetInstance(String targetInstance) { this.targetInstance = targetInstance; }

    // --- 显示选项 ---
    public boolean isShowValue() { return showValue; }
    public void setShowValue(boolean showValue) { this.showValue = showValue; }

    public boolean isShowType() { return showType; }
    public void setShowType(boolean showType) { this.showType = showType; }

    public boolean isShowModifiers() { return showModifiers; }
    public void setShowModifiers(boolean showModifiers) { this.showModifiers = showModifiers; }

    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }

    // --- 访问控制 ---
    public boolean isAccessSuper() { return accessSuper; }
    public void setAccessSuper(boolean accessSuper) { this.accessSuper = accessSuper; }

    public boolean isAccessInterfaces() { return accessInterfaces; }
    public void setAccessInterfaces(boolean accessInterfaces) { this.accessInterfaces = accessInterfaces; }

    public boolean isStaticOnly() { return staticOnly; }
    public void setStaticOnly(boolean staticOnly) { this.staticOnly = staticOnly; }

    // --- get 操作符组 ---
    public boolean isUseGet() { return useGet; }
    public void setUseGet(boolean useGet) { this.useGet = useGet; }

    public String getGetTargetFieldName() { return getTargetFieldName; }
    public void setGetTargetFieldName(String getTargetFieldName) { this.getTargetFieldName = getTargetFieldName; }

    // --- set 操作符组 ---
    public boolean isUseSet() { return useSet; }
    public void setUseSet(boolean useSet) { this.useSet = useSet; }

    public String getSetTargetFieldName() { return setTargetFieldName; }
    public void setSetTargetFieldName(String setTargetFieldName) { this.setTargetFieldName = setTargetFieldName; }

    public String getSetValueToSet() { return setValueToSet; }
    public void setSetValueToSet(String setValueToSet) { this.setValueToSet = setValueToSet; }

    /**
     * 获取当前的操作模式（使用基类的 receivedOperators 追踪列表）
     * @return "list", "get", 或 "set"
     */
    public String getOperationMode() {
        // 使用显式追踪的操作符列表
        if (hasOperator("set")) return "set";
        if (hasOperator("get")) return "get";
        return "list";
    }

    /**
     * 获取目标字段名（兼容 get 和 set 模式）
     */
    public String getEffectiveFieldName() {
        if (hasOperator("get")) return getTargetFieldName;
        if (hasOperator("set")) return setTargetFieldName;
        return null;
    }
}
