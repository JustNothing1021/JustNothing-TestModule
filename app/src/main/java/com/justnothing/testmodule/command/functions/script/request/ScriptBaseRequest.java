package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.annotation.SerializeKeyName;

@SerializeKeyName("script:base")
public abstract class ScriptBaseRequest extends CommandRequest {
}
