package com.justnothing.testmodule.command.functions.script.request;

import com.justnothing.testmodule.command.functions.script.ScriptTexts;
import com.justnothing.testmodule.command.functions.script.response.ScriptResult;
import com.justnothing.testmodule.command.framework.annotation.CmdParam;

public class ScriptPermPresetRequest extends ScriptBaseRequest<ScriptResult> {

    @CmdParam(name = "presetName", position = 1, description = ScriptTexts.PARAM_SCRIPT_PERMISSION_PRESET_PRESETNAME_DESC)
    private String presetName;

    public ScriptPermPresetRequest() {
        super();
    }

    public String getPresetName() { return presetName; }
    public void setPresetName(String presetName) { this.presetName = presetName; }
}
