package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.framework.base.command.CmdParam;

@SerializeKeyName("bsh:script:delete")
public class BshScriptDeleteRequest extends CommandRequest {

    @CmdParam(
        name = "name",
        position = 1,
        required = true,
        description = "脚本名称"
    )
    private String name;

    public BshScriptDeleteRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
