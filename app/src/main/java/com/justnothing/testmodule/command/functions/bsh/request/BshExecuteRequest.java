package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;
import com.justnothing.testmodule.command.functions.bsh.BshTexts;

public class BshExecuteRequest extends CommandRequest<BeanShellResult> {

    @CmdParam(
        name = "code",
        position = 1,
        required = false,
        description = BshTexts.PARAM_BSH_RUN_CODE_CODE_DESC,
        varArgs = true
    )
    private String code;

    public BshExecuteRequest() {
        super();
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
