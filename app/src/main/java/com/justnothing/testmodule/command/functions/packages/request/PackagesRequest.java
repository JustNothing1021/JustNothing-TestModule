package com.justnothing.testmodule.command.functions.packages.request;

import com.justnothing.testmodule.command.framework.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.framework.base.protocol.SerializeKeyName;

@SerializeKeyName("Packages")
public class PackagesRequest extends CommandRequest {

    public PackagesRequest() {
        super();
    }
}
