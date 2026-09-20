package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.framework.error.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.classcmd.ClassTexts;
import com.justnothing.testmodule.command.framework.model.CustomCommandLineParser;
import com.justnothing.testmodule.command.functions.classcmd.response.ReflectOperationResult;

import java.util.ArrayList;
import java.util.List;

public class ReflectClassRequest extends ClassCommandRequest<ReflectOperationResult> 
        implements CustomCommandLineParser {

    @CmdParam(
        name = "class",
        description = ClassTexts.PARAM_CLASS_REFLECT_CLASS_DESC,
        position = 1,
        required = true,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "operation",
        description = ClassTexts.PARAM_CLASS_REFLECT_OPERATION_DESC,
        position = 2,
        required = true,
        serializedName = "operationType"
    )
    private String operationType;

    @CmdParam(
        name = "member",
        description = ClassTexts.PARAM_CLASS_REFLECT_MEMBER_DESC,
        position = 3,
        required = true,
        serializedName = "memberName"
    )
    private String memberName;

    @CmdParam(
        name = "--super",
        description = ClassTexts.PARAM_CLASS_REFLECT_SUPER_DESC,
        aliases = {"-s"},
        serializedName = "accessSuper"
    )
    private boolean accessSuper;

    @CmdParam(
        name = "--interfaces",
        description = ClassTexts.PARAM_CLASS_REFLECT_INTERFACES_DESC,
        aliases = {"-i"},
        serializedName = "accessInterfaces"
    )
    private boolean accessInterfaces;

    @CmdParam(
        name = "--raw",
        description = ClassTexts.PARAM_CLASS_REFLECT_RAW_DESC,
        aliases = {"-r"},
        serializedName = "rawOutput"
    )
    private boolean rawOutput;

    // ★ 复杂字段: 由customParse()处理!
    private String valueToSet;      // -v/--value 消费1个参数
    private List<String> params;     // -p/--params 消费N个参数

    public ReflectClassRequest() {
        super();
        this.params = new ArrayList<>();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public boolean isAccessSuper() { return accessSuper; }
    public void setAccessSuper(boolean accessSuper) { this.accessSuper = accessSuper; }
    public boolean isAccessInterfaces() { return accessInterfaces; }
    public void setAccessInterfaces(boolean accessInterfaces) { this.accessInterfaces = accessInterfaces; }
    public boolean isRawOutput() { return rawOutput; }
    public void setRawOutput(boolean rawOutput) { this.rawOutput = rawOutput; }
    public String getValueToSet() { return valueToSet; }
    public void setValueToSet(String valueToSet) { this.valueToSet = valueToSet; }
    public List<String> getParams() { return params; }
    public void setParams(List<String> params) { this.params = params; }

    /**
     * ★ 自定义解析: 处理 -v/-p 参数及其可变数量消费
     */
    @Override
    public ReflectClassRequest customParse(ParseContext ctx) throws IllegalCommandLineArgumentException {
        List<String> args = ctx.remainingArgs();
        boolean valueSet = false;
        
        for (int i = 0; i < args.size(); i++) {
            switch (args.get(i)) {
                case "-v", "--value" -> {
                    if (i + 1 < args.size()) {
                        this.valueToSet = args.get(i + 1);
                        valueSet = true;
                        i++;
                    } else {
                        throw new IllegalCommandLineArgumentException(
                            "-v/--value 需要一个参数值");
                    }
                }
                case "-p", "--params" -> {
                    while (i + 1 < args.size() && !args.get(i + 1).startsWith("-")) {
                        this.params.add(args.get(i + 1));
                        i++;
                    }
                }
                default -> {
                    // 兜底逻辑: 未识别的非选项参数自动赋给valueToSet
                    if (!args.get(i).startsWith("-") && !valueSet) {
                        this.valueToSet = args.get(i);
                        valueSet = true;
                    }
                }
            }
        }
        
        return this;
    }

}
