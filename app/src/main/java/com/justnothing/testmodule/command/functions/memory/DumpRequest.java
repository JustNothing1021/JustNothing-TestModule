package com.justnothing.testmodule.command.functions.memory;

import com.justnothing.testmodule.command.base.parser.FlagParam;
import com.justnothing.testmodule.command.base.parser.PositionalParam;
import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.SerializeKeyName;

import org.json.JSONException;
import org.json.JSONObject;

@SerializeKeyName("Dump")
@AutoSerializable
public class DumpRequest extends CommandRequest {

    @FlagParam(names = {"--heap"}, description = "只导出堆信息")
    private boolean heapOnly = false;

    @FlagParam(names = {"--threads"}, description = "只导出线程信息")
    private boolean threadsOnly = false;

    @FlagParam(names = {"--full"}, description = "导出完整信息 (默认)")
    private boolean fullDump = true;

    @PositionalParam(name = "filePath", order = 0, required = false, description = "输出文件路径")
    private String filePath;

    public DumpRequest() {
        super();
    }

    public boolean isHeapOnly() {
        return heapOnly;
    }

    public void setHeapOnly(boolean heapOnly) {
        this.heapOnly = heapOnly;
    }

    public boolean isThreadsOnly() {
        return threadsOnly;
    }

    public void setThreadsOnly(boolean threadsOnly) {
        this.threadsOnly = threadsOnly;
    }

    public boolean isFullDump() {
        return fullDump;
    }

    public void setFullDump(boolean fullDump) {
        this.fullDump = fullDump;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("heapOnly", heapOnly);
        obj.put("threadsOnly", threadsOnly);
        obj.put("fullDump", fullDump);
        if (filePath != null) {
            obj.put("filePath", filePath);
        }
        return obj;
    }

    @Override
    public DumpRequest fromJson(JSONObject obj) throws JSONException {
        setRequestId(obj.optString("requestId"));
        setHeapOnly(obj.optBoolean("heapOnly", false));
        setThreadsOnly(obj.optBoolean("threadsOnly", false));
        setFullDump(obj.optBoolean("fullDump", true));
        setFilePath(obj.optString("filePath", null));
        return this;
    }
}
