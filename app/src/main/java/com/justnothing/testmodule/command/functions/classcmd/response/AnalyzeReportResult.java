package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("AnalyzeReport")
public class AnalyzeReportResult extends ClassCommandResult {

    private String className;

    private ClassInfo classInfo;

    private List<FieldInfo> fields = new ArrayList<>();

    private List<MethodInfo> methods = new ArrayList<>();

    private List<MethodInfo> constructors = new ArrayList<>();

    private List<String> interfaces = new ArrayList<>();

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
