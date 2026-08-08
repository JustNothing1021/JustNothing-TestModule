package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

@SerializeKeyName("bsh:execute")
public class BshExecuteRequest extends CommandRequest {

    @CmdParam(
        name = "code",
        position = 1,
        required = false,
        description = "BeanShell代码",
        varArgs = true
    )
    private String code;

    public BshExecuteRequest() {
        super();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
