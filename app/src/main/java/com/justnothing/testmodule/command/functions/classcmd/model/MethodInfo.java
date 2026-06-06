package com.justnothing.testmodule.command.functions.classcmd.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.GsonFactory;

import com.justnothing.testmodule.utils.reflect.DescriptorColorizer;

import org.json.JSONObject;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class MethodInfo {

    @Expose @SerializedName("name")
    private String name;

    @Expose @SerializedName("returnType")
    private String returnType;

    @Expose @SerializedName("genericReturnType")
    private String genericReturnType;

    @Expose @SerializedName("parameters")
    private List<String> parameters;

    @Expose @SerializedName("parameterTypes")
    private List<String> parameterTypes;

    @Expose @SerializedName("genericParameterTypes")
    private List<String> genericParameterTypes;

    @Expose @SerializedName("modifiers")
    private int modifiers;

    @Expose @SerializedName("declaringClass")
    private String declaringClass;

    @Expose @SerializedName("declaringClassIsInterface")
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

    public JSONObject toJson() {
        try {
            String jsonStr = GsonFactory.getInstance().toJson(this);
            return new JSONObject(jsonStr);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize MethodInfo", e);
        }
    }

    public static MethodInfo fromJson(JSONObject json) {
        try {
            String jsonStr = json.toString();
            return GsonFactory.getInstance().fromJson(jsonStr, MethodInfo.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize MethodInfo", e);
        }
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
