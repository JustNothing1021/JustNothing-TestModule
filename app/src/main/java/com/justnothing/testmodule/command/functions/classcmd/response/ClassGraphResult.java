package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ClassGraphResult extends ClassCommandResult {

    private String className;
    private List<HierarchyLevel> hierarchy;
    private List<String> subclasses;
    private List<String> implementedInterfaces;
    private boolean success;

    public ClassGraphResult() {
        super();
        this.hierarchy = new ArrayList<>();
        this.subclasses = new ArrayList<>();
        this.implementedInterfaces = new ArrayList<>();
    }

    public ClassGraphResult(String requestId) {
        super(requestId);
        this.hierarchy = new ArrayList<>();
        this.subclasses = new ArrayList<>();
        this.implementedInterfaces = new ArrayList<>();
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public List<HierarchyLevel> getHierarchy() { return hierarchy; }
    public void setHierarchy(List<HierarchyLevel> hierarchy) { this.hierarchy = hierarchy; }
    public List<String> getSubclasses() { return subclasses; }
    public void setSubclasses(List<String> subclasses) { this.subclasses = subclasses; }
    public List<String> getImplementedInterfaces() { return implementedInterfaces; }
    public void setImplementedInterfaces(List<String> implementedInterfaces) { this.implementedInterfaces = implementedInterfaces; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("success", success);

        JSONArray hierarchyArr = new JSONArray();
        for (HierarchyLevel level : hierarchy) {
            hierarchyArr.put(level.toJson());
        }
        obj.put("hierarchy", hierarchyArr);

        if (!subclasses.isEmpty()) {
            JSONArray subArr = new JSONArray();
            for (String sub : subclasses) { subArr.put(sub); }
            obj.put("subclasses", subArr);
        }

        if (!implementedInterfaces.isEmpty()) {
            JSONArray ifaceArr = new JSONArray();
            for (String iface : implementedInterfaces) { ifaceArr.put(iface); }
            obj.put("implementedInterfaces", ifaceArr);
        }

        return obj;
    }

    public static class HierarchyLevel {
        public String className;
        public int depth;
        public List<String> interfaces;

        public HierarchyLevel(String className, int depth, List<String> interfaces) {
            this.className = className;
            this.depth = depth;
            this.interfaces = interfaces != null ? interfaces : new ArrayList<>();
        }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("className", className);
            obj.put("depth", depth);

            if (!interfaces.isEmpty()) {
                JSONArray arr = new JSONArray();
                for (String i : interfaces) { arr.put(i); }
                obj.put("interfaces", arr);
            }

            return obj;
        }
    }
}
