package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("bsh:vars")
public class BshVarsRequest extends CommandRequest {

    public BshVarsRequest() {
        super();
    }
}
