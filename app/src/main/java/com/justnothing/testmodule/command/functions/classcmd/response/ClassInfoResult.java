package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.ClassInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import java.lang.reflect.Modifier;

public class ClassInfoResult extends ClassCommandResult {
    
    private ClassInfo classInfo;
    
    public ClassInfoResult() {
        super();
    }
    
    public ClassInfoResult(String requestId) {
        super(requestId);
    }
    
    public ClassInfo getClassInfo() {
        return classInfo;
    }
    
    public void setClassInfo(ClassInfo classInfo) {
        this.classInfo = classInfo;
    }
    
    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (classInfo != null) {
            obj.put("classInfo", classInfo.toJson());
        }
        return obj;
    }
    
    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        if (obj.has("classInfo")) {
            JSONObject classInfoObj = obj.getJSONObject("classInfo");
            classInfo = parseClassInfo(classInfoObj);
        }
    }
    
    private static ClassInfo parseClassInfo(JSONObject obj) throws JSONException {
        ClassInfo info = new ClassInfo();
        info.setName(obj.optString("name"));
        info.setSuperClass(obj.optString("superClass"));
        info.setModifiers(obj.optInt("modifiers", 0));
        info.setInterface(obj.optBoolean("isInterface", false));
        info.setAnnotation(obj.optBoolean("isAnnotation", false));
        info.setEnum(obj.optBoolean("isEnum", false));
        info.setAbstract(obj.optBoolean("isAbstract", false));
        info.setFinal(obj.optBoolean("isFinal", false));
        
        if (obj.has("interfaces")) {
            JSONArray arr = obj.getJSONArray("interfaces");
            java.util.List<String> list = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.optString(i));
            }
            info.setInterfaces(list);
        }
        
        if (obj.has("methods")) {
            JSONArray arr = obj.getJSONArray("methods");
            java.util.List<MethodInfo> list = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(parseMethodInfo(arr.getJSONObject(i)));
            }
            info.setMethods(list);
        }
        
        if (obj.has("constructors")) {
            JSONArray arr = obj.getJSONArray("constructors");
            java.util.List<MethodInfo> list = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(parseMethodInfo(arr.getJSONObject(i)));
            }
            info.setConstructors(list);
        }
        
        if (obj.has("fields")) {
            JSONArray arr = obj.getJSONArray("fields");
            java.util.List<FieldInfo> list = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(parseFieldInfo(arr.getJSONObject(i)));
            }
            info.setFields(list);
        }
        
        return info;
    }
    
    private static MethodInfo parseMethodInfo(JSONObject obj) throws JSONException {
        MethodInfo info = new MethodInfo();
        info.setName(obj.optString("name"));
        info.setReturnType(obj.optString("returnType"));
        info.setGenericReturnType(obj.optString("genericReturnType", obj.optString("returnType")));
        
        String modifiersStr = obj.optString("modifiers", "");
        if (!modifiersStr.isEmpty()) {
            info.setModifiers(parseModifiers(modifiersStr));
        }
        
        info.setDeclaringClass(obj.optString("declaringClass", null));
        info.setDeclaringClassIsInterface(obj.optBoolean("declaringClassIsInterface", false));
        
        if (obj.has("parameters")) {
            JSONArray arr = obj.getJSONArray("parameters");
            java.util.List<String> list = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.optString(i));
            }
            info.setParameters(list);
        }
        
        if (obj.has("parameterTypes")) {
            JSONArray arr = obj.getJSONArray("parameterTypes");
            java.util.List<String> list = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.optString(i));
            }
            info.setParameterTypes(list);
        }
        
        if (obj.has("genericParameterTypes")) {
            JSONArray arr = obj.getJSONArray("genericParameterTypes");
            java.util.List<String> list = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.optString(i));
            }
            info.setGenericParameterTypes(list);
        } else if (obj.has("parameterTypes")) {
            JSONArray arr = obj.getJSONArray("parameterTypes");
            java.util.List<String> list = new java.util.ArrayList<>();
            for (int i = 0; i < arr.length(); i++) {
                list.add(arr.optString(i));
            }
            info.setGenericParameterTypes(list);
        }
        
        return info;
    }
    
    private static int parseModifiers(String modifiersStr) {
        int modifiers = 0;
        if (modifiersStr.contains("public")) modifiers |= Modifier.PUBLIC;
        if (modifiersStr.contains("private")) modifiers |= Modifier.PRIVATE;
        if (modifiersStr.contains("protected")) modifiers |= Modifier.PROTECTED;
        if (modifiersStr.contains("static")) modifiers |= Modifier.STATIC;
        if (modifiersStr.contains("final")) modifiers |= Modifier.FINAL;
        if (modifiersStr.contains("synchronized")) modifiers |= Modifier.SYNCHRONIZED;
        if (modifiersStr.contains("volatile")) modifiers |= Modifier.VOLATILE;
        if (modifiersStr.contains("transient")) modifiers |= Modifier.TRANSIENT;
        if (modifiersStr.contains("native")) modifiers |= Modifier.NATIVE;
        if (modifiersStr.contains("interface")) modifiers |= Modifier.INTERFACE;
        if (modifiersStr.contains("abstract")) modifiers |= Modifier.ABSTRACT;
        if (modifiersStr.contains("strictfp")) modifiers |= Modifier.STRICT;
        return modifiers;
    }
    
    private static FieldInfo parseFieldInfo(JSONObject obj) throws JSONException {
        FieldInfo info = new FieldInfo();
        info.setName(obj.optString("name"));
        info.setType(obj.optString("type"));
        info.setGenericType(obj.optString("genericType", obj.optString("type")));
        
        String modifiersStr = obj.optString("modifiers", "");
        if (!modifiersStr.isEmpty()) {
            info.setModifiers(parseModifiers(modifiersStr));
        }
        
        info.setDeclaringClass(obj.optString("declaringClass", null));
        info.setDeclaringClassIsInterface(obj.optBoolean("declaringClassIsInterface", false));
        return info;
    }
}
