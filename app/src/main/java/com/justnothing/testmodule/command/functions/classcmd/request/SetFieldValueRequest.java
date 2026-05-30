package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("class:setfield")
public class SetFieldValueRequest extends ClassCommandRequest {

    @CmdParam(
        name = "--class",
        description = "类名",
        position = 1,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--field",
        description = "字段名",
        position = 2,
        serializedName = "fieldName"
    )
    private String fieldName;

    @CmdParam(
        name = "--value",
        description = "值表达式",
        position = 3,
        readMode = CmdParam.ReadMode.PRESERVED,
        serializedName = "valueExpression"
    )
    private String valueExpression;

    @CmdParam(
        name = "--static",
        description = "静态字段",
        aliases = {"-s"},
        serializedName = "static"
    )
    private boolean isStatic;

    @CmdParam(
        name = "--type",
        description = "值类型提示 (支持: --type=String 或 --type String)",
        serializedName = "valueTypeHint"
    )
    private String valueTypeHint;

    private String targetInstance;  // 目标实例表达式 (GUI用)

    public SetFieldValueRequest() {
        super();
        this.isStatic = false;
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getValueExpression() { return valueExpression; }
    public void setValueExpression(String valueExpression) { this.valueExpression = valueExpression; }
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }
    public String getValueTypeHint() { return valueTypeHint; }
    public void setValueTypeHint(String typeHint) { this.valueTypeHint = typeHint; }
    public String getTargetInstance() { return targetInstance; }
    public void setTargetInstance(String targetInstance) { this.targetInstance = targetInstance; }

    @Override
    public SetFieldValueRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(SetFieldValueRequest.class, args);
    }
}
