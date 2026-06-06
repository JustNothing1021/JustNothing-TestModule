package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

@SerializeKeyName("GetFieldValue")
public class GetFieldValueResult extends ClassCommandResult {

    private String valueString;

    private String valueTypeName;

    private int valueHash;

    public GetFieldValueResult() {
        super();
    }

    public GetFieldValueResult(String requestId) {
        super(requestId);
    }

    public String getValueString() { return valueString; }
    public void setValueString(String valueString) { this.valueString = valueString; }
    public String getValueTypeName() { return valueTypeName; }
    public void setValueTypeName(String valueTypeName) { this.valueTypeName = valueTypeName; }
    public int getValueHash() { return valueHash; }
    public void setValueHash(int valueHash) { this.valueHash = valueHash; }
}
