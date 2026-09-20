package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.BytecodeTexts;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeBatchExportRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "source",
        aliases = {"-s", "--source"},
        required = false,
        description = BytecodeTexts.PARAM_BYTECODE_BATCH_EXPORT_SOURCE_DESC
    )
    private String source;

    @CmdParam(
        name = "outputPath",
        aliases = {"-o", "--output"},
        required = false,
        description = BytecodeTexts.PARAM_BYTECODE_BATCH_EXPORT_OUTPUTPATH_DESC
    )
    private String outputPath;

    @CmdParam(
        name = "limit",
        aliases = {"-l", "--limit"},
        required = false,
        defaultValue = "32",
        description = BytecodeTexts.PARAM_BYTECODE_BATCH_EXPORT_LIMIT_DESC
    )
    private int limit = 32;

    public BytecodeBatchExportRequest() {
        super();
    }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getOutputPath() { return outputPath; }
    public void setOutputPath(String outputPath) { this.outputPath = outputPath; }

    public int getLimit() { return limit; }
    public void setLimit(int limit) { this.limit = limit; }
}
