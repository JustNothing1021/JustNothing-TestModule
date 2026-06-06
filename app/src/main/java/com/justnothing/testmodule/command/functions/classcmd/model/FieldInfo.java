package com.justnothing.testmodule.command.functions.classcmd.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.GsonFactory;

import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class FieldInfo {

    @Expose @SerializedName("name")
    private String name;

    @Expose @SerializedName("type")
    private String type;

    @Expose @SerializedName("genericType")
    private String genericType;

    @Expose @SerializedName("modifiers")
    private int modifiers;

    @Expose @SerializedName("declaringClass")
    private String declaringClass;

    @Expose @SerializedName("declaringClassIsInterface")
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

    public JSONObject toJson() {
        try {
            String jsonStr = GsonFactory.getInstance().toJson(this);
            return new org.json.JSONObject(jsonStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize FieldInfo", e);
        }
    }
}
