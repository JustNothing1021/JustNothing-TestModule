package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("bsh:script:create")
public class BshScriptCreateRequest extends CommandRequest {

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
