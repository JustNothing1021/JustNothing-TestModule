package com.justnothing.testmodule.command.functions.watch.response;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class WatchAddResult extends WatchCommandResult {

    @Expose @SerializedName("taskId")
    private Integer taskId;
    @Expose @SerializedName("targetType")
    private String targetType;
    @Expose @SerializedName("className")
    private String className;
    @Expose @SerializedName("memberName")
    private String memberName;
    @Expose @SerializedName("signature")
    private String signature;
    @Expose @SerializedName("interval")
    private Long interval;

    public WatchAddResult() {
        super();
    }

    public Integer getTaskId() { return taskId; }
    public void setTaskId(Integer taskId) { this.taskId = taskId; }

    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }

    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public Long getInterval() { return interval; }
    public void setInterval(Long interval) { this.interval = interval; }
}
