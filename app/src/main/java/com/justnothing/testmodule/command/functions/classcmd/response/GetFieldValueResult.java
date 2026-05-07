package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

@SerializeKeyName("GetFieldValue")
@AutoSerializable
public class GetFieldValueResult extends ClassCommandResult {

    @ResultField(name = "valueString")
    private String valueString;

    @ResultField(name = "valueTypeName")
    private String valueTypeName;

    @ResultField(name = "valueHash", defaultValue = ValueSupplier.ZeroSupplier.class)
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
