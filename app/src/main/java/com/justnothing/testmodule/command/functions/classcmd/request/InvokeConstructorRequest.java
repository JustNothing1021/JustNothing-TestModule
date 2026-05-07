package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.command.SubCommand;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.utils.ParamStringUtils;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("InvokeConstructor")
@SubCommand("constructor")
@AutoSerializable
public class InvokeConstructorRequest extends ClassCommandRequest {

    @PositionalParam(order = 1, name = "类名", required = true)
    private String className;

    private String signature;

    @PositionalParam(order = 2, name = "构造函数参数", required = false, varArgs = true)
    private String paramsRaw;

    private List<String> params;

    private List<String> paramTypes;

    @FlagParam(names = {"-f", "--free"}, description = "自由模式（使用表达式语法）")
    private boolean freeMode = false;
    
    public InvokeConstructorRequest() {
        super();
        this.params = new ArrayList<>();
        this.paramTypes = new ArrayList<>();
    }
    
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    
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
    

    @Override
    public InvokeConstructorRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        // ★ 改用ParamParser进行标准参数解析!
        InvokeConstructorRequest parsed = ParamParser.parse(InvokeConstructorRequest.class, args);
        
        // ★ 使用ParamStringUtils正确处理引号、转义字符!
        if (parsed.paramsRaw != null && !parsed.paramsRaw.isEmpty()) {
            List<ParamStringUtils.ParamToken> paramTokens = ParamStringUtils.parseParams(parsed.paramsRaw);
            
            for (ParamStringUtils.ParamToken token : paramTokens) {
                parsed.getParams().add(token.value());
                parsed.getParamTypes().add(token.typeHint());
            }
        }

        return parsed;
    }
}
