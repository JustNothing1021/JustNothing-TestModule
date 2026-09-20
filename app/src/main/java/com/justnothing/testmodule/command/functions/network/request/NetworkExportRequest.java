package com.justnothing.testmodule.command.functions.network.request;

import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.network.NetworkTexts;
import com.justnothing.testmodule.command.framework.model.CommandResult;

public class NetworkExportRequest extends CommandRequest<CommandResult> {

    @CmdParam(
        name = "filePath",
        position = 1,
        required = false,
        defaultValue = "/sdcard/network_log.json",
        description = NetworkTexts.PARAM_NETWORK_EXPORT_FILEPATH_DESC,
        serializedName = "filePath"
    )
    private String filePath = "/sdcard/network_log.json";

    public NetworkExportRequest() {
        super();
    }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
