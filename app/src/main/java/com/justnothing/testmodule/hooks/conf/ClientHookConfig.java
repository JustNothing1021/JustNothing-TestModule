package com.justnothing.testmodule.hooks.conf;

import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

public final class ClientHookConfig {

    private static final String TAG = "ClientHookConfig";

    private static final ReentrantLock lock = new ReentrantLock();

    /**
     * Hook 开关的当前状态，永远指向一份不可变快照。
     * <p>
     * 读的人（{@link #isHookEnabled}）在 hook 的调用路径上，不能加锁，所以这里不共享
     * 「一个会被就地修改的 Map」，而是共享「一份整体替换的快照」：写的人改完之后一次性
     * 换掉引用，读的人任何时候取到的都是一份自洽的数据，不会读到改了一半的中间状态。
     * </p>
     */
    private static final AtomicReference<Map<String, Boolean>> hookData =
            new AtomicReference<>(Collections.emptyMap());

    /** 上次刷新时间。读它的人不持锁，所以要 volatile，否则可能永远看不到别人写的新值。 */
    private static volatile long lastRefreshTime = 0;
    private static final long REFRESH_INTERVAL = 5000;
    private static final Logger logger = Logger.getLoggerForName(TAG);

    private static void refreshData() {
        if (System.currentTimeMillis() - lastRefreshTime < REFRESH_INTERVAL) {
            return;
        }

        // tryLock 而不是 lock：别人正在刷新时，这里立刻返回、继续用手上这份旧快照，
        // 不排队等一次 IO。原来的 isRefreshing 标志就是为了这个目的，现在锁本身
        // 就能表达「有没有人在刷」，那个标志也就没必要存在了。
        if (!lock.tryLock()) {
            return;
        }
        try {
            // 抢到锁不代表还需要刷新：等锁的这段时间里可能已经有人刷完了，再确认一次。
            // 这个时间戳要在锁里重新取，否则用的是抢锁之前那个过期值。
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastRefreshTime < REFRESH_INTERVAL) {
                return;
            }
            // 先记时间再读：读取失败时也算刷过一次，避免失败后每调用一次就重试一次。
            lastRefreshTime = currentTime;

            // 基于当前快照改出一份新的，最后一次性换上去（和原来的逐条 put 语义一致：
            // 配置里没提到的 key 保留原值）
            Map<String, Boolean> snapshot = new HashMap<>(hookData.get());
            JSONObject config = DataBridge.readClientHookConfig();
            if (config.length() > 0) {
                for (Iterator<String> it = config.keys(); it.hasNext(); ) {
                    String key = it.next();
                    snapshot.put(key, config.optBoolean(key, true));
                }
            }
            hookData.set(Collections.unmodifiableMap(snapshot));
        } catch (Exception e) {
            logger.error("读取Hook配置失败", e);
            DataBridge.createDefaultClientHookConfig();
        } finally {
            lock.unlock();
        }
    }

    public static boolean isHookEnabled(String hookName) {
        refreshData();
        // 取一次引用就够了，后续都在这一份快照上读，不会读到两次不同的数据
        Boolean enabled = hookData.get().get(hookName);
        return enabled == null || enabled;
    }

    public static void setHookEnabled(String name, boolean enabled) {
        lock.lock();
        try {
            Map<String, Boolean> states = new HashMap<>(hookData.get());
            logger.info("将" + name + "的状态设置为" + enabled);
            states.put(name, enabled);
            DataBridge.writeClientHookConfig(new JSONObject(states));
            hookData.set(Collections.unmodifiableMap(states));
            lastRefreshTime = System.currentTimeMillis();
            DataBridge.forceRefreshServerHookStatus();
        } finally {
            lock.unlock();
        }
    }

    public static void setHookStatus(Map<String, Boolean> status) {
        lock.lock();
        try {
            // 顺手拷一份：调用方传进来的 map 之后可能还会被改，不能让它影响这里的状态
            Map<String, Boolean> states = new HashMap<>(status);
            logger.info("将Hook状态更新为" + states);
            // 写入配置文件
            DataBridge.writeClientHookConfig(new JSONObject(states));
            hookData.set(Collections.unmodifiableMap(states));
            lastRefreshTime = System.currentTimeMillis();
            DataBridge.forceRefreshServerHookStatus();
        } finally {
            lock.unlock();
        }
    }

    public static void forceRefresh() {
        lock.lock();
        try {
            Map<String, Boolean> snapshot = new HashMap<>();
            JSONObject config = DataBridge.readClientHookConfig(true);
            if (config.length() > 0) {
                for (Iterator<String> it = config.keys(); it.hasNext(); ) {
                    String key = it.next();
                    snapshot.put(key, config.optBoolean(key, true));
                }
            }
            hookData.set(Collections.unmodifiableMap(snapshot));
            // 刷完才记时间：这样「刚强制刷过」的 5 秒内不会再刷一遍
            lastRefreshTime = System.currentTimeMillis();
            logger.debug("强制刷新Hook配置完成，配置项数量: " + snapshot.size());
        } finally {
            lock.unlock();
        }
    }

    public static Map<String, Boolean> getAllHookStates() {
        refreshData();
        // 给出去的是副本，调用方随便改都影响不到内部状态
        return new HashMap<>(hookData.get());
    }

}
