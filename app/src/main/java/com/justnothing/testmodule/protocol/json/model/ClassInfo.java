package com.justnothing.testmodule.protocol.json.model;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;


public class ClassInfo {
    
    private String name;
    private String superClass;
    private List<String> interfaces;
    private List<MethodInfo> methods;
    private List<MethodInfo> constructors;
    private List<FieldInfo> fields;
    private int modifiers;
    private String classLoader;
    private boolean isInterface;
    private boolean isAnnotation;
    private boolean isEnum;
    private boolean isAbstract;
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
    

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("superClass", superClass);
        obj.put("modifiers", modifiers);
        obj.put("classLoader", classLoader);
        obj.put("isInterface", isInterface);
        obj.put("isAnnotation", isAnnotation);
        obj.put("isEnum", isEnum);
        obj.put("isAbstract", isAbstract);
        obj.put("isFinal", isFinal);
        
        JSONArray interfacesArray = new JSONArray();
        for (String _interface : interfaces) {
            interfacesArray.put(_interface);
        }
        obj.put("interfaces", interfacesArray);
        
        JSONArray methodsArray = new JSONArray();
        for (MethodInfo method : methods) {
            methodsArray.put(method.toJson());
        }
        obj.put("methods", methodsArray);
        
        JSONArray constructorsArray = new JSONArray();
        for (MethodInfo constructor : constructors) {
            constructorsArray.put(constructor.toJson());
        }
        obj.put("constructors", constructorsArray);
        
        JSONArray fieldsArray = new JSONArray();
        for (FieldInfo field : fields) {
            fieldsArray.put(field.toJson());
        }
        obj.put("fields", fieldsArray);
        
        return obj;
    }
}
