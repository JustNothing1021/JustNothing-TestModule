package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;
import com.justnothing.testmodule.command.functions.classcmd.model.FieldInfo;
import com.justnothing.testmodule.command.functions.classcmd.model.MethodInfo;

@SerializeKeyName("ReflectOperation")
public class ReflectOperationResult extends ClassCommandResult {

    private String className;

    private String operationType;

    private String memberName;

    private FieldInfo fieldInfo;

    private MethodInfo methodInfo;

    private Object value;

    private String valueType;

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
}
