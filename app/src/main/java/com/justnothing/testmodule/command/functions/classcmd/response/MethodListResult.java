package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class MethodListResult extends ClassCommandResult {

    private String className;
    private String targetPackage;
    private String classLoader;
    private List<MethodInfo> methods;
    private int staticCount;
    private int instanceCount;
    private int totalCount;

    public MethodListResult() {
        super();
        this.methods = new ArrayList<>();
    }

    public MethodListResult(String requestId) {
        super(requestId);
        this.methods = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getTargetPackage() {
        return targetPackage;
    }

    public void setTargetPackage(String targetPackage) {
        this.targetPackage = targetPackage;
    }

    public String getClassLoader() {
        return classLoader;
    }

    public void setClassLoader(String classLoader) {
        this.classLoader = classLoader;
    }

    public List<MethodInfo> getMethods() {
        return methods;
    }

    public void setMethods(List<MethodInfo> methods) {
        this.methods = methods;
    }

    public int getStaticCount() {
        return staticCount;
    }

    public void setStaticCount(int staticCount) {
        this.staticCount = staticCount;
    }

    public int getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(int instanceCount) {
        this.instanceCount = instanceCount;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("targetPackage", targetPackage);
        obj.put("classLoader", classLoader);
        obj.put("staticCount", staticCount);
        obj.put("instanceCount", instanceCount);
        obj.put("totalCount", totalCount);

        JSONArray methodsArray = new JSONArray();
        for (MethodInfo method : methods) {
            methodsArray.put(method.toJson());
        }
        obj.put("methods", methodsArray);

        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        this.className = obj.optString("className");
        this.targetPackage = obj.optString("targetPackage");
        this.classLoader = obj.optString("classLoader");
        this.staticCount = obj.optInt("staticCount", 0);
        this.instanceCount = obj.optInt("instanceCount", 0);
        this.totalCount = obj.optInt("totalCount", 0);

        if (obj.has("methods")) {
            JSONArray arr = obj.getJSONArray("methods");
            List<MethodInfo> list = new ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(MethodInfo.fromJson(arr.getJSONObject(i)));
            }
            this.methods = list;
        }
    }
}
