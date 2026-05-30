package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.utils.CustomCommandLineParser;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("class:getfield")
public class GetFieldValueRequest extends ClassCommandRequest 
        implements CustomCommandLineParser {

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
        description = "显示所有信息",
        aliases = {"-a"},
        serializedName = "showAll"
    )
    private boolean showAll;

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
        description = "静态字段",
        aliases = {"-s"},
        serializedName = "static"
    )
    private boolean isStatic;

    // ★ 复杂字段: 由customParse()处理!
    private String operation;       // "list"/"get"/"set"/"info"
    private String valueToSet;      // 设置操作时的目标值
    private String targetInstance;  // 目标实例表达式 (GUI用)

    public GetFieldValueRequest() {
        super();
        this.showAll = true;
        this.operation = "list";
    }

    // Getters & Setters
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public boolean isShowValue() { return showValue; }
    public void setShowValue(boolean showValue) { this.showValue = showValue; }
    public boolean isShowType() { return showType; }
    public void setShowType(boolean showType) { this.showType = showType; }
    public boolean isShowModifiers() { return showModifiers; }
    public void setShowModifiers(boolean showModifiers) { this.showModifiers = showModifiers; }
    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }
    public boolean isAccessSuper() { return accessSuper; }
    public void setAccessSuper(boolean accessSuper) { this.accessSuper = accessSuper; }
    public boolean isAccessInterfaces() { return accessInterfaces; }
    public void setAccessInterfaces(boolean accessInterfaces) { this.accessInterfaces = accessInterfaces; }
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getValueToSet() { return valueToSet; }
    public void setValueToSet(String valueToSet) { this.valueToSet = valueToSet; }
    public String getTargetInstance() { return targetInstance; }
    public void setTargetInstance(String targetInstance) { this.targetInstance = targetInstance; }

    /**
     * ★ 自定义解析: 处理 -s/-g 操作符及其参数消费
     */
    @Override
    public GetFieldValueRequest customParse(ParseContext ctx) throws IllegalCommandLineArgumentException {
        List<String> args = ctx.remainingArgs();
        
        for (int i = 0; i < args.size(); i++) {
            switch (args.get(i)) {
                case "-g", "--get" -> {
                    operation = "get";
                    showAll = false;
                }
                case "-s", "--set" -> {
                    operation = "set";
                    showAll = false;
                    if (i + 3 < args.size()) {
                        className = args.get(i + 1);
                        fieldName = args.get(i + 2);
                        valueToSet = args.get(i + 3);
                        i += 3;
                    } else {
                        throw new IllegalCommandLineArgumentException(
                            "-s/--set 需要三个参数: <类名> <字段名> <值>");
                    }
                }
            }
        }
        
        if (className == null && !args.isEmpty()) {
            className = args.get(args.size() - 1);
        }
        if (fieldName == null && args.size() >= 2) {
            fieldName = args.get(args.size() - 2);
        }
        
        return this;
    }

    @Override
    public GetFieldValueRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return ParamParser.parse(GetFieldValueRequest.class, args);
    }
}
