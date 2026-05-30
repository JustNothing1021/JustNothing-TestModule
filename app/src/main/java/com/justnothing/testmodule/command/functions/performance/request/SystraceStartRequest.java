package com.justnothing.testmodule.command.functions.performance.request;

import com.justnothing.testmodule.command.base.command.CmdParam;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;
import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("perf:systrace:start")
public class SystraceStartRequest extends PerformanceRequest {

    @CmdParam(
        name = "duration",
        position = 1,
        required = false,
        description = "持续时间(ms)"
    )
    private Integer duration;

    @CmdParam(
        name = "categories",
        position = 2,
        required = false,
        varArgs = true,
        description = "跟踪类别"
    )
    private String categories;

    public SystraceStartRequest() {
        super();
    }

    public Integer getDuration() { return duration; }
    public void setDuration(Integer duration) { this.duration = duration; }

    public String getCategories() { return categories; }
    public void setCategories(String categories) { this.categories = categories; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (duration != null) obj.put("duration", duration);
        if (categories != null) obj.put("categories", categories);
        return obj;
    }

    @Override
    public SystraceStartRequest fromJson(JSONObject obj) throws org.json.JSONException {
        setRequestId(obj.optString("requestId"));
        if (obj.has("duration")) setDuration(obj.getInt("duration"));
        setCategories(obj.optString("categories", null));
        return this;
    }
}
