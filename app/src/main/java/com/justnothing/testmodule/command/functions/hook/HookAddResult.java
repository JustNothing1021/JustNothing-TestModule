package com.justnothing.testmodule.command.functions.hook;

import com.justnothing.testmodule.command.base.CommandResult;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

public class HookAddResult extends CommandResult {

    private String hookId;
    private boolean successAction;
    private String message;
    private List<HookDetailInfo> detail;

    public HookAddResult() {
        super();
        this.detail = new ArrayList<>();
    }

    public HookAddResult(String requestId) {
        super(requestId);
        this.detail = new ArrayList<>();
    }

    public String getHookId() { return hookId; }
    public void setHookId(String hookId) { this.hookId = hookId; }

    public boolean isSuccessAction() { return successAction; }
    public void setSuccessAction(boolean successAction) { this.successAction = successAction; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public List<HookDetailInfo> getDetail() { return detail; }
    public void setDetail(List<HookDetailInfo> detail) { this.detail = detail; }

    @Override
    public JSONObject toJson() throws JSONException {
        JSONObject obj = super.toJson();
        if (hookId != null) obj.put("hookId", hookId);
        obj.put("successAction", successAction);
        if (message != null) obj.put("message", message);

        if (detail != null && !detail.isEmpty()) {
            JSONArray arr = new JSONArray();
            for (HookDetailInfo info : detail) {
                arr.put(info.toJson());
            }
            obj.put("detail", arr);
        }

        return obj;
    }

    @Override
    public void fromJson(JSONObject obj) throws JSONException {
        super.fromJson(obj);
        hookId = obj.optString("hookId", null);
        successAction = obj.optBoolean("successAction", false);
        message = obj.optString("message", null);

        detail = new ArrayList<>();
        if (obj.has("detail")) {
            JSONArray arr = obj.getJSONArray("detail");
            for (int i = 0; i < arr.length(); i++) {
                HookDetailInfo info = new HookDetailInfo();
                info.fromJson(arr.getJSONObject(i));
                detail.add(info);
            }
        }
    }

    public static class HookDetailInfo {
        private String key;
        private String value;

        public HookDetailInfo() {}

        public HookDetailInfo(String key, String value) {
            this.key = key;
            this.value = value;
        }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public JSONObject toJson() throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("key", key);
            obj.put("value", value);
            return obj;
        }

        public void fromJson(JSONObject obj) throws JSONException {
            key = obj.optString("key", "");
            value = obj.optString("value", "");
        }
    }
}
