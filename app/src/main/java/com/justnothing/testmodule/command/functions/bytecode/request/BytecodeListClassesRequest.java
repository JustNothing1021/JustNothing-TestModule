package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

@SerializeKeyName("bytecode:list_classes")
public class BytecodeListClassesRequest extends CommandRequest {

    public BytecodeListClassesRequest() {
        super();
    }
}
