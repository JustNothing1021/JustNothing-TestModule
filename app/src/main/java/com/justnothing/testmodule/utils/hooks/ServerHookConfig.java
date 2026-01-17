package com.justnothing.testmodule.utils.hooks;

import static com.justnothing.testmodule.constants.HookConfig.KEY_ENABLED;

import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.functions.Logger;

import org.json.JSONException;
import org.json.JSONObject;

public class ServerHookConfig {

    public static final String TAG = "ServerHookConfig";

    public static class ServerHookConfigLogger extends Logger {

        @Override
        public String getTag() {
            return TAG;
        }
    }
    private static final ServerHookConfigLogger logger = new ServerHookConfigLogger();

    public static boolean isHookEnabled(String name) throws JSONException {
        JSONObject hookData = DataBridge.readServerHookConfig();
        JSONObject item = hookData.getJSONObject(name);
        return item.getBoolean(KEY_ENABLED);
    }

    public static JSONObject getHookConfig(String name) throws JSONException {
        return DataBridge.readClientHookConfig().getJSONObject(name);
    }

}
