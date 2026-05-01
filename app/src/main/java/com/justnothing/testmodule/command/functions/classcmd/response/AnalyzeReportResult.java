package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeReportResult extends ClassCommandResult {

    private String className;
    private ClassInfo classInfo;
    private List<FieldInfo> fields;
    private List<MethodInfo> methods;
    private List<MethodInfo> constructors;
    private List<String> interfaces;
    private String superClass;
    private boolean success;

    public AnalyzeReportResult() {
        super();
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.constructors = new ArrayList<>();
        this.interfaces = new ArrayList<>();
    }

    public AnalyzeReportResult(String requestId) {
        super(requestId);
        this.fields = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.constructors = new ArrayList<>();
        this.interfaces = new ArrayList<>();
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
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("success", success);

        if (classInfo != null) {
            obj.put("classInfo", classInfo.toJson());
        }

        if (!fields.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (FieldInfo f : fields) { arr.put(f.toJson()); }
            obj.put("fields", arr);
        }

        if (!methods.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (MethodInfo m : methods) { arr.put(m.toJson()); }
            obj.put("methods", arr);
        }

        if (!constructors.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (MethodInfo c : constructors) { arr.put(c.toJson()); }
            obj.put("constructors", arr);
        }

        if (!interfaces.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (String i : interfaces) { arr.put(i); }
            obj.put("interfaces", arr);
        }

        if (superClass != null) {
            obj.put("superClass", superClass);
        }

        return obj;
    }
}
