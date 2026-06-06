package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("MethodList")
public class MethodListResult extends ClassCommandResult {

    private String className;

    private String targetPackage;

    private String classLoader;

    private List<MethodInfo> methods = new ArrayList<>();

    private int staticCount;

    private int instanceCount;

    private int totalCount;

    public MethodListResult() {
        super();
    }

    public MethodListResult(String requestId) {
        super(requestId);
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getTargetPackage() { return targetPackage; }
    public void setTargetPackage(String targetPackage) { this.targetPackage = targetPackage; }
    public String getClassLoader() { return classLoader; }
    public void setClassLoader(String classLoader) { this.classLoader = classLoader; }
    public List<MethodInfo> getMethods() { return methods; }
    public void setMethods(List<MethodInfo> methods) { this.methods = methods; }
    public int getStaticCount() { return staticCount; }
    public void setStaticCount(int staticCount) { this.staticCount = staticCount; }
    public int getInstanceCount() { return instanceCount; }
    public void setInstanceCount(int instanceCount) { this.instanceCount = instanceCount; }
    public int getTotalCount() { return totalCount; }
    public void setTotalCount(int totalCount) { this.totalCount = totalCount; }
}
