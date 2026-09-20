package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bytecode.response.BytecodeResult;

public class BytecodeBatchExportRequest extends CommandRequest<BytecodeResult> {

    @CmdParam(
        name = "source",
        aliases = {"-s", "--source"},
        required = false,
        description = "来源文件或目录（目录会递归找 apk/jar/dex/vdex/odex）；不填则导出当前应用的 APK"
    )
    private String source;

    @CmdParam(
        name = "outputPath",
        aliases = {"-o", "--output"},
        required = false,
        description = "输出目录"
    )
    private String outputPath;

    @CmdParam(
        name = "limit",
        aliases = {"-l", "--limit"},
        required = false,
        defaultValue = "32",
        description = "最多处理多少个来源文件"
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
