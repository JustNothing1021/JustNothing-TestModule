package com.justnothing.testmodule.command.functions.hook;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class HookListResult extends CommandResult {

    private long timestamp;
    private int totalHookCount;
    private int activeCount;
    private List<HookItem> hooks;

    public HookListResult() {
        super();
        this.hooks = new ArrayList<>();
    }

    public HookListResult(String requestId) {
        super(requestId);
        this.hooks = new ArrayList<>();
    }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getTotalHookCount() { return totalHookCount; }
    public void setTotalHookCount(int totalHookCount) { this.totalHookCount = totalHookCount; }

    public int getActiveCount() { return activeCount; }
    public void setActiveCount(int activeCount) { this.activeCount = activeCount; }

    public List<HookItem> getHooks() { return hooks; }
    public void setHooks(List<HookItem> hooks) { this.hooks = hooks; }
    public void addHook(HookItem item) { this.hooks.add(item); }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        obj.put("timestamp", timestamp);
        obj.put("totalHookCount", totalHookCount);
        obj.put("activeCount", activeCount);

        if (hooks != null && !hooks.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (HookItem item : hooks) {
                arr.put(item.toJson());
            }
            obj.put("hooks", arr);
        }

        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        timestamp = obj.optLong("timestamp", 0);
        totalHookCount = obj.optInt("totalHookCount", 0);
        activeCount = obj.optInt("activeCount", 0);

        hooks = new ArrayList<>();
        if (obj.has("hooks")) {
            JSONArray arr = obj.getJSONArray("hooks");
            for (int i = 0; i < arr.length(); i++) {
                HookItem item = new HookItem();
                item.fromJson(arr.getJSONObject(i));
                hooks.add(item);
            }
        }
    }

    public static class HookItem {
        private String id;
        private String className;
        private String methodName;
        private String signature;
        private boolean hasBefore;
        private boolean hasAfter;
        private boolean hasReplace;
        private int callCount;
        private boolean active;
        private boolean enabled;
        private long createTime;
        private String beforeCodePreview;
        private String afterCodePreview;
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

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("className", className);
            obj.put("methodName", methodName);
            if (signature != null && !signature.isEmpty()) obj.put("signature", signature);
            obj.put("hasBefore", hasBefore);
            obj.put("hasAfter", hasAfter);
            obj.put("hasReplace", hasReplace);
            obj.put("callCount", callCount);
            obj.put("active", active);
            obj.put("enabled", enabled);
            obj.put("createTime", createTime);
            if (beforeCodePreview != null) obj.put("beforeCodePreview", beforeCodePreview);
            if (afterCodePreview != null) obj.put("afterCodePreview", afterCodePreview);
            if (replaceCodePreview != null) obj.put("replaceCodePreview", replaceCodePreview);
            return obj;
        }

        public void fromJson(JSONObject obj) throws JSONException {
            id = obj.optString("id", "");
            className = obj.optString("className", "");
            methodName = obj.optString("methodName", "");
            signature = obj.optString("signature", null);
            hasBefore = obj.optBoolean("hasBefore", false);
            hasAfter = obj.optBoolean("hasAfter", false);
            hasReplace = obj.optBoolean("hasReplace", false);
            callCount = obj.optInt("callCount", 0);
            active = obj.optBoolean("active", false);
            enabled = obj.optBoolean("enabled", true);
            createTime = obj.optLong("createTime", 0);
            beforeCodePreview = obj.optString("beforeCodePreview", null);
            afterCodePreview = obj.optString("afterCodePreview", null);
            replaceCodePreview = obj.optString("replaceCodePreview", null);
        }
    }
}
