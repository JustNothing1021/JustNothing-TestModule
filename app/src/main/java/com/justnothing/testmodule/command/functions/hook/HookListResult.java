package com.justnothing.testmodule.command.functions.hook;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import java.util.ArrayList;
import java.util.List;

public class HookListResult extends CommandResult {

    @Expose @SerializedName("subCommand")
    private String subCommand;
    @Expose @SerializedName("timestamp")
    private long timestamp;
    @Expose @SerializedName("totalHookCount")
    private int totalHookCount;
    @Expose @SerializedName("activeCount")
    private int activeCount;
    @Expose @SerializedName("hooks")
    private List<HookItem> hooks;

    public HookListResult() {
        super();
        this.hooks = new ArrayList<>();
    }

    public HookListResult(String requestId) {
        super(requestId);
        this.hooks = new ArrayList<>();
    }

    public String getSubCommand() { return subCommand; }
    public void setSubCommand(String subCommand) { this.subCommand = subCommand; }

    @Expose @SerializedName("message")
    private String message;
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getTotalHookCount() { return totalHookCount; }
    public void setTotalHookCount(int totalHookCount) { this.totalHookCount = totalHookCount; }

    public int getActiveCount() { return activeCount; }
    public void setActiveCount(int activeCount) { this.activeCount = activeCount; }

    public List<HookItem> getHooks() { return hooks; }
    public void setHooks(List<HookItem> hooks) { this.hooks = hooks; }
    public void addHook(HookItem item) { this.hooks.add(item); }

    public static class HookItem {
        @Expose @SerializedName("id")
        private String id;
        @Expose @SerializedName("className")
        private String className;
        @Expose @SerializedName("methodName")
        private String methodName;
        @Expose @SerializedName("signature")
        private String signature;
        @Expose @SerializedName("hasBefore")
        private boolean hasBefore;
        @Expose @SerializedName("hasAfter")
        private boolean hasAfter;
        @Expose @SerializedName("hasReplace")
        private boolean hasReplace;
        @Expose @SerializedName("callCount")
        private int callCount;
        @Expose @SerializedName("active")
        private boolean active;
        @Expose @SerializedName("enabled")
        private boolean enabled;
        @Expose @SerializedName("createTime")
        private long createTime;
        @Expose @SerializedName("beforeCodePreview")
        private String beforeCodePreview;
        @Expose @SerializedName("afterCodePreview")
        private String afterCodePreview;
        @Expose @SerializedName("replaceCodePreview")
        private String replaceCodePreview;

        public HookItem() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }

        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }

        public boolean isHasBefore() { return hasBefore; }
        public void setHasBefore(boolean hasBefore) { this.hasBefore = hasBefore; }

        public boolean isHasAfter() { return hasAfter; }
        public void setHasAfter(boolean hasAfter) { this.hasAfter = hasAfter; }

        public boolean isHasReplace() { return hasReplace; }
        public void setHasReplace(boolean hasReplace) { this.hasReplace = hasReplace; }

        public int getCallCount() { return callCount; }
        public void setCallCount(int callCount) { this.callCount = callCount; }

        public boolean isActive() { return active; }
        public void setActive(boolean active) { this.active = active; }

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public long getCreateTime() { return createTime; }
        public void setCreateTime(long createTime) { this.createTime = createTime; }

        public String getBeforeCodePreview() { return beforeCodePreview; }
        public void setBeforeCodePreview(String beforeCodePreview) { this.beforeCodePreview = beforeCodePreview; }

        public String getAfterCodePreview() { return afterCodePreview; }
        public void setAfterCodePreview(String afterCodePreview) { this.afterCodePreview = afterCodePreview; }

        public String getReplaceCodePreview() { return replaceCodePreview; }
        public void setReplaceCodePreview(String replaceCodePreview) { this.replaceCodePreview = replaceCodePreview; }
    }
}
