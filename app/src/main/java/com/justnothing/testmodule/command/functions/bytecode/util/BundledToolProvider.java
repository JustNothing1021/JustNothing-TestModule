package com.justnothing.testmodule.command.functions.bytecode.util;

import android.os.Build;

import com.justnothing.testmodule.constants.FileDirectory;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * 把打包在模块 APK 里的命令行工具提取到设备上、并赋予执行权限。
 *
 * <p>目前只有一个工具：vendored 的 {@code vdexExtractor} —— 它负责把 vdex 里的 dex
 * 反 quicken（de-odex）。没有它，从 vdex 切出来的 dex 虽然能被反编译工具打开，
 * 但指令是被 ART 改写过的，代码不可信。</p>
 *
 * <p>为什么要自己写这一层：我们的代码运行在<b>别人的进程</b>里（system_server / 被注入的应用），
 * 那里没有可用的 {@code Context}，也就拿不到 {@code AssetManager}。
 * 所以只能沿用项目既有的做法（{@code ShellService.copyHookExamplesToScripts} 与
 * {@code SandboxTestMain.extractNativeLib}）：用 {@link ZipFile} 直接读模块 APK 里的条目。</p>
 *
 * <p><b>这些工具是独立可执行文件，不是 .so，因此不能用 {@code System.load()}</b>——
 * 提取 + chmod 之后要以子进程方式运行。</p>
 */
public final class BundledToolProvider {

    private static final Logger logger = Logger.getLoggerForName("BundledToolProvider");

    /** 与构建脚本放置的位置一致：assets/bin/&lt;abi&gt;/&lt;toolName&gt;。 */
    private static final String ASSET_DIR = "assets/bin";

    private BundledToolProvider() {
    }

    /**
     * 取得工具在本地的可执行路径；本地没有就从模块 APK 里提取。
     *
     * <p>失败时返回 {@code null} 而不是抛异常：工具是"增强能力"，缺失时调用方应该
     * 降级（比如提示"该功能在当前设备不可用"），而不是把整个命令搞崩。</p>
     */
    public static synchronized File ensure(String toolName) {
        if (toolName == null || toolName.isEmpty()) {
            return null;
        }

        File target = new File(FileDirectory.TOOLS_DIR, toolName);
        if (target.isFile() && target.canExecute()) {
            return target;
        }

        String abi = Build.SUPPORTED_ABIS[0];
        String entryName = ASSET_DIR + "/" + abi + "/" + toolName;

        String modulePath = DataBridge.getModulePath();
        if (modulePath == null) {
            logger.warn("模块路径未初始化，无法提取 " + toolName);
            return null;
        }
        File moduleApk = new File(modulePath);
        if (!moduleApk.isFile()) {
            logger.warn("模块 APK 不存在: " + modulePath);
            return null;
        }

        try (ZipFile zipFile = new ZipFile(moduleApk)) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null) {
                logger.warn("模块 APK 中没有 " + entryName
                        + "（构建时可能因为没有 NDK 而跳过了 vdexExtractor）");
                return null;
            }

            File dir = new File(FileDirectory.TOOLS_DIR);
            if (!dir.isDirectory() && !dir.mkdirs() && !dir.isDirectory()) {
                logger.warn("无法创建工具目录: " + dir);
                return null;
            }
            // 目录必须对其他进程开放。第一次很可能是 system_server 建的，而它在默认 umask
            // 下建出来的是 0700 —— 那样普通应用进程连进都进不去，会把"工具已存在"
            // 误判成"工具不存在"，然后永远走兜底策略。
            if (!chmod(dir, "0777")) {
                logger.warn("无法放开工具目录权限: " + dir + "（其他进程可能读不到工具）");
            }

            try (InputStream in = zipFile.getInputStream(entry);
                 FileOutputStream out = new FileOutputStream(target)) {
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                }
            }

            if (!makeExecutable(target)) {
                logger.warn("无法给 " + target + " 加执行权限，工具将不可用");
                return null;
            }

            logger.info("已提取工具 " + toolName + "（abi=" + abi + "）到 " + target);
            return target;
        } catch (IOException e) {
            logger.error("提取工具失败: " + toolName, e);
            return null;
        }
    }

    /**
     * 用 {@code /system/bin/chmod} 改权限位。
     *
     * <p>刻意不经过 su / RootProcessPool：这一步只是给"我们自己刚写出来的文件"加权限位，
     * 不该触发 Magisk 的 root 授权（那会带来授权弹窗等一连串副作用）。</p>
     */
    private static boolean makeExecutable(File file) {
        return chmod(file, "0755");
    }

    private static boolean chmod(File file, String mode) {
        try {
            Process process = new ProcessBuilder("/system/bin/chmod", mode, file.getAbsolutePath())
                    .redirectErrorStream(true)
                    .start();
            return process.waitFor() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("chmod 被中断: " + file);
            return false;
        } catch (IOException e) {
            logger.warn("chmod 执行失败: " + e.getMessage());
            return false;
        }
    }
}
