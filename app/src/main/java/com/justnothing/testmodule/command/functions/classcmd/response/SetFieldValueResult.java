package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

@SerializeKeyName("SetFieldValue")
@AutoSerializable
public class SetFieldValueResult extends ClassCommandResult {

    @ResultField(name = "className")
    private String className;

    @ResultField(name = "fieldName")
    private String fieldName;

    @ResultField(name = "value")
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
