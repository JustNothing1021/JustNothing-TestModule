package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("bsh:script:show")
public class BshScriptShowRequest extends CommandRequest {

    @CmdParam(
        name = "name",
        position = 1,
        required = true,
        description = "脚本名称"
    )
    private String name;

    public BshScriptShowRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
