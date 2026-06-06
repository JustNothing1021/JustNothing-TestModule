package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

@SerializeKeyName("SetFieldValue")
public class SetFieldValueResult extends ClassCommandResult {

    private String className;

    private String fieldName;

    private String value;

    public SetFieldValueResult() {
        super();
    }

    public SetFieldValueResult(String requestId) {
        super(requestId);
    }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getFieldName() { return fieldName; }
    public void setFieldName(String fieldName) { this.fieldName = fieldName; }
    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
