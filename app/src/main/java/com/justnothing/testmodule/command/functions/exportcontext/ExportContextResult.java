package com.justnothing.testmodule.command.functions.exportcontext;

import com.justnothing.testmodule.command.framework.model.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class ExportContextResult extends CommandResult {

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
