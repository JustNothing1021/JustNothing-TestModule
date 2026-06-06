package com.justnothing.testmodule.command.agent;

import android.content.Context;
import android.content.SharedPreferences;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.constants.AgentResultTypes;

import org.json.JSONObject;

public class SpWriteHandler extends AgentCommandHandler {

    @Override
    public String getCommandType() {
        return "sp_write";
    }

    @Override
    public CommandResult handle(JSONObject params, Context context) throws Exception {
        String spName = params.getString("spName");
        String key = params.getString("key");
        int valueType = params.optInt("valueType", 0);

        SharedPreferences.Editor editor = context.getSharedPreferences(
                spName, Context.MODE_PRIVATE).edit();

        switch (valueType) {
            case 1:
                editor.putInt(key, params.getInt("value"));
                break;
            case 2:
                editor.putLong(key, params.getLong("value"));
                break;
            case 3:
                editor.putFloat(key, (float) params.getDouble("value"));
                break;
            case 4:
                editor.putBoolean(key, params.getBoolean("value"));
                break;
            default:
                editor.putString(key, params.getString("value"));
                break;
        }

        boolean committed = editor.commit();

        SpWriteResult result = new SpWriteResult(committed);
        result.setResultType(AgentResultTypes.SP_WRITE);
        result.setSpName(spName);
        result.setKey(key);

        return result;
    }
}
