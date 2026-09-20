package com.justnothing.testmodule.hooks;

import com.justnothing.testmodule.utils.logging.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Hook 组件包加载器。
 * <p>
 * 组件包是构建期可选塞进 {@code app/hookpacks/} 的 jar。每个 jar 里放一份清单
 * {@value #MANIFEST_RESOURCE}，一行一个 Hook 类的全限定名（{@code #} 开头算注释）。
 * 多个组件包的清单会被构建期拼接成一份，所以放几个都行，不用改代码。
 * </p>
 * <p>
 * Hook 的身份标识就是它的全限定类名（见 {@link XposedBasicHook#getHookName()}），
 * 天然唯一，不同组件包之间不会撞。
 * </p>
 * <p>
 * 之所以在 {@link HookEntry} 的静态块里做这件事：那是模块最早的执行点（早于
 * {@code initZygote} 与任何 {@code handleLoadPackage}），组件包里的 Hook 才能和
 * 内置 Hook 一样在系统起来时就位。
 * </p>
 * <p>
 * 没有组件包时这里只打一条日志就跳过，所以公开构建照样能跑。
 * </p>
 *
 * <h3>怎么写一个组件包</h3>
 * <ol>
 *   <li>写一个继承 {@link PackageHook}（或 {@link ZygoteHook}）的类，实现
 *       {@code hookImplements()} 与 {@code getTag()}，在 {@code hookImplements()} 里用
 *       {@code setHookDisplayName} / {@code setHookCondition} / {@code hookMethod} 这些
 *       辅助方法描述要做什么。类名要取得和别人不会撞 —— 它会成为配置与状态里的唯一标识。</li>
 *   <li>在 jar 根目录放一份 {@code hookpack.list}，一行一个第 1 步的类名。</li>
 *   <li>先产出 API 包：{@code gradlew :app:hookApiJar}（生成
 *       {@code app/build/hookApi/hook-api.jar}，里面是 Hook API + xposed api）。
 *       编译时 classpath 给这个 jar 加上 SDK 的 {@code android.jar}：
 *       <pre>
 * javac -encoding UTF-8 -source 17 -target 17 \
 *       -cp "app/build/hookApi/hook-api.jar;$ANDROID_HOME/platforms/android-36/android.jar" \
 *       -d classes YourHook.java</pre></li>
 *   <li>{@code jar cf my-hookpack.jar -C classes .}，把 jar 丢进 {@code app/hookpacks/}
 *       再正常构建 APK 就行。里面的 Hook 会和内置 Hook 一起在系统启动时装上。</li>
 * </ol>
 * <p>
 * 注意 {@code hook-api.jar} 里的 {@code include} 白名单在 {@code app/build.gradle} 的
 * {@code hookApiJar} 任务里，想给组件包开放更多包就往那儿加一行。
 * </p>
 */
public final class HookPackLoader {

    private static final Logger logger = Logger.getLoggerForName("HookPackLoader");

    /** 组件包清单在 jar 里的路径。多份由 {@code packaging.resources.merges} 拼成一份。 */
    private static final String MANIFEST_RESOURCE = "hookpack.list";

    private HookPackLoader() {}

    /**
     * 加载并注册组件包里的 Hook。
     *
     * @param packageHooks 组件包可往里追加 Package Hook 的列表
     * @param zygoteHooks  组件包可往里追加 Zygote Hook 的列表
     */
    public static void register(List<PackageHook> packageHooks, List<ZygoteHook> zygoteHooks) {
        List<String> classNames = readManifest();
        if (classNames.isEmpty()) {
            logger.info("未发现 Hook 组件包，只加载内置 Hook");
            return;
        }

        ClassLoader loader = HookPackLoader.class.getClassLoader();
        int success = 0;
        for (String className : classNames) {
            try {
                Object hook = Class.forName(className, true, loader)
                        .getDeclaredConstructor().newInstance();
                if (registerOne(hook, packageHooks, zygoteHooks)) success++;
            } catch (Throwable e) {
                logger.error("组件包的 Hook 加载失败: " + className, e);
            }
        }
        logger.info("Hook 组件包加载完成，成功 " + success + " / " + classNames.size() + " 个");
    }

    private static boolean registerOne(Object hook, List<PackageHook> packageHooks,
                                       List<ZygoteHook> zygoteHooks) {
        if (hook instanceof PackageHook packageHook) {
            packageHooks.add(packageHook);
        } else if (hook instanceof ZygoteHook zygoteHook) {
            zygoteHooks.add(zygoteHook);
        } else {
            logger.warn(hook.getClass().getName() + " 既不是 PackageHook 也不是 ZygoteHook，已跳过");
            return false;
        }
        logger.info("已注册组件包的 Hook: " + ((XposedBasicHook<?>) hook).getHookName());
        return true;
    }

    /** 读出所有组件包清单里的类名，按出现顺序去重。 */
    private static List<String> readManifest() {
        List<String> classNames = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        try {
            Enumeration<URL> manifests = HookPackLoader.class.getClassLoader()
                    .getResources(MANIFEST_RESOURCE);
            while (manifests.hasMoreElements()) {
                collectFrom(manifests.nextElement(), classNames, seen);
            }
        } catch (IOException e) {
            logger.error("读取组件包清单失败", e);
        }
        return classNames;
    }

    private static void collectFrom(URL manifest, List<String> classNames, Set<String> seen)
            throws IOException {
        try (InputStream in = manifest.openStream();
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String className = line.trim();
                if (className.isEmpty() || className.startsWith("#")) continue;
                if (seen.add(className)) classNames.add(className);
            }
        }
    }
}
