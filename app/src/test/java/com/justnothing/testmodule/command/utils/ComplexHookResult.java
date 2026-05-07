package com.justnothing.testmodule.command.utils;

import com.justnothing.testmodule.command.base.protocol.AutoSerializable;
import com.justnothing.testmodule.command.base.protocol.CommandResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@AutoSerializable
public class ComplexHookResult extends CommandResult {

    private String hookId;
    private List<String> hookedClasses;
    private Map<String, Object> statistics;
    private ComplexHookRequest originalRequest;

    public String getHookId() { return hookId; }
    public List<String> getHookedClasses() { return hookedClasses; }
    public Map<String, Object> getStatistics() { return statistics; }
    public ComplexHookRequest getOriginalRequest() { return originalRequest; }

    public void setHookId(String hookId) { this.hookId = hookId; }
    public void setHookedClasses(List<String> hookedClasses) { this.hookedClasses = hookedClasses; }
    public void setStatistics(Map<String, Object> statistics) { this.statistics = statistics; }
    public void setOriginalRequest(ComplexHookRequest request) { this.originalRequest = request; }

    @Override
    public String getResultType() {
        return "complex-hook";
    }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject json = super.toJson();
        if (hookId != null) json.put("hookId", hookId);
        
        if (hookedClasses != null && !hookedClasses.isEmpty()) {
            org.json.JSONArray arr = new org.json.JSONArray();
            for (String cls : hookedClasses) {
                arr.put(cls);
            }
            json.put("hookedClasses", arr);
        }
        
        if (statistics != null && !statistics.isEmpty()) {
            JSONObject statsObj = new JSONObject(statistics);
            json.put("statistics", statsObj);
        }
        
        if (originalRequest != null) {
            json.put("originalRequest", originalRequest.toJson());
        }
        
        return json;
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComplexHookResult)) return false;
        ComplexHookResult other = (ComplexHookResult) obj;
        return isSuccess() == other.isSuccess()
            && (hookId == null ? other.hookId == null : hookId.equals(other.hookId))
            && getMessage() == null ? other.getMessage() == null : getMessage().equals(other.getMessage())
            && (hookedClasses == null ? other.hookedClasses == null : hookedClasses.equals(other.hookedClasses));
    }
}
