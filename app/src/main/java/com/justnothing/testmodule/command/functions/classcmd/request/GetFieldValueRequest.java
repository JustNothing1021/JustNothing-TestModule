package com.justnothing.testmodule.command.functions.classcmd.request;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandRequest;

import org.json.JSONException;
import org.json.JSONObject;

public class GetFieldValueRequest extends ClassCommandRequest {

    private String className;
    private String fieldName;
    private String targetInstance;
    private boolean isStatic;
    private String operation;  // "list", "get", "set", "info"
    private String valueToSet;
    private boolean showValue;
    private boolean showType;
    private boolean showModifiers;
    private boolean showAll = true;
    private boolean accessSuper;
    private boolean accessInterfaces;

    public GetFieldValueRequest() {
        super();
        this.operation = "info";
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getTargetInstance() { return targetInstance; }
    public void setTargetInstance(String targetInstance) { this.targetInstance = targetInstance; }
    public boolean isStatic() { return isStatic; }
    public void setStatic(boolean aStatic) { isStatic = aStatic; }
    public String getOperation() { return operation; }
    public void setOperation(String operation) { this.operation = operation; }
    public String getValueToSet() { return valueToSet; }
    public void setValueToSet(String valueToSet) { this.valueToSet = valueToSet; }
    public boolean isShowValue() { return showValue; }
    public void setShowValue(boolean showValue) { this.showValue = showValue; }
    public boolean isShowType() { return showType; }
    public void setShowType(boolean showType) { this.showType = showType; }
    public boolean isShowModifiers() { return showModifiers; }
    public void setShowModifiers(boolean showModifiers) { this.showModifiers = showModifiers; }
    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }
    public boolean isAccessSuper() { return accessSuper; }
    public void setAccessSuper(boolean accessSuper) { this.accessSuper = accessSuper; }
    public boolean isAccessInterfaces() { return accessInterfaces; }
    public void setAccessInterfaces(boolean accessInterfaces) { this.accessInterfaces = accessInterfaces; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("fieldName", fieldName);
        obj.put("targetInstance", targetInstance);
        obj.put("isStatic", isStatic);
        obj.put("operation", operation);
        if (valueToSet != null) obj.put("valueToSet", valueToSet);
        obj.put("showValue", showValue);
        obj.put("showType", showType);
        obj.put("showModifiers", showModifiers);
        obj.put("showAll", showAll);
        obj.put("accessSuper", accessSuper);
        obj.put("accessInterfaces", accessInterfaces);
        return obj;
    }

    @Override
    public GetFieldValueRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setClassName(obj.optString("className"));
        setFieldName(obj.optString("fieldName"));
        setTargetInstance(obj.optString("targetInstance", null));
        setStatic(obj.optBoolean("isStatic", false));
        setOperation(obj.optString("operation", "info"));
        setValueToSet(obj.optString("valueToSet", null));
        setShowValue(obj.optBoolean("showValue", false));
        setShowType(obj.optBoolean("showType", false));
        setShowModifiers(obj.optBoolean("showModifiers", false));
        setShowAll(obj.optBoolean("showAll", true));
        setAccessSuper(obj.optBoolean("accessSuper", false));
        setAccessInterfaces(obj.optBoolean("accessInterfaces", false));
        return this;
    }

    @Override
    public GetFieldValueRequest fromCommandLine(String[] args) throws IllegalCommandLineArgumentException {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException(
                "参数不足: class field <class_name> [field_name] [options]");
        }

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "-g", "--get" -> {
                    operation = "get";
                    showAll = false;
                }
                case "-s", "--set" -> {
                    operation = "set";
                    showAll = false;
                    if (i + 3 < args.length - 1) {
                        className = args[i + 1];
                        fieldName = args[i + 2];
                        valueToSet = args[i + 3];
                        i += 3;
                    } else {
                        throw new IllegalCommandLineArgumentException(
                            "提供给-s的参数不足: class field -s <class> <field> <value>");
                    }
                }
                case "-v", "--value" -> {
                    showValue = true;
                    showAll = false;
                }
                case "-t", "--type" -> {
                    showType = true;
                    showAll = false;
                }
                case "-m", "--modifiers" -> {
                    showModifiers = true;
                    showAll = false;
                }
                case "-a", "--all" -> showAll = true;
                case "--super" -> accessSuper = true;
                case "--interfaces" -> accessInterfaces = true;
            }
        }

        if (className == null) {
            className = args[args.length - 1];
        }

        if (fieldName == null && args.length >= 2 && !args[args.length - 2].startsWith("-")) {
            fieldName = args[args.length - 2];
        }

        return this;
    }
}
