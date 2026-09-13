package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class BshScriptCreateRequest extends CommandRequest<BeanShellResult> {

    @CmdParam(
        name = "name",
        position = 1,
        required = true,
        description = "脚本名称"
    )
    private String name;

    public BshScriptCreateRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
