package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;

@SerializeKeyName("bsh:clear")
public class BshClearRequest extends CommandRequest {

    public BshClearRequest() {
        super();
    }
}
