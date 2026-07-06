package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;

@SerializeKeyName("bsh:vars")
public class BshVarsRequest extends CommandRequest {

    public BshVarsRequest() {
        super();
    }
}
