package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.parser.KeywordParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

@SerializeKeyName("SetField")
@SubCommand("setfield")
@AutoSerializable
public class SetFieldValueRequest extends ClassCommandRequest {

    @PositionalParam(order = 1, name = "类名")
    private String className;

    @PositionalParam(order = 2, name = "字段名")
    private String fieldName;

    @PositionalParam(order = 3, name = "值表达式", description = "要设置的值 (支持: 字符串/数字/null)")
    private String valueExpression;

    @FlagParam(names = {"-s", "--static"}, description = "静态字段")
    private boolean isStatic;

    @KeywordParam(name = "type", names = {"t"}, 
                 description = "值类型提示 (支持: --type=String 或 --type String)")
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
