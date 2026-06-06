package com.justnothing.testmodule.command.agent;

import android.content.Context;
import android.content.SharedPreferences;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.constants.AgentResultTypes;

import org.json.JSONObject;

import java.util.Map;
import java.util.Set;

public class SpReadHandler extends AgentCommandHandler {

    @Override
    public String getCommandType() {
        return "sp_read";
    }

    @Override
    public CommandResult handle(JSONObject params, Context context) throws Exception {
        SpReadResult result = new SpReadResult();
        result.setResultType(AgentResultTypes.SP_READ);

        String spName = params.getString("spName");
        String keyFilter = params.has("keyFilter") ? params.optString("keyFilter", null) : null;

        result.setSpName(spName);

        SharedPreferences sp = context.getSharedPreferences(spName, Context.MODE_PRIVATE);
        Map<String, ?> all = sp.getAll();

        if (keyFilter != null && !keyFilter.isEmpty()) {
            Object value = all.get(keyFilter);
            if (value != null) {
                result.putEntry(keyFilter, convertValue(value));
            } else {
                result.putEntry("_error", "key '" + keyFilter + "' not found");
            }
        } else {
            for (Map.Entry<String, ?> entry : all.entrySet()) {
                result.putEntry(entry.getKey(), convertValue(entry.getValue()));
            }
        }

        result.setSuccess(true);
        return result;
    }

    private Object convertValue(Object value) {
        if (value == null) return null;
        if (value instanceof String) return value;
        if (value instanceof Integer) return ((Integer) value).intValue();
        if (value instanceof Long) return ((Long) value).longValue();
        if (value instanceof Float) return ((Float) value).doubleValue();
        if (value instanceof Boolean) return value;
        if (value instanceof Set) {
            @SuppressWarnings("unchecked")
            Set<String> set = (Set<String>) value;
            return new java.util.ArrayList<>(set);
        }
        return String.valueOf(value);
    }
}
