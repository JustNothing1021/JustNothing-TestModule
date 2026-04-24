package com.justnothing.testmodule.protocol.json.response;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ClassHierarchyResult extends CommandResult {
    
    private List<HierarchyClassInfo> classChain;
    private List<List<String>> interfacesPerLevel;
    
    public ClassHierarchyResult() {
        super();
    }
    
    public ClassHierarchyResult(String requestId) {
        super(requestId);
    }
    
    public List<HierarchyClassInfo> getClassChain() {
        return classChain;
    }
    
    public void setClassChain(List<HierarchyClassInfo> classChain) {
        this.classChain = classChain;
    }
    
    public List<List<String>> getInterfacesPerLevel() {
        return interfacesPerLevel;
    }
    
    public void setInterfacesPerLevel(List<List<String>> interfacesPerLevel) {
        this.interfacesPerLevel = interfacesPerLevel;
    }
    
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        
        if (classChain != null) {
            JSONArray chainArray = new JSONArray();
            for (HierarchyClassInfo info : classChain) {
                chainArray.put(info.toJson());
            }
            obj.put("classChain", chainArray);
        }
        
        if (interfacesPerLevel != null) {
            JSONArray interfacesArray = new JSONArray();
            for (List<String> level : interfacesPerLevel) {
                JSONArray levelArray = new JSONArray();
                for (String iface : level) {
                    levelArray.put(iface);
                }
                interfacesArray.put(levelArray);
            }
            obj.put("interfacesPerLevel", interfacesArray);
        }
        
        return obj;
    }
    
    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        
        classChain = new ArrayList<>();
        if (obj.has("classChain")) {
            JSONArray chainArray = obj.getJSONArray("classChain");
            for (int i = 0; i < chainArray.length(); i++) {
                classChain.add(HierarchyClassInfo.fromJson(chainArray.getJSONObject(i)));
            }
        }
        
        interfacesPerLevel = new ArrayList<>();
        if (obj.has("interfacesPerLevel")) {
            JSONArray interfacesArray = obj.getJSONArray("interfacesPerLevel");
            for (int i = 0; i < interfacesArray.length(); i++) {
                JSONArray levelArray = interfacesArray.getJSONArray(i);
                List<String> level = new ArrayList<>();
                for (int j = 0; j < levelArray.length(); j++) {
                    level.add(levelArray.optString(j));
                }
                interfacesPerLevel.add(level);
            }
        }
    }
    
    public static class HierarchyClassInfo {
        private String name;
        private boolean isInterface;
        private boolean isAnnotation;
        private boolean isEnum;
        private boolean isAbstract;
        private boolean isFinal;
        
        public HierarchyClassInfo() {}
        
        public HierarchyClassInfo(String name, boolean isInterface, boolean isAnnotation, 
                                   boolean isEnum, boolean isAbstract, boolean isFinal) {
            this.name = name;
            this.isInterface = isInterface;
            this.isAnnotation = isAnnotation;
            this.isEnum = isEnum;
            this.isAbstract = isAbstract;
            this.isFinal = isFinal;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public boolean isInterface() { return isInterface; }
        public void setInterface(boolean anInterface) { isInterface = anInterface; }
        public boolean isAnnotation() { return isAnnotation; }
        public void setAnnotation(boolean annotation) { isAnnotation = annotation; }
        public boolean isEnum() { return isEnum; }
        public void setEnum(boolean anEnum) { isEnum = anEnum; }
        public boolean isAbstract() { return isAbstract; }
        public void setAbstract(boolean anAbstract) { isAbstract = anAbstract; }
        public boolean isFinal() { return isFinal; }
        public void setFinal(boolean aFinal) { isFinal = aFinal; }
        
        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("name", name);
            obj.put("isInterface", isInterface);
            obj.put("isAnnotation", isAnnotation);
            obj.put("isEnum", isEnum);
            obj.put("isAbstract", isAbstract);
            obj.put("isFinal", isFinal);
            return obj;
        }
        
        public static HierarchyClassInfo fromJson(JSONObject obj) throws JSONException {
            HierarchyClassInfo info = new HierarchyClassInfo();
            info.setName(obj.optString("name"));
            info.setInterface(obj.optBoolean("isInterface", false));
            info.setAnnotation(obj.optBoolean("isAnnotation", false));
            info.setEnum(obj.optBoolean("isEnum", false));
            info.setAbstract(obj.optBoolean("isAbstract", false));
            info.setFinal(obj.optBoolean("isFinal", false));
            return info;
        }
    }
}
