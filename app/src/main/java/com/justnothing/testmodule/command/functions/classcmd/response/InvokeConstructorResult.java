package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

@SerializeKeyName("InvokeConstructor")
@AutoSerializable
public class InvokeConstructorResult extends ClassCommandResult {

    @ResultField(name = "resultString")
    private String resultString;

    @ResultField(name = "resultTypeName")
    private String resultTypeName;

    @ResultField(name = "resultHash", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int resultHash;

    public InvokeConstructorResult() {
        super();
    }

    public InvokeConstructorResult(String requestId) {
        super(requestId);
    }

    public String getResultString() { return resultString; }
    public void setResultString(String resultString) { this.resultString = resultString; }
    public String getResultTypeName() { return resultTypeName; }
    public void setResultTypeName(String resultTypeName) { this.resultTypeName = resultTypeName; }
    public int getResultHash() { return resultHash; }
    public void setResultHash(int resultHash) { this.resultHash = resultHash; }
}
