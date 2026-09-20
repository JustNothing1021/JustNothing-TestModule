package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.BytecodeTexts;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeListClassesRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "source",
        aliases = {"-s", "--source"},
        required = false,
        description = BytecodeTexts.PARAM_BYTECODE_LIST_CLASSES_SOURCE_DESC
    )
    private String source;

    @CmdParam(
        name = "limit",
        aliases = {"-l", "--limit"},
        required = false,
        defaultValue = "200",
        description = BytecodeTexts.PARAM_BYTECODE_LIST_CLASSES_LIMIT_DESC
    )
    private int limit = 200;

    public BytecodeListClassesRequest() {
        super();
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
