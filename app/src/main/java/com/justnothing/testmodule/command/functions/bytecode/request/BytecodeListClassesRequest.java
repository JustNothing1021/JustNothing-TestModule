package com.justnothing.testmodule.command.functions.bytecode.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("bytecode:list_classes")
public class BytecodeListClassesRequest extends CommandRequest {

    public BytecodeListClassesRequest() {
        super();
    }
}
