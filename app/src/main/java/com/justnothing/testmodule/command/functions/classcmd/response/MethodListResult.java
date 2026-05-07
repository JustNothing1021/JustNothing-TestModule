package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("MethodList")
@AutoSerializable
public class MethodListResult extends ClassCommandResult {

    @ResultField(name = "className")
    private String className;

    @ResultField(name = "targetPackage")
    private String targetPackage;

    @ResultField(name = "classLoader")
    private String classLoader;

    @ResultField(name = "methods", defaultValue = ValueSupplier.EmptyListSupplier.class)
    private List<MethodInfo> methods = new ArrayList<>();

    @ResultField(name = "staticCount", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int staticCount;

    @ResultField(name = "instanceCount", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int instanceCount;

    @ResultField(name = "totalCount", defaultValue = ValueSupplier.ZeroSupplier.class)
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
