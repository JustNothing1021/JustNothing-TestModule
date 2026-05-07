package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.utils.CustomCommandLineParser;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("ReflectClass")
@SubCommand("reflect")
@AutoSerializable
public class ReflectClassRequest extends ClassCommandRequest 
        implements CustomCommandLineParser {

    @PositionalParam(order = 1, name = "类名", required = true)
    private String className;

    @PositionalParam(order = 2, name = "操作类型", required = true)
    private String operationType;

    @PositionalParam(order = 3, name = "成员名称", required = true)
    private String memberName;

    @FlagParam(names = {"-s", "--super"}, description = "访问父类成员")
    private boolean accessSuper;

    @FlagParam(names = {"-i", "--interfaces"}, description = "访问接口成员")
    private boolean accessInterfaces;

    @FlagParam(names = {"-r", "--raw"}, description = "原始输出格式")
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

    @Override
    public ReflectClassRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        return (ReflectClassRequest) ParamParser.parse(ReflectClassRequest.class, args);
    }
}
