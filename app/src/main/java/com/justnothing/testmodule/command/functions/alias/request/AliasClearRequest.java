package com.justnothing.testmodule.command.functions.alias.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;

@SerializeKeyName("AliasClear")
public class AliasClearRequest extends CommandRequest {

    public AliasClearRequest() {
        super();
    }
}
