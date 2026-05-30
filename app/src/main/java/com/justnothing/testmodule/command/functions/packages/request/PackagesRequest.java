package com.justnothing.testmodule.command.functions.packages.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("Packages")
public class PackagesRequest extends CommandRequest {

    public PackagesRequest() {
        super();
    }
}
