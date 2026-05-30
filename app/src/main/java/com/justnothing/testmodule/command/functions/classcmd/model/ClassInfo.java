package com.justnothing.testmodule.command.functions.classcmd.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;
import com.justnothing.testmodule.command.utils.AutoSerializer;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;


@AutoSerializable
public class ClassInfo {

    @Expose @SerializedName("name")
    @ResultField(name = "name", description = "类名", required = true)
    private String name;

    @Expose @SerializedName("superClass")
    @ResultField(name = "superClass", description = "父类", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String superClass;

    @Expose @SerializedName("interfaces")
    @ResultField(name = "interfaces", description = "接口列表")
    private List<String> interfaces;

    @Expose @SerializedName("methods")
    @ResultField(name = "methods", description = "方法列表")
    private List<MethodInfo> methods;

    @Expose @SerializedName("constructors")
    @ResultField(name = "constructors", description = "构造函数列表")
    private List<MethodInfo> constructors;

    @Expose @SerializedName("fields")
    @ResultField(name = "fields", description = "字段列表")
    private List<FieldInfo> fields;

    @Expose @SerializedName("modifiers")
    @ResultField(name = "modifiers", description = "修饰符", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int modifiers;

    @Expose @SerializedName("classLoader")
    @ResultField(name = "classLoader", description = "类加载器", defaultValue = ValueSupplier.EmptyStringSupplier.class)
    private String classLoader;

    @Expose @SerializedName("isInterface")
    @ResultField(name = "isInterface", description = "是否为接口", defaultValue = ValueSupplier.FalseSupplier.class)
    private boolean isInterface;

    @Expose @SerializedName("isAnnotation")
    @ResultField(name = "isAnnotation", description = "是否为注解", defaultValue = ValueSupplier.FalseSupplier.class)
    private boolean isAnnotation;

    @Expose @SerializedName("isEnum")
    @ResultField(name = "isEnum", description = "是否为枚举", defaultValue = ValueSupplier.FalseSupplier.class)
    private boolean isEnum;

    @Expose @SerializedName("isAbstract")
    @ResultField(name = "isAbstract", description = "是否为抽象类", defaultValue = ValueSupplier.FalseSupplier.class)
    private boolean isAbstract;

    @Expose @SerializedName("isFinal")
    @ResultField(name = "isFinal", description = "是否为final类", defaultValue = ValueSupplier.FalseSupplier.class)
    private boolean isFinal;
    
    public ClassInfo() {
        this.interfaces = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.constructors = new ArrayList<>();
        this.fields = new ArrayList<>();
    }
    
    public static ClassInfo fromClass(Class<?> clazz) {
        ClassInfo classInfo = new ClassInfo();
        classInfo.setName(clazz.getName());
        classInfo.setModifiers(clazz.getModifiers());
        classInfo.setInterface(clazz.isInterface());
        classInfo.setAnnotation(clazz.isAnnotation());
        classInfo.setEnum(clazz.isEnum());
        classInfo.setAbstract(Modifier.isAbstract(clazz.getModifiers()));
        classInfo.setFinal(Modifier.isFinal(clazz.getModifiers()));
        return classInfo;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getSuperClass() {
        return superClass;
    }
    
    public void setSuperClass(String superClass) {
        this.superClass = superClass;
    }
    
    public List<String> getInterfaces() {
        return interfaces;
    }
    
    public void setInterfaces(List<String> interfaces) {
        this.interfaces = interfaces;
    }
    
    public List<MethodInfo> getMethods() {
        return methods;
    }
    
    public void setMethods(List<MethodInfo> methods) {
        this.methods = methods;
    }
    
    public List<MethodInfo> getConstructors() {
        return constructors;
    }
    
    public void setConstructors(List<MethodInfo> constructors) {
        this.constructors = constructors;
    }
    
    public List<FieldInfo> getFields() {
        return fields;
    }
    
    public void setFields(List<FieldInfo> fields) {
        this.fields = fields;
    }
    
    public int getModifiers() {
        return modifiers;
    }
    
    public void setModifiers(int modifiers) {
        this.modifiers = modifiers;
    }
    
    public String getClassLoader() {
        return classLoader;
    }
    
    public void setClassLoader(String classLoader) {
        this.classLoader = classLoader;
    }
    
    public String getModifiersString() {
        String result = Modifier.toString(modifiers);
        if (isInterface && result.contains("abstract")) {
            result = result.replace("abstract", "").trim().replaceAll(" +", " ");
        }
        return result;
    }
    
    public boolean isInterface() {
        return isInterface;
    }
    
    public void setInterface(boolean anInterface) {
        isInterface = anInterface;
    }
    
    public boolean isAnnotation() {
        return isAnnotation;
    }
    
    public void setAnnotation(boolean annotation) {
        isAnnotation = annotation;
    }
    
    public boolean isEnum() {
        return isEnum;
    }
    
    public void setEnum(boolean anEnum) {
        isEnum = anEnum;
    }
    
    public boolean isAbstract() {
        return isAbstract;
    }
    
    public void setAbstract(boolean anAbstract) {
        isAbstract = anAbstract;
    }
    
    public boolean isFinal() {
        return isFinal;
    }
    
    public void setFinal(boolean aFinal) {
        isFinal = aFinal;
    }

    public JSONObject toJson() {
        try {
            String jsonStr = AutoSerializer.toJson(this);
            return new org.json.JSONObject(jsonStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ClassInfo", e);
        }
    }
}
