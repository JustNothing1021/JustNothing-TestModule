package com.justnothing.testmodule.command.functions.bsh.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("bsh:clear")
public class BshClearRequest extends CommandRequest {

    public BshClearRequest() {
        super();
    }
}
