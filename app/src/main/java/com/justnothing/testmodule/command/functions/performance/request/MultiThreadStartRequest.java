package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("perf:multithread:start")
public class MultiThreadStartRequest extends PerformanceRequest {

    @CmdParam(
        name = "rate",
        position = 1,
        required = false,
        defaultValue = "100",
        description = "采样频率（Hz）"
    )
    private int rate = 100;

    @CmdParam(
        name = "--exclude",
        aliases = {"-e"},
        required = false,
        description = "排除的类/方法模式"
    )
    private String exclude;

    public MultiThreadStartRequest() {
        super();
    }

    public int getRate() { return rate; }
    public void setRate(int rate) { this.rate = rate; }

    public String getExclude() { return exclude; }
    public void setExclude(String exclude) { this.exclude = exclude; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("rate", rate);
        if (exclude != null) obj.put("exclude", exclude);
        return obj;
    }

    @Override
    public MultiThreadStartRequest fromJson(JSONObject obj) {
        setRequestId(obj.optString("requestId"));
        setRate(obj.optInt("rate", 100));
        setExclude(obj.optString("exclude", null));
        return this;
    }
}
