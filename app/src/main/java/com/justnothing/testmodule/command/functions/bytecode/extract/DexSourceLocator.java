package com.justnothing.testmodule.command.functions.bytecode.extract;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.os.Build;

import com.justnothing.testmodule.utils.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * "这个类住在哪个文件里" —— 给一个类名，找出装着它的来源（{@link DexSource}）。
 *
 * <p>这是整条提取链路里<b>唯一不会失效</b>的一环：它只读 dex 的类名表，不依赖任何外部二进制、
 * 不管 dex 有没有被 quicken、也不管 vdex 是哪个版本。所以即使后面提取失败，
 * "类在哪个文件里"这个答案永远给得出来。</p>
 *
 * <h3>候选来源从哪来（按可信度排序）</h3>
 * <ol>
 *   <li><b>应用自身的 APK</b>（含 split）—— 里面的 dex 是原件，最优先。
 *       通过 {@code ActivityThread#currentActivityThread().getApplication().getApplicationInfo()}
 *       拿；在 system_server 里拿不到（返回 null），属正常情况。</li>
 *   <li><b>boot classpath 里的 vdex</b> —— 读 {@code /proc/self/environ} 的 {@code BOOTCLASSPATH}
 *       环境变量拿到 jar 列表，再按 {@code <jar 所在目录>/oat/<arch>/<名字>.vdex} 映射。
 *       <b>不再走 {@code pathList}/{@code dexElements} 反射</b>：实测拿不到东西。</li>
 *   <li><b>本进程映射过的代码文件</b>（{@code /proc/self/maps}）—— 兜底，能覆盖
 *       magisk 模块 dex、动态加载的 dex 等不走前两条路的东西。</li>
 * </ol>
 *
 * <p>为什么要扫这么多候选而不是直接猜一个：ART 不保留原始 {@code .class}，
 * framework jar 在新设备上还是空壳（只剩 {@code META-INF/}），所以"类在哪儿"这件事
 * 必须靠实际扫描确认，不能靠文件名推断。</p>
 */
public final class DexSourceLocator {

    private static final Logger logger = Logger.getLoggerForName("DexSourceLocator");

    /** 候选上限：异常环境下（maps 内容很杂）防止扫上千个文件。 */
    private static final int MAX_CANDIDATES = 64;

    private static final String ENV_BOOTCLASSPATH = "BOOTCLASSPATH=";

    /** 拿不到 BOOTCLASSPATH 时的回退扫描位置（按优先级）。 */
    private static final String[] FRAMEWORK_OAT_DIRS = {
            "/apex/com.android.art/javalib/oat",
            "/apex/com.android.conscrypt/javalib/oat",
            "/apex/com.android.i18n/javalib/oat",
            "/system/framework/oat",
            "/system_ext/framework/oat",
            "/vendor/framework/oat",
    };

    private DexSourceLocator() {
    }

    /**
     * 扫描进度回调。
     *
     * <p>定位一个系统类要挨个检查 boot classpath 里十几个 vdex，几百毫秒里什么都看不到 ——
     * 这个回调让命令层能挂个进度条，而不是让用户对着黑屏猜是不是卡死了。</p>
     */
    public interface ScanListener {

        /** 扫描开始，{@code total} 是候选来源数量。 */
        void onStart(int total);

        /** 每检查完一个候选来源回调一次。 */
        void onScanned(int done, String label);
    }

    /**
     * 找出所有装着这个类的来源；找不到返回空列表。
     *
     * <p>会扫描多个候选文件，调用方应放在后台线程。</p>
     */
    public static List<DexSource> locate(String className) {
        return locate(className, null);
    }

    /** 带进度回调的 {@link #locate(String)}。 */
    public static List<DexSource> locate(String className, ScanListener listener) {
        List<DexSource> hits = new ArrayList<>();
        String normalized = normalize(className);
        if (normalized == null) {
            return hits;
        }

        Map<String, Candidate> candidates = collectCandidates();
        logger.debug("为 " + normalized + " 收集到 " + candidates.size() + " 个候选来源");

        if (listener != null) {
            listener.onStart(candidates.size());
        }

        int done = 0;
        for (Candidate candidate : candidates.values()) {
            try {
                if (DexClassIndex.containsClass(candidate.file, normalized)) {
                    hits.add(DexSource.of(candidate.kind, candidate.file));
                    logger.info("类 " + normalized + " 在 " + candidate.path);
                }
            } catch (IOException e) {
                logger.warn("检查 " + candidate.path + " 失败: " + e.getMessage());
            }
            done++;
            if (listener != null) {
                listener.onScanned(done, candidate.path);
            }
        }

        if (hits.isEmpty()) {
            logger.warn("在 " + candidates.size() + " 个候选来源里都没找到类 " + normalized);
        }
        return hits;
    }

    /**
     * 本进程所有"可能装着 dex"的来源，按可信度排序。
     *
     * <p>给 {@code list_classes} / {@code batch_export} 这类"不针对某个类"的命令用。</p>
     */
    public static List<DexSource> candidates() {
        List<DexSource> out = new ArrayList<>();
        for (Candidate candidate : collectCandidates().values()) {
            out.add(DexSource.of(candidate.kind, candidate.file));
        }
        return out;
    }

    // ---------- 候选收集 ----------

    private static Map<String, Candidate> collectCandidates() {
        Map<String, Candidate> out = new LinkedHashMap<>();
        addApplicationCandidates(out);
        addBootClasspathCandidates(out);
        addMappedFileCandidates(out);
        return out;
    }

    /** 当前应用的 APK（含 split）。system_server 里拿不到，直接跳过。 */
    @SuppressLint({"PrivateApi", "DiscouragedPrivateApi"})
    private static void addApplicationCandidates(Map<String, Candidate> out) {
        try {
            Class<?> activityThreadClass = Class.forName("android.app.ActivityThread");
            Object activityThread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            if (activityThread == null) {
                return;
            }
            Object application = activityThreadClass.getMethod("getApplication").invoke(activityThread);
            if (application == null) {
                return;
            }
            Object info = application.getClass().getMethod("getApplicationInfo").invoke(application);
            if (!(info instanceof ApplicationInfo appInfo)) {
                return;
            }

            addCandidate(out, appInfo.sourceDir, DexSource.Kind.APK);
            if (appInfo.splitSourceDirs != null) {
                for (String split : appInfo.splitSourceDirs) {
                    addCandidate(out, split, DexSource.Kind.APK);
                }
            }
        } catch (Throwable t) {
            // system_server 里 getApplication() 返回 null，或进程没有 ActivityThread，都属正常
            logger.debug("拿不到当前应用的 APK 路径: " + t);
        }
    }

    private static void addBootClasspathCandidates(Map<String, Candidate> out) {
        String bootClasspath = readBootClasspath();
        if (bootClasspath == null || bootClasspath.isEmpty()) {
            logger.debug("环境变量里没有 BOOTCLASSPATH，改为直接扫 oat 目录");
            addFrameworkOatCandidates(out);
            return;
        }

        for (String entry : bootClasspath.split(":")) {
            if (entry.isEmpty()) {
                continue;
            }
            File jar = new File(entry);
            // vdex 优先：现代设备上 framework jar 是空壳，dex 只存在于 vdex 里
            addVdexCandidatesFor(out, jar);
            addCandidate(out, entry, kindOfPath(entry));
        }
    }

    /**
     * 把一个 jar 映射到它旁边 {@code oat/<arch>} 下的 vdex / odex。
     *
     * <p>例：{@code /system/framework/services.jar}
     * → {@code /system/framework/oat/arm64/services.vdex}。</p>
     */
    private static void addVdexCandidatesFor(Map<String, Candidate> out, File jar) {
        File dir = jar.getParentFile();
        if (dir == null) {
            return;
        }
        String base = jar.getName();
        if (base.endsWith(".jar")) {
            base = base.substring(0, base.length() - 4);
        }

        for (File archDir : archDirs(new File(dir, "oat"))) {
            addCandidate(out, new File(archDir, base + ".vdex").getAbsolutePath(), DexSource.Kind.VDEX);
            addCandidate(out, new File(archDir, base + ".odex").getAbsolutePath(), DexSource.Kind.ODEX);
        }
    }

    /** 拿不到 BOOTCLASSPATH 时的兜底：直接列 framework 里所有模块的 vdex/odex。 */
    private static void addFrameworkOatCandidates(Map<String, Candidate> out) {
        for (String dir : FRAMEWORK_OAT_DIRS) {
            for (File archDir : archDirs(new File(dir))) {
                File[] files = archDir.listFiles(
                        (d, name) -> name.endsWith(".vdex") || name.endsWith(".odex"));
                if (files == null) {
                    continue;
                }
                // 同一个模块同时有 .vdex 和 .odex 时只要 vdex（odex 是它的外层容器）
                Map<String, File> preferred = new LinkedHashMap<>();
                Arrays.sort(files, Comparator.comparing(File::getName));
                for (File file : files) {
                    String name = file.getName();
                    String base = name.substring(0, name.lastIndexOf('.'));
                    File existing = preferred.get(base);
                    if (existing == null || (name.endsWith(".vdex") && !existing.getName().endsWith(".vdex"))) {
                        preferred.put(base, file);
                    }
                }
                for (File file : preferred.values()) {
                    addCandidate(out, file.getAbsolutePath(),
                            file.getName().endsWith(".vdex") ? DexSource.Kind.VDEX : DexSource.Kind.ODEX);
                }
            }
        }
    }

    /** 本进程 mmap 过的代码文件 —— 覆盖前两条路拿不到的（magisk 模块 dex、动态加载 dex 等）。 */
    private static void addMappedFileCandidates(Map<String, Candidate> out) {
        byte[] maps = readProcFile("/proc/self/maps");
        if (maps == null) {
            return;
        }
        for (String line : new String(maps, StandardCharsets.UTF_8).split("\n")) {
            int slash = line.indexOf('/');
            if (slash < 0) {
                continue;
            }
            String path = line.substring(slash).trim();
            int deleted = path.indexOf(" (deleted)");
            if (deleted > 0) {
                path = path.substring(0, deleted);
            }
            String lower = path.toLowerCase(Locale.ROOT);
            if (lower.endsWith(".apk")) {
                addCandidate(out, path, DexSource.Kind.APK);
            } else if (lower.endsWith(".jar")) {
                addCandidate(out, path, DexSource.Kind.JAR);
            } else if (lower.endsWith(".vdex")) {
                addCandidate(out, path, DexSource.Kind.VDEX);
            } else if (lower.endsWith(".odex")) {
                addCandidate(out, path, DexSource.Kind.ODEX);
            } else if (lower.endsWith(".dex")) {
                addCandidate(out, path, DexSource.Kind.DEX);
            }
        }
    }

    /**
     * 定位 {@code oat/} 下的架构目录。
     *
     * <p>先试 {@code Build.SUPPORTED_ABIS[0]}，再补上 oat 下存在的其它目录 ——
     * 因为目录名不一定是 ABI 名（历史上有过 {@code arm} / {@code arm64} 之外的写法），
     * 而且 32 位进程跑在 64 位系统上时，进程实际用的是 {@code arm} 而不是 {@code arm64}。</p>
     */
    private static List<File> archDirs(File oatDir) {
        List<File> dirs = new ArrayList<>();
        File preferred = null;
        if (Build.SUPPORTED_ABIS.length > 0) {
            preferred = new File(oatDir, Build.SUPPORTED_ABIS[0]);
            if (preferred.isDirectory()) {
                dirs.add(preferred);
            }
        }
        File[] children = oatDir.listFiles(File::isDirectory);
        if (children != null) {
            for (File child : children) {
                if (!child.equals(preferred)) {
                    dirs.add(child);
                }
            }
        }
        return dirs;
    }

    private static void addCandidate(Map<String, Candidate> out, String path, DexSource.Kind kind) {
        if (path == null || path.isEmpty() || out.size() >= MAX_CANDIDATES) {
            return;
        }
        File file = new File(path);
        if (!file.isFile()) {
            return;
        }
        String key;
        try {
            key = file.getCanonicalPath();
        } catch (IOException e) {
            key = file.getAbsolutePath();
        }
        out.putIfAbsent(key, new Candidate(key, file, kind));
    }

    // ---------- 杂项 ----------

    /** 接受 {@code com.foo.Bar} / {@code Lcom/foo/Bar;} / {@code com/foo/Bar} 三种写法。 */
    private static String normalize(String className) {
        if (className == null) {
            return null;
        }
        String name = className.trim();
        if (name.isEmpty()) {
            return null;
        }
        if (name.charAt(0) == 'L' && name.endsWith(";")) {
            name = name.substring(1, name.length() - 1);
        }
        name = name.replace('/', '.');
        return name.isEmpty() ? null : name;
    }

    /** 按文件名后缀判断来源类型；认不出来时按裸 dex 处理。 */
    public static DexSource.Kind kindOfPath(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".jar")) {
            return DexSource.Kind.JAR;
        }
        if (lower.endsWith(".apk")) {
            return DexSource.Kind.APK;
        }
        if (lower.endsWith(".vdex")) {
            return DexSource.Kind.VDEX;
        }
        if (lower.endsWith(".odex")) {
            return DexSource.Kind.ODEX;
        }
        return DexSource.Kind.DEX;
    }

    /**
     * 读 {@code BOOTCLASSPATH}。
     *
     * <p>读 {@code /proc/self/environ} 而不是 {@code System.getenv()}：环境变量是 zygote
     * fork 时设好的，{@code System.getenv} 走的是同一个来源但偶尔拿不全；
     * 直接读 procfs 更直白。</p>
     */
    private static String readBootClasspath() {
        byte[] env = readProcFile("/proc/self/environ");
        if (env == null) {
            return null;
        }
        for (String entry : new String(env, StandardCharsets.UTF_8).split("\u0000")) {
            if (entry.startsWith(ENV_BOOTCLASSPATH)) {
                return entry.substring(ENV_BOOTCLASSPATH.length());
            }
        }
        return null;
    }

    /** procfs 文件的 {@code length()} 通常报 0，所以按流读而不是按大小读。 */
    private static byte[] readProcFile(String path) {
        try (InputStream in = new FileInputStream(path)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4096);
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
                if (out.size() > (1 << 20)) {
                    break;
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            logger.debug("读取 " + path + " 失败: " + e.getMessage());
            return null;
        }
    }

    private record Candidate(String path, File file, DexSource.Kind kind) {
    }
}
