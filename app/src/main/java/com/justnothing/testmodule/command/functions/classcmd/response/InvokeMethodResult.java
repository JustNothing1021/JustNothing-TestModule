package com.justnothing.testmodule.command.functions.classcmd.response;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;
import com.justnothing.testmodule.command.functions.classcmd.ClassCommandResult;

@SerializeKeyName("InvokeMethod")
@AutoSerializable
public class InvokeMethodResult extends ClassCommandResult {

    @ResultField(name = "resultString")
    private String resultString;

    @ResultField(name = "resultTypeName")
    private String resultTypeName;

    @ResultField(name = "resultHash", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int resultHash;

    @ResultField(name = "instanceAfterInvocation")
    private String instanceAfterInvocation;

    @ResultField(name = "instanceHash", defaultValue = ValueSupplier.ZeroSupplier.class)
    private int instanceHash;

    public InvokeMethodResult() {
        super();
    }

    public InvokeMethodResult(String requestId) {
        super(requestId);
    }

    public String getResultString() { return resultString; }
    public void setResultString(String resultString) { this.resultString = resultString; }
    public String getResultTypeName() { return resultTypeName; }
    public void setResultTypeName(String resultTypeName) { this.resultTypeName = resultTypeName; }
    public int getResultHash() { return resultHash; }
    public void setResultHash(int resultHash) { this.resultHash = resultHash; }
    public String getInstanceAfterInvocation() { return instanceAfterInvocation; }
    public void setInstanceAfterInvocation(String instanceAfterInvocation) { this.instanceAfterInvocation = instanceAfterInvocation; }
    public int getInstanceHash() { return instanceHash; }
    public void setInstanceHash(int instanceHash) { this.instanceHash = instanceHash; }
}
