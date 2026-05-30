package com.justnothing.testmodule.command.functions.alias.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

@SerializeKeyName("AliasAdd")
public class AliasAddRequest extends CommandRequest {

    @CmdParam(
        name = "name",
        description = "简短的替代名称（建议 2-6 个字符）",
        required = true,
        position = 1
    )
    private String name;

    @CmdParam(
        name = "command",
        description = "完整的原始命令（支持多词命令）",
        required = false,
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
