package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.functions.classcmd.response.GetFieldValueResult;

public class FieldRequest extends ClassCommandRequest<GetFieldValueResult> {

    // ========== 通用参数（不属于任何操作符）==========

    @CmdParam(
        name = "class",
        description = ClassTexts.PARAM_CLASS_FIELD_CLASS_DESC,
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--instance",
        description = ClassTexts.PARAM_CLASS_FIELD_INSTANCE_DESC,
        aliases = {"-i"},
        required = false,
        readMode = CmdParam.ReadMode.PRESERVED,
        serializedName = "targetInstance"
    )
    private String targetInstance;

    // ========== 显示选项（不属于任何操作符）==========

    @CmdParam(
        name = "--value",
        description = ClassTexts.PARAM_CLASS_FIELD_VALUE_DESC,
        aliases = {"-v"},
        serializedName = "showValue"
    )
    private boolean showValue;

    @CmdParam(
        name = "--type",
        description = ClassTexts.PARAM_CLASS_FIELD_TYPE_DESC,
        aliases = {"-t"},
        serializedName = "showType"
    )
    private boolean showType;

    @CmdParam(
        name = "--modifiers",
        description = ClassTexts.PARAM_CLASS_FIELD_MODIFIERS_DESC,
        aliases = {"-m"},
        serializedName = "showModifiers"
    )
    private boolean showModifiers;

    @CmdParam(
        name = "--all",
        description = ClassTexts.PARAM_CLASS_FIELD_ALL_DESC,
        aliases = {"-a"},
        serializedName = "showAll"
    )
    private boolean showAll;

    // ========== 访问控制选项（不属于任何操作符）==========

    @CmdParam(
        name = "--super",
        description = ClassTexts.PARAM_CLASS_FIELD_SUPER_DESC,
        serializedName = "accessSuper"
    )
    private boolean accessSuper;

    @CmdParam(
        name = "--interfaces",
        description = ClassTexts.PARAM_CLASS_FIELD_INTERFACES_DESC,
        serializedName = "accessInterfaces"
    )
    private boolean accessInterfaces;

    @CmdParam(
        name = "--static",
        description = ClassTexts.PARAM_CLASS_FIELD_STATIC_DESC,
        aliases = {"--static-only"},
        serializedName = "staticOnly"
    )
    private boolean staticOnly;

    // ========== get 操作符组 (分离存储) ==========

    @CmdParam(
        name = "--get",
        description = ClassTexts.PARAM_CLASS_FIELD_GET_DESC,
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
        description = ClassTexts.PARAM_CLASS_FIELD_GET_TARGET_DESC,
        belongsToOperator = "get",
        operatorIndex = 1,  // get 的第1个参数
        required = false,
        serializedName = "fieldName"
    )
    private String getTargetFieldName;

    // ========== set 操作符组 (分离存储) ==========

    @CmdParam(
        name = "--set",
        description = ClassTexts.PARAM_CLASS_FIELD_SET_DESC,
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
        description = ClassTexts.PARAM_CLASS_FIELD_SET_TARGET_DESC,
        belongsToOperator = "set",
        operatorIndex = 1,  // set 的第1个参数
        required = false,
        serializedName = "setFieldName"
    )
    private String setTargetFieldName;

    @CmdParam(
        name = "--set-value",
        description = ClassTexts.PARAM_CLASS_FIELD_SET_VALUE_DESC,
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
        // receivedOperators 只在命令行解析（CmdArgParser.handleOperator）时填充；
        // GUI 走 JSON 反序列化，这里为空，所以还要认 useGet/useSet 这两个可序列化字段。
        if (hasOperator("set") || useSet) return "set";
        if (hasOperator("get") || useGet) return "get";
        return "list";
    }

    /**
     * 获取目标字段名（兼容 get 和 set 模式）
     */
    public String getEffectiveFieldName() {
        if (hasOperator("get") || useGet) return getTargetFieldName;
        if (hasOperator("set") || useSet) return setTargetFieldName;
        return null;
    }
}
