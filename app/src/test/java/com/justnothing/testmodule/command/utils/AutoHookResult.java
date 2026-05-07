package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.AutoSerializableBase;
import com.justnothing.testmodule.command.base.protocol.ResultField;

import java.util.List;
import java.util.Map;

@AutoSerializable
public class AutoHookResult extends AutoSerializableBase {

    @ResultField(name = "success", description = "是否成功")
    private boolean success;

    @ResultField(name = "message", description = "消息")
    private String message;

    @ResultField(name = "hookId", description = "Hook ID")
    private String hookId;

    @ResultField(name = "hookedClasses", description = "已 Hook 的类列表")
    private List<String> hookedClasses;

    @ResultField(name = "statistics", description = "统计信息")
    private Map<String, Object> statistics;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getHookId() { return hookId; }
    public List<String> getHookedClasses() { return hookedClasses; }
    public Map<String, Object> getStatistics() { return statistics; }

    public void setSuccess(boolean success) { this.success = success; }
    public void setMessage(String message) { this.message = message; }
    public void setHookId(String hookId) { this.hookId = hookId; }
    public void setHookedClasses(List<String> hookedClasses) { this.hookedClasses = hookedClasses; }
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof AutoHookResult)) return false;
        AutoHookResult other = (AutoHookResult) obj;
        return success == other.success
            && (hookId == null ? other.hookId == null : hookId.equals(other.hookId))
            && (message == null ? other.message == null : message.equals(other.message))
            && (hookedClasses == null ? other.hookedClasses == null : hookedClasses.equals(other.hookedClasses));
    }
}
