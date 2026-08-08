package com.justnothing.testmodule.command.functions.alias.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("AliasList")
public class AliasListRequest extends CommandRequest {

    public AliasListRequest() {
        super();
    }
}
