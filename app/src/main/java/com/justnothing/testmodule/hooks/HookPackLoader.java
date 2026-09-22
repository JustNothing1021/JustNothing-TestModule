package com.justnothing.testmodule.hooks;

import com.justnothing.testmodule.utils.logging.Logger;

import java.util.Arrays;
import java.util.List;

/**
 * Hook 组件包加载器。
 * <p>
 * 组件包是构建期可选塞进 {@code app/hookpacks/} 的 jar。每个 jar 根目录放一份清单
 * {@code hookpack.list}，一行一个 Hook 类的全限定名（{@code #} 开头算注释）。
 * 多份清单由构建期的 {@code generateHookPackRegistry} 任务拼成一份，生成
 * {@link HookPackRegistry} 常量类。所以放几个组件包都行，不用改代码。
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
 * <h3>为什么清单是编译期常量，而不是运行时读出来的</h3>
 * <p>
 * <b>这是踩过一次大坑换来的，别改回去。</b> {@link HookEntry} 的静态块在 <b>zygote 进程</b>
 * 里执行（EdXposed 调 {@code initZygote} 之前得先把那个类初始化）。静态块里只要调一次
 * {@code ClassLoader.getResources("hookpack.list")}，classloader 就会把<b>模块自己的 APK</b>
 * 当 zip 打开来翻条目 —— zygote 的 fd 表里从此多一条指向
 * {@code /data/app/<pkg>-<后缀>/base.apk} 的 fd（{@code /proc/<zygote>/maps} 里也会多一条
 * {@code r--s ... base.apk}）。
 * </p>
 * <p>
 * 这条 fd 平时完全无害，但<b>重装模块</b>时 PackageManager 会把包挪到新的
 * {@code /data/app/<pkg>-<新后缀>/} 并删掉旧目录，fd 就此悬空；之后<b>任何一个新进程启动</b>，
 * fork 出来的子进程在 specialize 阶段要按路径重开 keep-list 里的描述符，{@code open()} 失败
 * → {@code RuntimeAbort("Unable to reopen whitelisted descriptors")} → SIGABRT，
 * 整机所有新进程级联崩掉，只能重启恢复。
 * </p>
 * <p>
 * 这件事发生在 <b>native 层、任何 Java 执行之前</b>
 * （{@code com_android_internal_os_Zygote.cpp:1101}），所以打日志、抛异常都拦不住，
 * 崩溃现场也只有一句 {@code Failed open}。实测对照：留着那次 {@code getResources} 的开机，
 * zygote 握着 {@code base.apk} 的 fd；把清单挪到编译期之后，fd 消失。八个已启用模块里
 * 也只有我们这样（其余七个的 zygote 阶段只碰 oat，从不打开 APK）。
 * </p>
 * <p>
 * 一句话：<b>zygote 阶段（含 {@link HookEntry} 的静态块和 {@code initZygote}）不要碰自己的
 * APK，也不要从 classloader 读任何资源。</b> {@link HookEntry} 结尾有一道自检盯着这条。
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
 *       再正常构建 APK 就行。清单会在构建期被并进 {@link HookPackRegistry}，
 *       里面的 Hook 和内置 Hook 一起在系统启动时装上。</li>
 * </ol>
 * <p>
 * 注意 {@code hook-api.jar} 里的 {@code include} 白名单在 {@code app/build.gradle} 的
 * {@code hookApiJar} 任务里，想给组件包开放更多包就往那儿加一行。
 * </p>
 */
public final class HookPackLoader {

    private static final Logger logger = Logger.getLoggerForName("HookPackLoader");

    private HookPackLoader() {}

    /**
     * 加载并注册组件包里的 Hook。
     *
     * @param packageHooks 组件包可往里追加 Package Hook 的列表
     * @param zygoteHooks  组件包可往里追加 Zygote Hook 的列表
     */
    public static void register(List<PackageHook> packageHooks, List<ZygoteHook> zygoteHooks) {
        List<String> classNames = Arrays.asList(HookPackRegistry.HOOK_CLASSES);
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
}
