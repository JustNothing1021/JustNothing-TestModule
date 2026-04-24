package com.justnothing.testmodule.protocol.json.model;

import org.json.JSONObject;
import org.json.JSONException;

import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldInfo {
    
    private String name;
    private String type;
    private String genericType;
    private int modifiers;
    private String declaringClass;
    private boolean declaringClassIsInterface;
    
    public FieldInfo() {
    }
    
    public static FieldInfo fromField(Field field) {
        FieldInfo fieldInfo = new FieldInfo();
        fieldInfo.setName(field.getName());
        fieldInfo.setType(DescriptorColorizer.formatTypeName(field.getType().getName()));
        fieldInfo.setGenericType(DescriptorColorizer.formatTypeName(field.getGenericType().toString()));
        fieldInfo.setModifiers(field.getModifiers());
        fieldInfo.setDeclaringClass(field.getDeclaringClass().getName());
        fieldInfo.setDeclaringClassIsInterface(field.getDeclaringClass().isInterface());
        return fieldInfo;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getGenericType() { return genericType; }
    public void setGenericType(String genericType) { this.genericType = genericType; }
    public int getModifiers() { return modifiers; }
    public void setModifiers(int modifiers) { this.modifiers = modifiers; }
    public String getDeclaringClass() { return declaringClass; }
    public void setDeclaringClass(String declaringClass) { this.declaringClass = declaringClass; }
    public boolean isDeclaringClassIsInterface() { return declaringClassIsInterface; }
    public void setDeclaringClassIsInterface(boolean declaringClassIsInterface) { this.declaringClassIsInterface = declaringClassIsInterface; }
    
    public String getModifiersString() {
        String result = Modifier.toString(modifiers);
        if (declaringClassIsInterface) {
            if (result.contains("public")) {
                result = result.replace("public", "").trim().replaceAll(" +", " ");
            }
            if (result.contains("static")) {
                result = result.replace("static", "").trim().replaceAll(" +", " ");
            }
            if (result.contains("final")) {
                result = result.replace("final", "").trim().replaceAll(" +", " ");
            }
        }
        return result;
    }
    
    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("type", type);
        obj.put("genericType", genericType);
        obj.put("modifiers", getModifiersString());
        obj.put("declaringClass", declaringClass);
        obj.put("declaringClassIsInterface", declaringClassIsInterface);
        return obj;
    }
}
