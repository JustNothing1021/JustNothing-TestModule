package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("AnalyzeReport")
@AutoSerializable
public class AnalyzeReportResult extends ClassCommandResult {

    @ResultField(name = "className")
    private String className;

    @ResultField(name = "classInfo")
    private ClassInfo classInfo;

    @ResultField(name = "fields", defaultValue = ValueSupplier.EmptyListSupplier.class)
    private List<FieldInfo> fields = new ArrayList<>();

    @ResultField(name = "methods", defaultValue = ValueSupplier.EmptyListSupplier.class)
    private List<MethodInfo> methods = new ArrayList<>();

    @ResultField(name = "constructors", defaultValue = ValueSupplier.EmptyListSupplier.class)
    private List<MethodInfo> constructors = new ArrayList<>();

    @ResultField(name = "interfaces", defaultValue = ValueSupplier.EmptyListSupplier.class)
    private List<String> interfaces = new ArrayList<>();

    @ResultField(name = "superClass")
    private String superClass;

    public AnalyzeReportResult() {
        super();
    }

    public AnalyzeReportResult(String requestId) {
        super(requestId);
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public ClassInfo getClassInfo() { return classInfo; }
    public void setClassInfo(ClassInfo classInfo) { this.classInfo = classInfo; }
    public List<FieldInfo> getFields() { return fields; }
    public void setFields(List<FieldInfo> fields) { this.fields = fields; }
    public List<MethodInfo> getMethods() { return methods; }
    public void setMethods(List<MethodInfo> methods) { this.methods = methods; }
    public List<MethodInfo> getConstructors() { return constructors; }
    public void setConstructors(List<MethodInfo> constructors) { this.constructors = constructors; }
    public List<String> getInterfaces() { return interfaces; }
    public void setInterfaces(List<String> interfaces) { this.interfaces = interfaces; }
    public String getSuperClass() { return superClass; }
    public void setSuperClass(String superClass) { this.superClass = superClass; }
}
