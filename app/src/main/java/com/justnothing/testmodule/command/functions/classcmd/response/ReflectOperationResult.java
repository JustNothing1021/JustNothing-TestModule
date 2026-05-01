package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

import org.json.JSONObject;
import org.json.JSONException;

public class ReflectOperationResult extends ClassCommandResult {

    private String className;
    private String operationType;  // "field", "method", "constructor", "static"
    private String memberName;
    private FieldInfo fieldInfo;
    private MethodInfo methodInfo;
    private Object value;
    private String valueType;
    private boolean success;

    public ReflectOperationResult() {
        super();
    }

    public ReflectOperationResult(String requestId) {
        super(requestId);
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getOperationType() { return operationType; }
    public void setOperationType(String operationType) { this.operationType = operationType; }
    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public FieldInfo getFieldInfo() { return fieldInfo; }
    public void setFieldInfo(FieldInfo fieldInfo) { this.fieldInfo = fieldInfo; }
    public MethodInfo getMethodInfo() { return methodInfo; }
    public void setMethodInfo(MethodInfo methodInfo) { this.methodInfo = methodInfo; }
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    public String getValueType() { return valueType; }
    public void setValueType(String valueType) { this.valueType = valueType; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("className", className);
        obj.put("operationType", operationType);
        obj.put("memberName", memberName);
        obj.put("success", success);

        if (fieldInfo != null) {
            obj.put("fieldInfo", fieldInfo.toJson());
        }

        if (methodInfo != null) {
            obj.put("methodInfo", methodInfo.toJson());
        }

        if (value != null) {
            obj.put("value", value.toString());
            obj.put("valueType", valueType != null ? valueType : value.getClass().getName());
        } else if ("get".equals(operationType) || "invoke".equals(operationType)) {
            obj.put("value", JSONObject.NULL);
        }

        return obj;
    }
}
