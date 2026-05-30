package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;
import com.justnothing.testmodule.command.utils.ParamParser;
import com.justnothing.testmodule.command.utils.ParamStringUtils;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("class:invoke")
public class InvokeMethodRequest extends ClassCommandRequest {

    @CmdParam(
        name = "--class",
        description = "类名",
        position = 1,
        serializedName = "className"
    )
    private String className;

    @CmdParam(
        name = "--method",
        description = "方法名",
        position = 2,
        serializedName = "methodName"
    )
    private String methodName;

    @CmdParam(
        name = "--params",
        description = "方法参数",
        position = 3,
        varArgs = true,
        serializedName = "paramsRaw"
    )
    private String paramsRaw;

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

    private String signature;
    private String targetInstance;
    private List<String> params;
    private List<String> paramTypes;

    public InvokeMethodRequest() {
        super();
        this.params = new ArrayList<>();
        this.paramTypes = new ArrayList<>();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    
    public String getMethodName() { return methodName; }
    public void setMethodName(String methodName) { this.methodName = methodName; }
    
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

    @Override
    public InvokeMethodRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        InvokeMethodRequest parsed = ParamParser.parse(InvokeMethodRequest.class, args);

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
