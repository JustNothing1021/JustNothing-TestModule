package com.justnothing.testmodule.command.functions.watch.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WatchClearResult extends WatchCommandResult {

    @Expose @SerializedName("clearedCount")
    private Integer clearedCount;

    public WatchClearResult() {
        super();
    }

    public Integer getClearedCount() { return clearedCount; }
    public void setClearedCount(Integer clearedCount) { this.clearedCount = clearedCount; }
}
