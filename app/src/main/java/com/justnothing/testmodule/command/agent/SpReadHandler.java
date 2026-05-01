package com.justnothing.testmodule.command.agent;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

public class SpReadHandler extends AgentCommandHandler<JSONObject> {

    @Override
    public String getCommandType() {
        return "sp_read";
    }

    @Override
    public JSONObject handle(JSONObject params, Context context) throws Exception {
        String spName = params.getString("spName");
        String keyFilter = params.has("keyFilter") ? params.optString("keyFilter", null) : null;

        SharedPreferences sp = context.getSharedPreferences(spName, Context.MODE_PRIVATE);
        Map<String, ?> all = sp.getAll();

        JSONObject result = new JSONObject();
        if (keyFilter != null && !keyFilter.isEmpty()) {
            Object value = all.get(keyFilter);
            if (value != null) {
                putValue(result, keyFilter, value);
            } else {
                result.put("_error", "key '" + keyFilter + "' not found");
            }
        } else {
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                putValue(result, entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    private void putValue(JSONObject obj, String key, Object value) throws org.json.JSONException {
        if (value == null) {
            obj.put(key, JSONObject.NULL);
        } else if (value instanceof String) {
            obj.put(key, value);
        } else if (value instanceof Integer) {
            obj.put(key, ((Integer) value).intValue());
        } else if (value instanceof Long) {
            obj.put(key, ((Long) value).longValue());
        } else if (value instanceof Float) {
            obj.put(key, ((Float) value).doubleValue());
        } else if (value instanceof Boolean) {
            obj.put(key, value);
        } else if (value instanceof Set) {
            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) value;
            obj.put(key, new org.json.JSONArray(set));
        } else {
            obj.put(key, String.valueOf(value));
        }
    }
}
