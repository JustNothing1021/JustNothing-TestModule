package com.justnothing.testmodule.command.functions.alias.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("AliasClear")
public class AliasClearRequest extends CommandRequest {

    public AliasClearRequest() {
        super();
    }
}
