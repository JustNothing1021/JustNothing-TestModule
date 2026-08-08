package com.justnothing.testmodule.command.functions.alias.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

@SerializeKeyName("AliasRemove")
public class AliasRemoveRequest extends CommandRequest {

    @CmdParam(
        name = "name",
        description = "要删除的别名名称",
        required = true,
        position = 1
    )
    private String name;

    public AliasRemoveRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
