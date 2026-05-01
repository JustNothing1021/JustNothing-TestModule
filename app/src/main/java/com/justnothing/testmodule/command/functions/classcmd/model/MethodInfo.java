package com.justnothing.testmodule.command.functions.classcmd.model;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MethodInfo {

    private String name;
    private String returnType;
    private String genericReturnType;
    private List<String> parameters;
    private List<String> parameterTypes;
    private List<String> genericParameterTypes;
    private int modifiers;
    private String declaringClass;
    private boolean declaringClassIsInterface;

    public MethodInfo() {
        this.parameters = new ArrayList<>();
        this.parameterTypes = new ArrayList<>();
        this.genericParameterTypes = new ArrayList<>();
    }

    public static MethodInfo fromMethod(Method method) {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.setName(method.getName());
        methodInfo.setReturnType(DescriptorColorizer.formatTypeName(method.getReturnType().getName()));
        methodInfo.setGenericReturnType(DescriptorColorizer.formatTypeName(method.getGenericReturnType().toString()));
        methodInfo.setModifiers(method.getModifiers());
        methodInfo.setDeclaringClass(method.getDeclaringClass().getName());
        methodInfo.setDeclaringClassIsInterface(method.getDeclaringClass().isInterface());

        Type[] genericParamTypes = method.getGenericParameterTypes();
        for (Type genericParamType : genericParamTypes) {
            methodInfo.getGenericParameterTypes().add(DescriptorColorizer.formatTypeName(genericParamType.toString()));
        }

        for (Parameter parameter : method.getParameters()) {
            methodInfo.getParameters().add(parameter.getName());
            methodInfo.getParameterTypes().add(DescriptorColorizer.formatTypeName(parameter.getType().getName()));
        }

        return methodInfo;
    }

    public static MethodInfo fromConstructor(Constructor<?> constructor) {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.setName(constructor.getName());
        methodInfo.setReturnType("void");
        methodInfo.setGenericReturnType("void");
        methodInfo.setModifiers(constructor.getModifiers());
        methodInfo.setDeclaringClass(constructor.getDeclaringClass().getName());
        methodInfo.setDeclaringClassIsInterface(false);

        Type[] genericParamTypes = constructor.getGenericParameterTypes();
        for (Type genericParamType : genericParamTypes) {
            methodInfo.getGenericParameterTypes().add(DescriptorColorizer.formatTypeName(genericParamType.toString()));
        }

        for (Parameter parameter : constructor.getParameters()) {
            methodInfo.getParameters().add(parameter.getName());
            methodInfo.getParameterTypes().add(DescriptorColorizer.formatTypeName(parameter.getType().getName()));
        }

        return methodInfo;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getReturnType() { return returnType; }
    public void setReturnType(String returnType) { this.returnType = returnType; }
    public String getGenericReturnType() { return genericReturnType; }
    public void setGenericReturnType(String genericReturnType) { this.genericReturnType = genericReturnType; }
    public List<String> getParameters() { return parameters; }
    public void setParameters(List<String> parameters) { this.parameters = parameters; }
    public List<String> getParameterTypes() { return parameterTypes; }
    public void setParameterTypes(List<String> parameterTypes) { this.parameterTypes = parameterTypes; }
    public List<String> getGenericParameterTypes() { return genericParameterTypes; }
    public void setGenericParameterTypes(List<String> genericParameterTypes) { this.genericParameterTypes = genericParameterTypes; }
    public int getModifiers() { return modifiers; }
    public void setModifiers(int modifiers) { this.modifiers = modifiers; }
    public String getDeclaringClass() { return declaringClass; }
    public void setDeclaringClass(String declaringClass) { this.declaringClass = declaringClass; }
    public boolean isDeclaringClassIsInterface() { return declaringClassIsInterface; }
    public void setDeclaringClassIsInterface(boolean declaringClassIsInterface) { this.declaringClassIsInterface = declaringClassIsInterface; }

    public String getModifiersString() {
        String result = Modifier.toString(modifiers);
        if (declaringClassIsInterface) {
            if (result.contains("abstract")) {
                result = result.replace("abstract", "").trim().replaceAll(" +", " ");
            }
            if (result.contains("public")) {
                result = result.replace("public", "").trim().replaceAll(" +", " ");
            }
        }
        return result;
    }

    public String getSignature() {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append("(");
        for (int i = 0; i < parameterTypes.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(parameterTypes.get(i));
        }
        sb.append(")");
        return sb.toString();
    }

    public JSONObject toJson() throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("name", name);
        obj.put("returnType", returnType);
        obj.put("genericReturnType", genericReturnType);
        obj.put("modifiers", getModifiersString());
        obj.put("signature", getSignature());
        obj.put("declaringClass", declaringClass);
        obj.put("declaringClassIsInterface", declaringClassIsInterface);

        JSONArray parametersArray = new JSONArray();
        for (String param : parameters) {
            parametersArray.put(param);
        }
        obj.put("parameters", parametersArray);

        JSONArray parameterTypesArray = new JSONArray();
        for (String paramType : parameterTypes) {
            parameterTypesArray.put(paramType);
        }
        obj.put("parameterTypes", parameterTypesArray);

        JSONArray genericParameterTypesArray = new JSONArray();
        for (String genericParamType : genericParameterTypes) {
            genericParameterTypesArray.put(genericParamType);
        }
        obj.put("genericParameterTypes", genericParameterTypesArray);

        return obj;
    }

    public static MethodInfo fromJson(JSONObject json) throws JSONException {
        MethodInfo methodInfo = new MethodInfo();
        methodInfo.setName(json.getString("name"));
        methodInfo.setReturnType(json.getString("returnType"));
        methodInfo.setGenericReturnType(json.optString("genericReturnType", methodInfo.getReturnType()));
        methodInfo.setModifiers(parseModifiers(json.optString("modifiers", "")));
        methodInfo.setDeclaringClass(json.optString("declaringClass", ""));
        methodInfo.setDeclaringClassIsInterface(json.optBoolean("declaringClassIsInterface", false));

        JSONArray parametersArray = json.optJSONArray("parameters");
        if (parametersArray != null) {
            for (int i = 0; i < parametersArray.length(); i++) {
                methodInfo.getParameters().add(parametersArray.getString(i));
            }
        }

        JSONArray parameterTypesArray = json.optJSONArray("parameterTypes");
        if (parameterTypesArray != null) {
            for (int i = 0; i < parameterTypesArray.length(); i++) {
                methodInfo.getParameterTypes().add(parameterTypesArray.getString(i));
            }
        }

        JSONArray genericParameterTypesArray = json.optJSONArray("genericParameterTypes");
        if (genericParameterTypesArray != null) {
            for (int i = 0; i < genericParameterTypesArray.length(); i++) {
                methodInfo.getGenericParameterTypes().add(genericParameterTypesArray.getString(i));
            }
        }

        return methodInfo;
    }

    private static int parseModifiers(String modifiersStr) {
        int modifiers = 0;
        if (modifiersStr == null || modifiersStr.isEmpty()) return modifiers;

        String[] parts = modifiersStr.split("\\s+");
        for (String part : parts) {
            switch (part) {
                case "public": modifiers |= Modifier.PUBLIC; break;
                case "private": modifiers |= Modifier.PRIVATE; break;
                case "protected": modifiers |= Modifier.PROTECTED; break;
                case "static": modifiers |= Modifier.STATIC; break;
                case "final": modifiers |= Modifier.FINAL; break;
                case "synchronized": modifiers |= Modifier.SYNCHRONIZED; break;
                case "volatile": modifiers |= Modifier.VOLATILE; break;
                case "transient": modifiers |= Modifier.TRANSIENT; break;
                case "native": modifiers |= Modifier.NATIVE; break;
                case "interface": modifiers |= Modifier.INTERFACE; break;
                case "abstract": modifiers |= Modifier.ABSTRACT; break;
                case "strictfp": modifiers |= Modifier.STRICT; break;
            }
        }
        return modifiers;
    }
}