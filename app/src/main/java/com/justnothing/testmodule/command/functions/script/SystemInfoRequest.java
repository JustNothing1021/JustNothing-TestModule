package com.justnothing.testmodule.command.functions.script;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import com.justnothing.testmodule.command.base.command.CmdParam;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("SystemInfo")
public class SystemInfoRequest extends CommandRequest {

    @CmdParam(
        name = "--cpu",
        description = "显示CPU信息",
        required = false,
        aliases = {"-c"}
    )
    private boolean showCpu = false;

    @CmdParam(
        name = "--memory",
        description = "显示内存信息",
        required = false,
        aliases = {"-m"}
    )
    private boolean showMemory = false;

    @CmdParam(
        name = "--os",
        description = "显示操作系统信息",
        required = false,
        aliases = {"-o"}
    )
    private boolean showOs = false;

    @CmdParam(
        name = "--props",
        description = "显示系统属性",
        required = false,
        aliases = {"-p"}
    )
    private boolean showProps = false;

    @CmdParam(
        name = "--all",
        description = "显示所有信息（默认）",
        required = false,
        defaultValue = "true"
    )
    private boolean showAll = true;

    public SystemInfoRequest() {
        super();
    }

    public boolean isShowCpu() { return showCpu; }
    public void setShowCpu(boolean showCpu) { this.showCpu = showCpu; }

    public boolean isShowMemory() { return showMemory; }
    public void setShowMemory(boolean showMemory) { this.showMemory = showMemory; }

    public boolean isShowOs() { return showOs; }
    public void setShowOs(boolean showOs) { this.showOs = showOs; }

    public boolean isShowProps() { return showProps; }
    public void setShowProps(boolean showProps) { this.showProps = showProps; }

    public boolean isShowAll() { return showAll; }
    public void setShowAll(boolean showAll) { this.showAll = showAll; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("showCpu", showCpu);
        obj.put("showMemory", showMemory);
        obj.put("showOs", showOs);
        obj.put("showProps", showProps);
        obj.put("showAll", showAll);
        return obj;
    }

    @Override
    public SystemInfoRequest fromJson(JSONObject obj) {
        this.setRequestId(obj.optString("requestId"));
        this.showCpu = obj.optBoolean("showCpu", false);
        this.showMemory = obj.optBoolean("showMemory", false);
        this.showOs = obj.optBoolean("showOs", false);
        this.showProps = obj.optBoolean("showProps", false);
        this.showAll = obj.optBoolean("showAll", true);
        return this;
    }

    @Override
    public CommandRequest fromCommandLine(String[] args) {
        return this;
    }
}
