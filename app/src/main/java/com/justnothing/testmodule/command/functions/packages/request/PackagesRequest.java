package com.justnothing.testmodule.command.functions.packages.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("Packages")
public class PackagesRequest extends CommandRequest {

    public PackagesRequest() {
        super();
    }
}
