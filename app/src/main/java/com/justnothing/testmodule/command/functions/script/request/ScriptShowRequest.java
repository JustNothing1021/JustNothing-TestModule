package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.functions.script.ScriptResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class ScriptShowRequest extends ScriptBaseRequest<ScriptResult> {

    @CmdParam(name = "name", position = 1, required = true, description = "脚本名称")
    private String name;

    public ScriptShowRequest() {
        super();
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
