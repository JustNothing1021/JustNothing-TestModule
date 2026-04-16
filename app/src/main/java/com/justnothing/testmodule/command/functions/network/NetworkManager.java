package com.justnothing.testmodule.command.functions.network;

import com.justnothing.testmodule.utils.logging.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import de.robv.android.xposed.XC_MethodHook;

public class NetworkManager {

    private static final String TAG = "NetworkManager";
    private static final Logger logger = Logger.getLoggerForName(TAG);
    private static final int MAX_RECORDS = 100;

    private static final NetworkManager instance = new NetworkManager();

    private volatile boolean interceptEnabled = false;
    private volatile boolean recordEnabled = true;
    private final ConcurrentHashMap<Integer, NetworkRequestInfo> requests = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, MockRule> mockRules = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, XC_MethodHook.Unhook> activeHooks = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final List<NetworkListener> listeners = Collections.synchronizedList(new ArrayList<>());

    private NetworkManager() {
    }

    public static NetworkManager getInstance() {
        return instance;
    }

    public boolean isInterceptEnabled() {
        return interceptEnabled;
    }

    public void setInterceptEnabled(boolean enabled) {
        this.interceptEnabled = enabled;
        logger.info("网络拦截: " + (enabled ? "已启用" : "已禁用"));
    }

    public boolean isRecordEnabled() {
        return recordEnabled;
    }

    public void setRecordEnabled(boolean enabled) {
        this.recordEnabled = enabled;
        logger.info("网络记录: " + (enabled ? "已启用" : "已禁用"));
    }

    public NetworkRequestInfo createRequest(String url, String method, String clientType) {
        int id = idGenerator.getAndIncrement();
        NetworkRequestInfo request = new NetworkRequestInfo(id, url, method, clientType);
        
        if (recordEnabled) {
            if (requests.size() >= MAX_RECORDS) {
                removeOldestRequest();
            }
            requests.put(id, request);
        }
        
        notifyRequestCreated(request);
        logger.debug("创建网络请求记录: #" + id + " " + method + " " + url);
        return request;
    }

    public NetworkRequestInfo getRequest(int id) {
        return requests.get(id);
    }

    public List<NetworkRequestInfo> getAllRequests() {
        return new ArrayList<>(requests.values());
    }

    public List<NetworkRequestInfo> getRequests(Predicate<NetworkRequestInfo> filter) {
        List<NetworkRequestInfo> result = new ArrayList<>();
        for (NetworkRequestInfo request : requests.values()) {
            if (filter.test(request)) {
                result.add(request);
            }
        }
        return result;
    }

    public List<NetworkRequestInfo> getRequestsByHost(String host) {
        return getRequests(r -> r.getHost().contains(host));
    }

    public List<NetworkRequestInfo> getRequestsByMethod(String method) {
        return getRequests(r -> r.getMethod().equalsIgnoreCase(method));
    }

    public List<NetworkRequestInfo> getRequestsByStatus(int minStatus, int maxStatus) {
        return getRequests(r -> r.getResponseCode() >= minStatus && r.getResponseCode() <= maxStatus);
    }

    public void completeRequest(int id) {
        NetworkRequestInfo request = requests.get(id);
        if (request != null) {
            request.setCompleted(true);
            notifyRequestCompleted(request);
            logger.debug("网络请求完成: #" + id);
        }
    }

    public void failRequest(int id, Throwable error) {
        NetworkRequestInfo request = requests.get(id);
        if (request != null) {
            request.setError(error);
            notifyRequestFailed(request, error);
            logger.error("网络请求失败: #" + id + " - " + error.getMessage());
        }
    }

    public void clearRequests() {
        int count = requests.size();
        requests.clear();
        logger.info("已清除 " + count + " 条网络请求记录");
    }

    public int getRequestCount() {
        return requests.size();
    }

    private void removeOldestRequest() {
        int oldestId = Integer.MAX_VALUE;
        for (Integer id : requests.keySet()) {
            if (id < oldestId) {
                oldestId = id;
            }
        }
        if (oldestId != Integer.MAX_VALUE) {
            requests.remove(oldestId);
        }
    }

    public void addMockRule(String pattern, String response, int statusCode) {
        MockRule rule = new MockRule(pattern, response, statusCode);
        mockRules.put(pattern, rule);
        logger.info("添加 Mock 规则: " + pattern + " -> " + statusCode);
    }

    public void removeMockRule(String pattern) {
        mockRules.remove(pattern);
        logger.info("移除 Mock 规则: " + pattern);
    }

    public void clearMockRules() {
        mockRules.clear();
        logger.info("已清除所有 Mock 规则");
    }

    public MockRule findMockRule(String url) {
        for (MockRule rule : mockRules.values()) {
            if (url.contains(rule.pattern) || url.matches(rule.pattern)) {
                return rule;
            }
        }
        return null;
    }

    public List<MockRule> getAllMockRules() {
        return new ArrayList<>(mockRules.values());
    }

    public int getMockRuleCount() {
        return mockRules.size();
    }

    public void addHook(String key, XC_MethodHook.Unhook unhook) {
        activeHooks.put(key, unhook);
        logger.debug("添加 Hook: " + key);
    }

    public void removeHook(String key) {
        XC_MethodHook.Unhook unhook = activeHooks.remove(key);
        if (unhook != null) {
            unhook.unhook();
            logger.debug("移除 Hook: " + key);
        }
    }

    public void clearHooks() {
        for (Map.Entry<String, XC_MethodHook.Unhook> entry : activeHooks.entrySet()) {
            try {
                entry.getValue().unhook();
            } catch (Exception e) {
                logger.error("移除 Hook 失败: " + entry.getKey(), e);
            }
        }
        activeHooks.clear();
        logger.info("已清除所有网络 Hook");
    }

    public int getHookCount() {
        return activeHooks.size();
    }

    public void addListener(NetworkListener listener) {
        listeners.add(listener);
    }

    public void removeListener(NetworkListener listener) {
        listeners.remove(listener);
    }

    private void notifyRequestCreated(NetworkRequestInfo request) {
        synchronized (listeners) {
            for (NetworkListener listener : listeners) {
                try {
                    listener.onRequestCreated(request);
                } catch (Exception e) {
                    logger.error("通知监听器失败", e);
                }
            }
        }
    }

    private void notifyRequestCompleted(NetworkRequestInfo request) {
        synchronized (listeners) {
            for (NetworkListener listener : listeners) {
                try {
                    listener.onRequestCompleted(request);
                } catch (Exception e) {
                    logger.error("通知监听器失败", e);
                }
            }
        }
    }

    private void notifyRequestFailed(NetworkRequestInfo request, Throwable error) {
        synchronized (listeners) {
            for (NetworkListener listener : listeners) {
                try {
                    listener.onRequestFailed(request, error);
                } catch (Exception e) {
                    logger.error("通知监听器失败", e);
                }
            }
        }
    }

    public void shutdown() {
        clearHooks();
        clearMockRules();
        clearRequests();
        listeners.clear();
        interceptEnabled = false;
        logger.info("NetworkManager 已关闭");
    }

    public static class MockRule {
        public final String pattern;
        public final String response;
        public final int statusCode;
        public final Map<String, String> headers;

        public MockRule(String pattern, String response, int statusCode) {
            this.pattern = pattern;
            this.response = response;
            this.statusCode = statusCode;
            this.headers = new ConcurrentHashMap<>();
        }

        public MockRule addHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }
    }

    public interface NetworkListener {
        void onRequestCreated(NetworkRequestInfo request);
        void onRequestCompleted(NetworkRequestInfo request);
        void onRequestFailed(NetworkRequestInfo request, Throwable error);
    }
}
