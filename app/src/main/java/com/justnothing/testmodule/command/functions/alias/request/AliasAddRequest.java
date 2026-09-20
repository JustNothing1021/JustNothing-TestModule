package com.justnothing.testmodule.command.functions.alias.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.alias.AliasTexts;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;

public class AliasAddRequest extends CommandRequest<AliasResult> {

    @CmdParam(
        name = "name",
        description = AliasTexts.PARAM_ALIAS_ADD_NAME_DESC,
        required = true,
        position = 1
    )
    private String name;

    @CmdParam(
        name = "command",
        description = AliasTexts.PARAM_ALIAS_ADD_COMMAND_DESC,
        varArgs = true,
        position = 2
    )
    private String command;

    public AliasAddRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
}
