package com.justnothing.testmodule.utils.hooks;

import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.functions.Logger;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

public class ClientHookConfig {

    private static final String TAG = "ClientHookConfig";

    private static final ReentrantLock lock = new ReentrantLock();
    private static final Map<String, Boolean> hookData = new HashMap<>();
    private static boolean isRefreshing = false;

    private static final class HookConfigLogger extends Logger {
        @Override
        public String getTag() {
            return TAG;
        }
    }
    private static final HookConfigLogger logger = new HookConfigLogger();

    private static void refreshData() {
        if (isRefreshing) {
            return;
        }
        
        lock.lock();
        try {
            if (isRefreshing) {
                return;
            }
            
            isRefreshing = true;
            JSONObject config = DataBridge.readClientHookConfig();
            if (config.length() > 0) {
                for (Iterator<String> it = config.keys(); it.hasNext(); ) {
                    String key = it.next();
                    hookData.put(key, config.optBoolean(key, true));
                }
            }
        } catch (Exception e) {
            logger.error("读取Hook配置失败", e);
            DataBridge.createDefaultClientHookConfig();
        } finally {
            isRefreshing = false;
            lock.unlock();
        }
    }

    public static boolean isHookEnabled(String hookName) {
        refreshData();
        Boolean enabled = hookData.get(hookName);
        return enabled == null || enabled;
    }

    public static void setHookEnabled(String name, boolean enabled) {
        refreshData();
        Map<String, Boolean> states = getAllHookStates();
        logger.info("将" + name + "的状态设置为" + enabled);
        states.put(name, enabled);
        DataBridge.writeClientHookConfig(new JSONObject(states));
    }

    public static Map<String, Boolean> getAllHookStates() {
        refreshData();
        return new HashMap<>(hookData);
    }

}
