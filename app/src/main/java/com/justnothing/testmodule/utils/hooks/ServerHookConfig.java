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
    
    private static JSONObject cachedServerHookConfig = null;
    private static long serverConfigCacheTime = 0;
    private static final long SERVER_CONFIG_CACHE_TTL = 5000;

    public static boolean isHookEnabled(String name) throws JSONException {
        long currentTime = System.currentTimeMillis();
        if (cachedServerHookConfig == null || (currentTime - serverConfigCacheTime) >= SERVER_CONFIG_CACHE_TTL) {
            cachedServerHookConfig = DataBridge.readServerHookConfig();
            serverConfigCacheTime = currentTime;
        }
        JSONObject item = cachedServerHookConfig.getJSONObject(name);
        return item.getBoolean(KEY_ENABLED);
    }

    public static JSONObject getHookConfig(String name) throws JSONException {
        return DataBridge.readClientHookConfig().getJSONObject(name);
    }
    
    public static void invalidateCache() {
        cachedServerHookConfig = null;
        serverConfigCacheTime = 0;
    }

}
