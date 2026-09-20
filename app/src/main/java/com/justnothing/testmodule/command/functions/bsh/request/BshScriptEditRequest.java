package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bsh.BshTexts;

public class BshScriptEditRequest extends CommandRequest<BeanShellResult> {

    @CmdParam(
        name = "name",
        position = 1,
        required = true,
        description = BshTexts.PARAM_BSH_SCRIPT_EDIT_NAME_DESC
    )
    private String name;

    public BshScriptEditRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
