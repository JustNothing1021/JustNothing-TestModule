package com.justnothing.testmodule.utils.reflect;

import com.justnothing.engine.api.IClassFinder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 脚本引擎的类查找桥接（{@link IClassFinder} 实现）。
 *
 * <p>引擎默认的 {@code DefaultClassFinder} 背后是带正/负缓存的
 * {@code com.justnothing.engine.api.ClassResolver}。本模块把它替换成了本类，
 * 而本类背后的 {@link ClassResolver} <b>完全没有缓存</b>：同一个名字反复查找
 * 会反复在 6 个左右 ClassLoader 上各做一次 {@code Class.forName}，每次失败还会写一条
 * DEBUG 日志。脚本解析器会把每个标识符（以及它叠加默认 import 前缀后的候选全名、
 * 嵌套类变体）都当作候选类名去探测，因此几百行脚本会放大成上万次“必然失败”的查找。
 *
 * <p>本类补上与引擎默认实现等价的缓存语义：
 * <ul>
 *   <li>正缓存（找到的类）与负缓存（确认不存在的名字）都缓存，避免重复扫描；</li>
 *   <li>缓存有界（访问序 LRU，上限 {@link #MAX_CACHE_ENTRIES}），不会在内存紧张的手表上无限增长；</li>
 *   <li>线程安全：查表/写入加锁，真正的查找在锁外进行，不长时间持锁；</li>
 *   <li>实现 {@link IClassFinder#clearBlacklist()} / {@link IClassFinder#clearCache()}，
 *       让引擎在新增 import（{@code ParseContext.addImport}）后能使旧结论失效；</li>
 *   <li>APK ClassLoader 实例变化时整体清空，保证后来才出现的类加载器仍能被发现。</li>
 * </ul>
 *
 * <p><b>键的设计</b>：键 = 变体前缀 + import 列表指纹 + 类名。
 * 不带 ClassLoader 实例，这与引擎默认实现一致 —— {@link ClassResolver} 的查找本身就会
 * 遍历所有已注册的 ClassLoader，同一名字在不同 preferredLoader 下“找不到”的结论是一致的，
 * 把它放进键里反而会让每次新建 ClassLoader 的调用（如每次 {@code script run} 都会新建
 * {@code CompositeClassLoader}）无法复用缓存。带 import 指纹是必要的：
 * 简单名（如 {@code Foo}）能否找到取决于 import 列表，指纹不同即视为不同的查询。
 * 注意这里的直接查找（{@code findClass}）与带 import 查找使用不同的变体前缀，
 * 与引擎自身把两类查找分开缓存的理由相同（避免互相污染）。
 */
public class AppClassFinder implements IClassFinder {

    /** 缓存条目上限。手表内存紧张，必须保持有界。 */
    private static final int MAX_CACHE_ENTRIES = 8192;

    /** 键 -> Class（正缓存）或 {@link #NOT_FOUND}（负缓存）。访问序 LRU，超限自动淘汰最旧条目。 */
    private static final Map<String, Object> CACHE = new LinkedHashMap<String, Object>(256, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Object> eldest) {
            return size() > MAX_CACHE_ENTRIES;
        }
    };

    /** 哨兵对象：已查找但确认不存在。 */
    private static final Object NOT_FOUND = new Object();

    /** 键分隔符，避免类名/import 拼接产生歧义。 */
    private static final char SEP = '\u0000';

    /** 上次见到的 APK ClassLoader 实例；变化时说明类加载器集合变了，旧结论必须作废。 */
    private static volatile ClassLoader cachedApkLoader;

    /** 一次真正的类查找。 */
    private interface Lookup {
        Class<?> find();
    }

    /** 一次真正的类查找（找不到时抛异常的重载）。 */
    private interface FailLookup {
        Class<?> find() throws ClassNotFoundException;
    }

    private static Object cacheGet(String key) {
        synchronized (CACHE) {
            return CACHE.get(key);
        }
    }

    private static void cachePut(String key, Object value) {
        synchronized (CACHE) {
            CACHE.put(key, value);
        }
    }

    /** 命中则直接返回；未命中则执行查找并写入正/负缓存。 */
    private static Class<?> cached(String key, Lookup lookup) {
        Object hit = cacheGet(key);
        if (hit != null) {
            return hit == NOT_FOUND ? null : (Class<?>) hit;
        }
        Class<?> clazz = lookup.find();
        cachePut(key, clazz != null ? clazz : NOT_FOUND);
        return clazz;
    }

    /**
     * APK ClassLoader 是唯一会在运行期被注册进来的类加载器（见
     * {@link ClassLoaderManager#getApkClassLoader()} 中的注册调用）。它从 null 变成非 null 时，
     * 之前写入的负缓存可能不再成立，因此这里检测到实例变化就清空缓存。
     * 未变化时只是一次静态字段读取，开销可忽略。
     */
    private static void dropCacheIfLoaderChanged() {
        ClassLoader current = ClassLoaderManager.getApkClassLoader();
        if (current == cachedApkLoader) return;
        synchronized (CACHE) {
            if (current != cachedApkLoader) {
                CACHE.clear();
                cachedApkLoader = current;
            }
        }
    }

    private static String importsFingerprint(List<String> imports) {
        if (imports == null || imports.isEmpty()) return "";
        StringBuilder sb = new StringBuilder(imports.size() * 24);
        for (String importStmt : imports) {
            sb.append(importStmt).append(SEP);
        }
        return sb.toString();
    }

    @Override
    public Class<?> findClass(String className) {
        dropCacheIfLoaderChanged();
        return cached("D" + SEP + className, () -> ClassResolver.findClass(className));
    }

    @Override
    public Class<?> findClass(String className, ClassLoader classLoader) {
        dropCacheIfLoaderChanged();
        return cached("D" + SEP + className, () -> ClassResolver.findClass(className, classLoader));
    }

    @Override
    public Class<?> findClassWithImports(String className, ClassLoader classLoader, List<String> imports) {
        dropCacheIfLoaderChanged();
        String key = "I" + SEP + importsFingerprint(imports) + className;
        return cached(key, () -> ClassResolver.findClassWithImports(className, classLoader, imports));
    }

    @Override
    public Class<?> findClassOrFail(String className) throws ClassNotFoundException {
        dropCacheIfLoaderChanged();
        return findClassOrFailCached("F" + SEP + className, className,
                () -> ClassResolver.findClassOrFail(className));
    }

    @Override
    public Class<?> findClassOrFail(String className, ClassLoader classLoader) throws ClassNotFoundException {
        dropCacheIfLoaderChanged();
        return findClassOrFailCached("F" + SEP + className, className,
                () -> ClassResolver.findClassOrFail(className, classLoader));
    }

    private static Class<?> findClassOrFailCached(String key, String className, FailLookup lookup)
            throws ClassNotFoundException {
        Object hit = cacheGet(key);
        if (hit != null) {
            if (hit == NOT_FOUND) throw new ClassNotFoundException("未找到类: " + className);
            return (Class<?>) hit;
        }
        try {
            Class<?> clazz = lookup.find();
            cachePut(key, clazz);
            return clazz;
        } catch (ClassNotFoundException e) {
            cachePut(key, NOT_FOUND);
            throw e;
        }
    }

    /** 移除负缓存（找不到的类），保留正缓存。 */
    @Override
    public void clearBlacklist() {
        synchronized (CACHE) {
            CACHE.entrySet().removeIf(entry -> entry.getValue() == NOT_FOUND);
        }
    }

    /** 清空全部缓存（包括已找到的类）。 */
    @Override
    public void clearCache() {
        synchronized (CACHE) {
            CACHE.clear();
        }
    }
}
