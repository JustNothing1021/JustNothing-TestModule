package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.functions.script.ScriptResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class ScriptPermPresetRequest extends ScriptBaseRequest<ScriptResult> {

    @CmdParam(name = "presetName", position = 1, description = "预设名称(sandbox/expression/minimal/full)")
    private String presetName;

    public ScriptPermPresetRequest() {
        super();
    }

    public String getPresetName() { return presetName; }
    public void setPresetName(String presetName) { this.presetName = presetName; }
}
