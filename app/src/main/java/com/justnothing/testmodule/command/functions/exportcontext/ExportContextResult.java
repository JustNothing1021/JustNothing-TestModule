package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.ResultField;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.protocol.ValueSupplier;

import java.util.ArrayList;
import java.util.List;

@SerializeKeyName("ExportContext")
@AutoSerializable
public class ExportContextResult extends CommandResult {

    @ResultField(name = "fields", defaultValue = ValueSupplier.EmptyListSupplier.class)
    private List<ContextFieldInfo> fields = new ArrayList<>();

    public ExportContextResult() {
        super();
    }

    public ExportContextResult(String requestId) {
        super(requestId);
    }

    public List<ContextFieldInfo> getFields() { return fields; }
    public void setFields(List<ContextFieldInfo> fields) { this.fields = fields; }
}
