package com.justnothing.testmodule.command.functions.tests;

import com.justnothing.engine.ScriptRunner;
import com.justnothing.engine.api.DefaultOutputHandler;
import com.justnothing.engine.security.SandboxConfig;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.sandbox.BlockGuardSandbox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.justnothing.testmodule.command.framework.annotation.Cmd;

@Cmd(name = "sandboxtest", description = "沙箱安全机制测试")
public class SandboxTestMain extends MainCommand<CommandResult> {

    private static volatile boolean nativeLoaded = false;
    private static volatile Throwable nativeLoadError = null;
    private static volatile String nativeLibPath = null;

    // 本文件内多处复用的输出文案（「错误: 」这类跨文件通用的在 CliMessages 里，不在这里重复定义）。
    private static final Text VALUE_AVAILABLE = Text.zhEn("可用", "available");
    private static final Text VALUE_UNAVAILABLE = Text.zhEn("不可用", "unavailable");
    private static final Text VALUE_SUCCESS = Text.zhEn("成功", "success");
    private static final Text VALUE_FAILURE = Text.zhEn("失败", "failed");
    private static final Text VALUE_BLOCKED = Text.zhEn("已拦截 ✓", "Blocked ✓");
    private static final Text VALUE_NOT_BLOCKED = Text.zhEn("未拦截 ✗", "Not blocked ✗");
    private static final Text INFO_PREFIX = Text.zhEn("  信息: ", "  Info: ");
    private static final Text CONCLUSION_PREFIX = Text.zhEn("结论: ", "Conclusion: ");
    private static final Text WARNING_PREFIX = Text.zhEn("警告: ", "Warning: ");
    private static final Text HINT_PREFIX = Text.zhEn("提示: ", "Hint: ");
    private static final Text ERROR_CODE_PREFIX = Text.zhEn("  错误码: ", "  Error code: ");
    private static final Text TEST_FAILED = Text.zhEn("测试失败: %s", "Test failed: %s");
    private static final Text RUNNING_IN_ISOLATED_THREAD =
            Text.zhEn("[%s 测试] 在独立线程中执行...", "[%s test] running in an isolated thread...");

    public SandboxTestMain() {
        super("SandboxTest", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return Text.zhEn(
                "===== 沙箱测试命令 =====\n\n用法: sandboxtest <子命令>\n\n子命令:\n    load             - 加载 native 库\n    blockguard       - 测试 BlockGuard I/O 拦截\n    seccomp          - 测试 seccomp-bpf 进程拦截\n    clonefork        - 测试 clone/fork 拦截\n    penetration      - 安全渗透测试（尝试攻破沙箱）\n    all              - 运行所有测试\n    info             - 显示环境信息\n\n说明:\n    此命令用于测试 Android 沙箱机制的可用性。\n    - BlockGuard: 拦截磁盘 I/O 和网络操作\n    - seccomp-bpf: 拦截进程创建和线程创建\n    - penetration: 尝试各种方法绕过安全限制\n\n相关命令:\n    anonclasstest    - 匿名类生成诊断 (独立工具)\n\n",
                "===== Sandbox test command =====\n\nUsage: sandboxtest <subcommand>\n\nSubcommands:\n    load             - load the native library\n    blockguard       - test BlockGuard I/O interception\n    seccomp          - test seccomp-bpf process interception\n    clonefork        - test clone/fork interception\n    penetration      - security penetration test (try to break the sandbox)\n    all              - run all tests\n    info             - show environment info\n\nNotes:\n    Checks whether the Android sandbox mechanisms are available.\n    - BlockGuard: intercepts disk I/O and network operations\n    - seccomp-bpf: intercepts process and thread creation\n    - penetration: tries various ways to bypass the restrictions\n\nSee also:\n    anonclasstest    - anonymous class generation diagnostics (standalone tool)\n\n")
                .text();
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        String[] args = context.args();
        
        if (args.length < 1) {
            context.println(getHelpText());
            return createErrorResult(Text.zhEn("参数不足，需要指定子命令", "Not enough arguments; a subcommand is required").text());
        }

        String subCommand = args[0];

        switch (subCommand) {
            case "load" -> loadNativeLibrary(context);
            case "blockguard" -> executeInIsolatedThread(context, "BlockGuard", () -> testBlockGuardInternal(context));
            case "seccomp" -> executeInIsolatedThread(context, "seccomp", () -> testSeccompInternal(context));
            case "clonefork" -> executeInIsolatedThread(context, "clone/fork", () -> testCloneForkInternal(context));
            case "penetration" -> executeInIsolatedThread(context, "penetration", () -> testPenetrationInternal(context));
            case "all" -> runAllTests(context);
            case "info" -> showEnvironmentInfo(context);
            default -> {
                context.print(Text.zhEn("未知子命令: ", "Unknown subcommand: ").text(), Colors.RED);
                context.println(subCommand, Colors.YELLOW);
                context.println(getHelpText());
            }
        }
        return createSuccessResult(Text.zhEn("沙箱测试命令执行完成", "Sandbox test command finished").text());
    }

    private interface TestRunnable {
        void run() throws Exception;
    }

    private void executeInIsolatedThread(CommandExecutor.CmdExecContext context, String testName, TestRunnable test) {
        context.println(RUNNING_IN_ISOLATED_THREAD.format(testName), Colors.CYAN);
        context.println("", Colors.WHITE);
        
        AtomicReference<Throwable> errorRef = new AtomicReference<>(null);
        AtomicReference<Boolean> completed = new AtomicReference<>(false);
        
        Future<?> future = ThreadPoolManager.submitIOCallable(() -> {
            try {
                test.run();
                completed.set(true);
            } catch (Throwable e) {
                errorRef.set(e);
            }
            return null;
        });
        
        try {
            future.get(60, TimeUnit.SECONDS);
            
            if (errorRef.get() != null) {
                Throwable e = errorRef.get();
                context.print(Text.zhEn("测试过程中发生异常: ", "Exception while running the test: ").text(), Colors.RED);
                context.println(e.getMessage(), Colors.ORANGE);
                context.output().printStackTrace(e);
            } else if (!completed.get()) {
                context.println(Text.zhEn("测试未完成（未知状态）", "Test did not complete (unknown state)").text(), Colors.ORANGE);
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            context.println(Text.zhEn("测试超时（60秒），已取消", "Test timed out (60s) and was cancelled").text(), Colors.RED);
        } catch (Exception e) {
            context.print(Text.zhEn("等待测试结果时发生异常: ", "Exception while waiting for the test result: ").text(), Colors.RED);
            context.println(e.getMessage(), Colors.ORANGE);
        }
    }

    private void showEnvironmentInfo(CommandExecutor.CmdExecContext context) {
        context.println(Text.zhEn("===== 环境信息 =====", "===== Environment info =====").text(), Colors.CYAN);
        context.println("", Colors.WHITE);
        
        context.print(Text.zhEn("进程 ID: ", "Process ID: ").text(), Colors.CYAN);
        context.println(String.valueOf(android.os.Process.myPid()), Colors.WHITE);
        
        context.print(Text.zhEn("用户 ID: ", "User ID: ").text(), Colors.CYAN);
        context.println(String.valueOf(android.os.Process.myUid()), Colors.WHITE);
        
        context.println("", Colors.WHITE);
        context.print(Text.zhEn("Native 库状态: ", "Native library: ").text(), Colors.CYAN);
        if (nativeLoaded) {
            context.println(Text.zhEn("已加载 (%s)", "Loaded (%s)").format(nativeLibPath), Colors.GREEN);
        } else if (nativeLoadError != null) {
            context.println(Text.zhEn("加载失败", "Load failed").text(), Colors.RED);
        } else {
            context.println(Text.zhEn("未加载", "Not loaded").text(), Colors.GRAY);
        }
        
        context.println("", Colors.WHITE);
        context.print(Text.zhEn("模块路径: ", "Module path: ").text(), Colors.CYAN);
        String modulePath = DataBridge.getModulePath();
        context.println(modulePath != null ? modulePath : Text.zhEn("未知", "unknown").text(), Colors.GRAY);
        
        context.println("", Colors.WHITE);
        context.print("BlockGuard: ", Colors.CYAN);
        context.println(BlockGuardSandbox.isBlockGuardAvailable() ? VALUE_AVAILABLE.text() : VALUE_UNAVAILABLE.text(), 
                BlockGuardSandbox.isBlockGuardAvailable() ? Colors.GREEN : Colors.RED);
    }

    private void loadNativeLibrary(CommandExecutor.CmdExecContext context) {
        if (nativeLoaded) {
            context.println(Text.zhEn("Native 库已加载", "Native library already loaded").text(), Colors.GREEN);
            context.println(Text.zhEn("路径: %s", "Path: %s").format(nativeLibPath), Colors.GRAY);
            return;
        }

        if (nativeLoadError != null) {
            context.println(Text.zhEn("Native 库加载已失败，不再重试", "Native library load already failed; not retrying").text(), Colors.RED);
            context.println(CliMessages.ERROR_PREFIX.text() + nativeLoadError.getMessage(), Colors.GRAY);
            return;
        }

        synchronized (SandboxTestMain.class) {
            if (nativeLoaded) {
                context.println(Text.zhEn("Native 库已加载", "Native library already loaded").text(), Colors.GREEN);
                return;
            }

            try {
                String apkPath = DataBridge.getModulePath();
                if (apkPath == null) {
                    throw new RuntimeException(Text.zhEn("无法获取模块 APK 路径", "Cannot resolve the module APK path").text());
                }
                context.println(Text.zhEn("模块 APK: %s", "Module APK: %s").format(apkPath), Colors.GRAY);

                String abi = getAbi();
                context.println(Text.zhEn("当前 ABI: %s", "Current ABI: %s").format(abi), Colors.GRAY);

                File libFile = extractNativeLib(apkPath, abi, context);
                if (libFile == null) {
                    throw new RuntimeException(Text.zhEn("无法从 APK 提取 native 库", "Cannot extract the native library from the APK").text());
                }

                context.println(Text.zhEn("提取到: %s", "Extracted to: %s").format(libFile.getAbsolutePath()), Colors.GRAY);

                System.load(libFile.getAbsolutePath());
                nativeLibPath = libFile.getAbsolutePath();
                nativeLoaded = true;

                context.println(Text.zhEn("Native 库加载成功!", "Native library loaded!").text(), Colors.GREEN);
            } catch (Throwable e) {
                nativeLoadError = e;
                context.println(Text.zhEn("Native 库加载失败", "Failed to load the native library").text(), Colors.RED);
                context.println(CliMessages.ERROR_PREFIX.text() + e.getMessage(), Colors.GRAY);
            }
        }
    }

    private String getAbi() {
        return android.os.Build.SUPPORTED_ABIS[0];
    }

    private File extractNativeLib(String apkPath, String abi, CommandExecutor.CmdExecContext context) {
        String libName = "libsandbox_test.so";
        String entryName = "lib/" + abi + "/" + libName;

        try (ZipFile zipFile = new ZipFile(apkPath)) {
            ZipEntry entry = zipFile.getEntry(entryName);
            if (entry == null) {
                context.println(Text.zhEn("APK 中未找到: %s", "Not found in the APK: %s").format(entryName), Colors.RED);
                for (String supportedAbi : android.os.Build.SUPPORTED_ABIS) {
                    String altEntry = "lib/" + supportedAbi + "/" + libName;
                    entry = zipFile.getEntry(altEntry);
                    if (entry != null) {
                        entryName = altEntry;
                        context.println(Text.zhEn("使用备用 ABI: %s", "Using the fallback ABI: %s").format(supportedAbi), Colors.GRAY);
                        break;
                    }
                }
            }

            if (entry == null) {
                context.println(Text.zhEn("APK 中未找到任何 native 库", "No native library found in the APK").text(), Colors.RED);
                return null;
            }

            File outputDir = new File("/data/local/tmp");
            if (!outputDir.exists() || !outputDir.canWrite()) {
                outputDir = DataBridge.getDataDir();
            }

            File libFile = new File(outputDir, libName);

            try (InputStream is = zipFile.getInputStream(entry);
                 FileOutputStream fos = new FileOutputStream(libFile)) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = is.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
            }

            if (!libFile.setExecutable(true)) {
                context.println(WARNING_PREFIX.text() + Text.zhEn("无法设置可执行权限", "cannot set the executable permission").text(), Colors.ORANGE);
            }

            return libFile;
        } catch (Exception e) {
            context.println(Text.zhEn("提取失败: %s", "Extraction failed: %s").format(e.getMessage()), Colors.RED);
            return null;
        }
    }

    private void testBlockGuardInternal(CommandExecutor.CmdExecContext context) {
        context.println(Text.zhEn("===== 测试 BlockGuard I/O 拦截 =====", "===== Testing BlockGuard I/O interception =====").text(), Colors.CYAN);
        context.println("", Colors.WHITE);

        boolean available = BlockGuardSandbox.isBlockGuardAvailable();
        context.print(Text.zhEn("BlockGuard 可用性: ", "BlockGuard available: ").text(), Colors.CYAN);
        context.println(available ? VALUE_AVAILABLE.text() : VALUE_UNAVAILABLE.text(), available ? Colors.GREEN : Colors.RED);

        if (!available) {
            context.println("", Colors.WHITE);
            context.println(CONCLUSION_PREFIX.text() + Text.zhEn("BlockGuard 不可用（非 Android 环境？）", "BlockGuard is unavailable (not an Android environment?)").text(), Colors.RED);
            return;
        }

        File testFile = new File(DataBridge.getDataDir(), "sandbox_test_" + System.currentTimeMillis() + ".tmp");
        
        BlockGuardSandbox.bypass(() -> {
            try {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(testFile);
                fos.write("test".getBytes());
                fos.close();
            } catch (Exception e) {
                context.println(WARNING_PREFIX.text() + Text.zhEn("无法创建测试文件: %s", "cannot create the test file: %s").format(e.getMessage()), Colors.ORANGE);
            }
        });
        
        context.println("", Colors.WHITE);
        context.println(Text.zhEn("测试磁盘写入拦截...", "Testing disk write interception...").text(), Colors.CYAN);
        context.println(Text.zhEn("测试文件: %s", "Test file: %s").format(testFile.getAbsolutePath()), Colors.GRAY);
        
        boolean[] writeBlocked = {false};
        String[] writeError = {null};
        
        SandboxConfig writeConfig = SandboxConfig.builder()
                .allowDiskRead()
                .denyDiskWrite()
                .allowNetwork()
                .allowLocalSocket()
                .allowThreadCreate()
                .allowThreadModify()
                .allowProcessCreate()
                .build();
        try {
            BlockGuardSandbox.execute(writeConfig, () -> {
                try {
                    java.io.FileOutputStream fos = new java.io.FileOutputStream(testFile);
                    fos.write("test".getBytes());
                    fos.close();
                } catch (SecurityException e) {
                    writeBlocked[0] = true;
                    writeError[0] = e.getMessage();
                } catch (java.io.IOException e) {
                    writeError[0] = "IOException: " + e.getMessage();
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof SecurityException) {
                writeBlocked[0] = true;
                writeError[0] = e.getCause().getMessage();
            }
        }

        context.print(Text.zhEn("磁盘写入拦截: ", "Disk write interception: ").text(), Colors.CYAN);
        context.println(writeBlocked[0] ? VALUE_BLOCKED.text() : VALUE_NOT_BLOCKED.text(), writeBlocked[0] ? Colors.GREEN : Colors.RED);
        if (writeError[0] != null) {
            context.println(INFO_PREFIX.text() + writeError[0], Colors.GRAY);
        }
        
        context.println("", Colors.WHITE);
        context.println(Text.zhEn("测试磁盘读取拦截...", "Testing disk read interception...").text(), Colors.CYAN);
        
        boolean[] readBlocked = {false};
        String[] readError = {null};
        
        SandboxConfig readConfig = SandboxConfig.builder()
                .denyDiskRead()
                .allowDiskWrite()
                .allowNetwork()
                .allowLocalSocket()
                .allowThreadCreate()
                .allowThreadModify()
                .allowProcessCreate()
                .build();
        try {
            BlockGuardSandbox.execute(readConfig, () -> {
                try {
                    java.io.FileInputStream fis = new java.io.FileInputStream(testFile);
                    fis.read();
                    fis.close();
                } catch (SecurityException e) {
                    readBlocked[0] = true;
                    readError[0] = e.getMessage();
                } catch (java.io.IOException e) {
                    readError[0] = "IOException: " + e.getMessage();
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof SecurityException) {
                readBlocked[0] = true;
                readError[0] = e.getCause().getMessage();
            }
        }

        context.print(Text.zhEn("磁盘读取拦截: ", "Disk read interception: ").text(), Colors.CYAN);
        context.println(readBlocked[0] ? VALUE_BLOCKED.text() : VALUE_NOT_BLOCKED.text(), readBlocked[0] ? Colors.GREEN : Colors.RED);
        if (readError[0] != null) {
            context.println(INFO_PREFIX.text() + readError[0], Colors.GRAY);
        }

        context.println("", Colors.WHITE);
        context.println(Text.zhEn("测试 File 元数据读取拦截...", "Testing File metadata read interception...").text(), Colors.CYAN);

        // java.io.File / java.io.UnixFileSystem 曾在读白名单里，而白名单是"栈里出现即放行"，
        // 于是 File.exists()/length()/list() 这类调用栈里必然有 java.io.File 的读全部漏过。
        boolean[] metaBlocked = {false};
        String[] metaError = {null};

        try {
            BlockGuardSandbox.execute(readConfig, () -> {
                try {
                    if (!new File("/system/build.prop").exists()) {
                        metaError[0] = Text.zhEn("文件不存在", "file does not exist").text();
                    }
                } catch (SecurityException e) {
                    metaBlocked[0] = true;
                    metaError[0] = e.getMessage();
                }
            });
        } catch (RuntimeException e) {
            if (e.getCause() instanceof SecurityException) {
                metaBlocked[0] = true;
                metaError[0] = e.getCause().getMessage();
            }
        }

        context.print(Text.zhEn("File.exists() 拦截: ", "File.exists() interception: ").text(), Colors.CYAN);
        context.println(metaBlocked[0] ? VALUE_BLOCKED.text() : VALUE_NOT_BLOCKED.text(), metaBlocked[0] ? Colors.GREEN : Colors.RED);
        if (metaError[0] != null) {
            context.println(INFO_PREFIX.text() + metaError[0], Colors.GRAY);
        }

        context.println("", Colors.WHITE);
        context.println(Text.zhEn("测试子线程继承（裸线程 vs wrap）...", "Testing child thread inheritance (raw thread vs wrap)...").text(), Colors.CYAN);
        context.println(Text.zhEn("注: libcore 的 BlockGuard 策略是 per-thread 且不继承，裸子线程会绕过 Java 层", "Note: libcore's BlockGuard policy is per-thread and not inherited, so a raw child thread bypasses the Java layer").text(), Colors.GRAY);

        File rawFile = new File(DataBridge.getDataDir(), "sandbox_child_raw_" + System.currentTimeMillis() + ".tmp");
        File wrappedFile = new File(DataBridge.getDataDir(), "sandbox_child_wrapped_" + System.currentTimeMillis() + ".tmp");

        boolean[] rawBlocked = {false};
        boolean[] wrappedBlocked = {false};
        String[] rawError = {null};
        String[] wrappedError = {null};

        try {
            BlockGuardSandbox.execute(writeConfig, () -> {
                try {
                    childTryWrite(rawFile, false, rawBlocked, rawError);
                    childTryWrite(wrappedFile, true, wrappedBlocked, wrappedError);
                } catch (Exception e) {
                    rawError[0] = Text.zhEn("子线程测试异常: %s", "Child thread test failed: %s").format(e.getMessage());
                }
            });
        } catch (RuntimeException e) {
            rawError[0] = Text.zhEn("沙箱外层异常: %s", "Exception outside the sandbox: %s").format(e.getMessage());
        }

        context.print(Text.zhEn("裸子线程写入: ", "Write from a raw child thread: ").text(), Colors.CYAN);
        context.println(rawBlocked[0] ? Text.zhEn("已拦截", "Blocked").text() : Text.zhEn("未拦截（符合预期：Java 层不继承）", "Not blocked (expected: the Java layer policy is not inherited)").text(),
                rawBlocked[0] ? Colors.GREEN : Colors.ORANGE);
        if (rawError[0] != null) {
            context.println(INFO_PREFIX.text() + rawError[0], Colors.GRAY);
        }

        context.print(Text.zhEn("wrap 后子线程写入: ", "Write from a wrapped child thread: ").text(), Colors.CYAN);
        context.println(wrappedBlocked[0] ? VALUE_BLOCKED.text() : VALUE_NOT_BLOCKED.text(), wrappedBlocked[0] ? Colors.GREEN : Colors.RED);
        if (wrappedError[0] != null) {
            context.println(INFO_PREFIX.text() + wrappedError[0], Colors.GRAY);
        }

        context.println("", Colors.WHITE);
        context.println(Text.zhEn("测试线程池交接（沙箱内提交到 IO 池）...", "Testing thread-pool hand-off (submitting to the IO pool from inside the sandbox)...").text(), Colors.CYAN);
        context.println(Text.zhEn("注: 池线程是另一个线程，交接时 Java 层策略会丢，靠 ThreadPoolManager.wrapTask 补装", "Note: a pool thread is a different thread, so the Java layer policy is lost on hand-off and is re-installed by ThreadPoolManager.wrapTask").text(), Colors.GRAY);

        File pooledFile = new File(DataBridge.getDataDir(), "sandbox_pool_" + System.currentTimeMillis() + ".tmp");
        boolean[] pooledBlocked = {false};
        String[] pooledError = {null};

        try {
            BlockGuardSandbox.execute(writeConfig, () -> {
                try {
                    Future<?> future = ThreadPoolManager.submitIOCallable(() -> {
                        try (FileOutputStream fos = new FileOutputStream(pooledFile)) {
                            fos.write("test".getBytes());
                        } catch (SecurityException e) {
                            pooledBlocked[0] = true;
                            pooledError[0] = e.getMessage();
                        } catch (java.io.IOException e) {
                            pooledError[0] = "IOException: " + e.getMessage();
                        }
                        return null;
                    });
                    if (future != null) {
                        future.get(10, TimeUnit.SECONDS);
                    } else {
                        pooledError[0] = Text.zhEn("线程池未初始化，任务未提交", "thread pool not initialized; task not submitted").text();
                    }
                } catch (Exception e) {
                    pooledError[0] = Text.zhEn("提交/等待异常: %s", "Submit/wait failed: %s").format(e.getClass().getSimpleName())
                            + (e.getMessage() != null ? ": " + e.getMessage() : "");
                }
            });
        } catch (RuntimeException e) {
            pooledError[0] = Text.zhEn("沙箱外层异常: %s", "Exception outside the sandbox: %s").format(e.getMessage());
        }

        context.print(Text.zhEn("池线程写入: ", "Pool thread write: ").text(), Colors.CYAN);
        context.println(pooledBlocked[0] ? VALUE_BLOCKED.text() : VALUE_NOT_BLOCKED.text(), pooledBlocked[0] ? Colors.GREEN : Colors.RED);
        if (pooledError[0] != null) {
            context.println(INFO_PREFIX.text() + pooledError[0], Colors.GRAY);
        }

        BlockGuardSandbox.bypass(() -> {
            for (File f : new File[] {testFile, rawFile, wrappedFile, pooledFile}) {
                if (f.exists()) {
                    f.delete();
                }
            }
        });

        context.println("", Colors.WHITE);
        boolean allOk = writeBlocked[0] && readBlocked[0] && metaBlocked[0]
                && wrappedBlocked[0] && pooledBlocked[0];
        if (allOk) {
            context.println(CONCLUSION_PREFIX.text() + Text.zhEn("BlockGuard I/O 拦截正常工作", "BlockGuard I/O interception works").text(), Colors.GREEN);
        } else if (writeBlocked[0] || readBlocked[0] || metaBlocked[0]
                || wrappedBlocked[0] || pooledBlocked[0]) {
            context.println(CONCLUSION_PREFIX.text() + Text.zhEn("BlockGuard 部分工作", "BlockGuard partially works").text(), Colors.ORANGE);
        } else {
            context.println(CONCLUSION_PREFIX.text() + Text.zhEn("BlockGuard 未拦截任何操作", "BlockGuard did not block anything").text(), Colors.RED);
            context.println(HINT_PREFIX.text() + Text.zhEn("可能需要检查 BlockGuard 策略设置", "the BlockGuard policy settings may need checking").text(), Colors.GRAY);
        }
    }

    /**
     * 在子线程里尝试写文件，回填是否被拦。
     *
     * @param wrap true 时用 {@link BlockGuardSandbox#wrap} 包装 —— 因为 libcore 的 BlockGuard
     *             策略不随线程继承，不经包装的子线程拿到的永远是宽松策略
     */
    private void childTryWrite(File target, boolean wrap, boolean[] blocked, String[] error) throws Exception {
        Runnable write = () -> {
            try (FileOutputStream fos = new FileOutputStream(target)) {
                fos.write("test".getBytes());
            } catch (java.io.IOException e) {
                error[0] = "IOException: " + e.getMessage();
            }
        };

        Runnable task = wrap ? BlockGuardSandbox.wrap(write) : write;
        Thread thread = new Thread(() -> {
            try {
                task.run();
            } catch (Throwable e) {
                blocked[0] = e instanceof SecurityException;
                error[0] = e.getClass().getSimpleName() + (e.getMessage() != null ? ": " + e.getMessage() : "");
            }
        }, "SandboxTest-Child");
        thread.start();
        thread.join();
    }

    private void testSeccompInternal(CommandExecutor.CmdExecContext context) {
        if (!ensureNativeLoaded(context)) {
            return;
        }

        context.println(Text.zhEn("===== 测试 seccomp-bpf 进程拦截 =====", "===== Testing seccomp-bpf process interception =====").text(), Colors.CYAN);
        context.println("", Colors.WHITE);

        try {
            SeccompTestResult result = testSeccompNative();

            context.print("prctl(PR_SET_NO_NEW_PRIVS): ", Colors.CYAN);
            context.println(result.prctlSuccess ? VALUE_SUCCESS.text() : VALUE_FAILURE.text(), result.prctlSuccess ? Colors.GREEN : Colors.RED);
            if (result.prctlError != 0) {
                context.println(ERROR_CODE_PREFIX.text() + result.prctlError + " (" + strerror(result.prctlError) + ")", Colors.GRAY);
            }

            context.print("seccomp(SECCOMP_SET_MODE_FILTER): ", Colors.CYAN);
            context.println(result.seccompSuccess ? VALUE_SUCCESS.text() : VALUE_FAILURE.text(), result.seccompSuccess ? Colors.GREEN : Colors.RED);
            if (result.seccompError != 0) {
                context.println(ERROR_CODE_PREFIX.text() + result.seccompError + " (" + strerror(result.seccompError) + ")", Colors.GRAY);
            }

            context.print(Text.zhEn("execve 拦截测试: ", "execve interception test: ").text(), Colors.CYAN);
            context.println(result.execveBlocked ? VALUE_BLOCKED.text() : VALUE_NOT_BLOCKED.text(), result.execveBlocked ? Colors.GREEN : Colors.RED);

            context.println("", Colors.WHITE);
            if (result.seccompSuccess && result.execveBlocked) {
                context.println(CONCLUSION_PREFIX.text() + Text.zhEn("seccomp-bpf 可用于进程创建拦截", "seccomp-bpf can intercept process creation").text(), Colors.GREEN);
            } else if (result.prctlSuccess && !result.seccompSuccess) {
                context.println(CONCLUSION_PREFIX.text() + Text.zhEn("prctl 可用但 seccomp 被阻止，可能是 SELinux 限制", "prctl works but seccomp is blocked, possibly by SELinux").text(), Colors.ORANGE);
            } else {
                context.println(CONCLUSION_PREFIX.text() + Text.zhEn("seccomp-bpf 不可用", "seccomp-bpf is unavailable").text(), Colors.RED);
            }
        } catch (Throwable e) {
            context.println(TEST_FAILED.format(e.getMessage()), Colors.RED);
        }
    }

    private void testCloneForkInternal(CommandExecutor.CmdExecContext context) {
        if (!ensureNativeLoaded(context)) {
            return;
        }

        context.println(Text.zhEn("===== 测试 clone/fork 拦截 =====", "===== Testing clone/fork interception =====").text(), Colors.CYAN);
        context.println("", Colors.WHITE);

        context.println(Text.zhEn("测试场景: 同时拦截线程和进程创建", "Scenario: intercepting both thread and process creation").text(), Colors.CYAN);
        context.println("", Colors.WHITE);

        try {
            CloneForkTestResult result = testCloneForkBlocking(true, true);

            context.print(Text.zhEn("seccomp 过滤器安装: ", "seccomp filter installation: ").text(), Colors.CYAN);
            context.println(result.seccompSuccess ? VALUE_SUCCESS.text() : VALUE_FAILURE.text(), result.seccompSuccess ? Colors.GREEN : Colors.RED);

            if (!result.seccompSuccess) {
                context.print(CliMessages.ERR_MESSAGE.text(), Colors.RED);
                context.println(result.errorMsg != null ? result.errorMsg : Text.zhEn("未知错误", "unknown error").text(), Colors.GRAY);
                return;
            }

            if (result.errorMsg != null) {
                context.print(WARNING_PREFIX.text(), Colors.ORANGE);
                context.println(result.errorMsg, Colors.GRAY);
            }

            context.println("", Colors.WHITE);
            
            context.print(Text.zhEn("fork() 拦截: ", "fork() interception: ").text(), Colors.CYAN);
            context.println(result.forkBlocked ? VALUE_BLOCKED.text() : VALUE_NOT_BLOCKED.text(), result.forkBlocked ? Colors.GREEN : Colors.RED);

            context.print(Text.zhEn("pthread_create() 拦截: ", "pthread_create() interception: ").text(), Colors.CYAN);
            context.println(result.threadBlocked ? VALUE_BLOCKED.text() : VALUE_NOT_BLOCKED.text(), result.threadBlocked ? Colors.GREEN : Colors.RED);

            context.println("", Colors.WHITE);
            
            if (result.forkBlocked && result.threadBlocked) {
                context.println(CONCLUSION_PREFIX.text() + Text.zhEn("seccomp-bpf 可完全拦截线程和进程创建", "seccomp-bpf fully intercepts thread and process creation").text(), Colors.GREEN);
            } else if (result.errorMsg != null) {
                context.println(CONCLUSION_PREFIX.text() + Text.zhEn("测试未完成，子进程在测试期间崩溃", "the test did not finish; the child process crashed during the test").text(), Colors.ORANGE);
                context.println(HINT_PREFIX.text() + Text.zhEn("可能是 BPF 过滤器配置问题或设备兼容性问题", "this may be a BPF filter configuration problem or a device compatibility issue").text(), Colors.GRAY);
            } else if (result.forkBlocked || result.threadBlocked) {
                context.println(CONCLUSION_PREFIX.text() + Text.zhEn("seccomp-bpf 部分有效，需要进一步调试", "seccomp-bpf partially works; more debugging is needed").text(), Colors.ORANGE);
            } else {
                context.println(CONCLUSION_PREFIX.text() + Text.zhEn("seccomp-bpf 未拦截任何操作", "seccomp-bpf did not block anything").text(), Colors.RED);
            }

            context.println("", Colors.WHITE);
            context.println(Text.zhEn("技术说明:", "Technical notes:").text(), Colors.CYAN);
            context.println(Text.zhEn("  - clone() 系统调用通过 CLONE_VM 标志区分线程/进程", "  - the clone() syscall distinguishes threads from processes via the CLONE_VM flag").text(), Colors.GRAY);
            context.println(Text.zhEn("  - fork()/vfork() 是进程创建的传统方式", "  - fork()/vfork() are the traditional ways to create a process").text(), Colors.GRAY);
            context.println(Text.zhEn("  - pthread_create() 内部使用 clone() + CLONE_VM", "  - pthread_create() uses clone() + CLONE_VM internally").text(), Colors.GRAY);

        } catch (Throwable e) {
            context.println(TEST_FAILED.format(e.getMessage()), Colors.RED);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean ensureNativeLoaded(CommandExecutor.CmdExecContext context) {
        if (!nativeLoaded) {
            context.println(Text.zhEn("Native 库未加载，正在加载...", "Native library not loaded; loading it now...").text(), Colors.ORANGE);
            BlockGuardSandbox.bypass(() -> loadNativeLibrary(context));
        }

        if (!nativeLoaded) {
            context.println(CliMessages.ERROR_PREFIX.text() + Text.zhEn("Native 库未加载", "native library not loaded").text(), Colors.RED);
            return false;
        }
        return true;
    }

    private void runAllTests(CommandExecutor.CmdExecContext context) {
        showEnvironmentInfo(context);
        context.println("", Colors.WHITE);
        context.println("─────────────────────────────", Colors.GRAY);
        context.println("", Colors.WHITE);
        executeInIsolatedThread(context, "BlockGuard", () -> testBlockGuardInternal(context));
        context.println("", Colors.WHITE);
        context.println("─────────────────────────────", Colors.GRAY);
        context.println("", Colors.WHITE);
        executeInIsolatedThread(context, "seccomp", () -> testSeccompInternal(context));
        context.println("", Colors.WHITE);
        context.println("─────────────────────────────", Colors.GRAY);
        context.println("", Colors.WHITE);
        executeInIsolatedThread(context, "clone/fork", () -> testCloneForkInternal(context));
    }

    public static class SeccompTestResult {
        public boolean prctlSuccess;
        public int prctlError;
        public boolean seccompSuccess;
        public int seccompError;
        public boolean execveBlocked;
    }

    public static class CloneForkTestResult {
        public boolean seccompSuccess;
        public boolean forkBlocked;
        public boolean threadBlocked;
        public String errorMsg;
    }

    private void testPenetrationInternal(CommandExecutor.CmdExecContext context) {
        context.println(Text.zhEn("===== 安全渗透测试 =====", "===== Security penetration test =====").text(), Colors.CYAN);
        context.println("", Colors.WHITE);
        context.println(Text.zhEn("尝试各种方法获取 Process 对象...", "Trying various ways to obtain a Process object...").text(), Colors.GRAY);
        context.println("", Colors.WHITE);

        ScriptRunner runner = new ScriptRunner(context.classLoader());
        SandboxConfig config = SandboxConfig.SANDBOX;
        DefaultOutputHandler outputHandler = new DefaultOutputHandler(System.out, System.in);

        int totalTests = 0;
        int passed = 0;

        String[][] attackScripts = {
            {Text.zhEn("1. 直接调用 Runtime.exec()", "1. Call Runtime.exec() directly").text(), 
                "Runtime.getRuntime().exec(\"echo pwned\");"},
            
            {Text.zhEn("2. Runtime.exec() 数组参数", "2. Runtime.exec() with an array argument").text(), 
                "Runtime.getRuntime().exec(new String[]{\"echo\", \"pwned\"});"},
            
            {Text.zhEn("3. Runtime.exec() 带环境变量", "3. Runtime.exec() with environment variables").text(), 
                "Runtime.getRuntime().exec(\"echo pwned\", null);"},
            
            {Text.zhEn("4. Runtime.exec() 带工作目录", "4. Runtime.exec() with a working directory").text(), 
                "Runtime.getRuntime().exec(\"echo pwned\", null, new java.io.File(\"/tmp\"));"},
            
            {Text.zhEn("5. 变量存储后调用", "5. Store in a variable, then call").text(), 
                "Runtime rt = Runtime.getRuntime(); Process p = rt.exec(\"echo pwned\"); p;"},
            
            {Text.zhEn("6. ProcessBuilder 单命令", "6. ProcessBuilder with a single command").text(), 
                "new ProcessBuilder(\"echo\", \"pwned\").start();"},
            
            {Text.zhEn("7. ProcessBuilder 命令列表", "7. ProcessBuilder with a command list").text(), 
                "java.util.List<String> cmd = new java.util.ArrayList<>(); cmd.add(\"echo\"); cmd.add(\"pwned\"); new ProcessBuilder(cmd).start();"},
            
            {"8. ProcessBuilder.directory()", 
                "new ProcessBuilder(\"echo\", \"pwned\").directory(new java.io.File(\"/tmp\")).start();"},
            
            {"9. ProcessBuilder.redirectErrorStream()", 
                "new ProcessBuilder(\"echo\", \"pwned\").redirectErrorStream(true).start();"},
            
            {Text.zhEn("10. ProcessBuilder 数组命令", "10. ProcessBuilder with an array command").text(), 
                "new ProcessBuilder(new String[]{\"echo\", \"pwned\"}).start();"},
            
            {Text.zhEn("11. 反射获取 Runtime 后 exec", "11. Reflect to Runtime, then exec").text(), 
                "Class.forName(\"java.lang.Runtime\").getMethod(\"getRuntime\").invoke(null).getClass().getMethod(\"exec\", String.class).invoke(Class.forName(\"java.lang.Runtime\").getMethod(\"getRuntime\").invoke(null), \"echo pwned\");"},
            
            {Text.zhEn("12. 反射获取 ProcessBuilder", "12. Reflect to ProcessBuilder").text(), 
                "Class<?> pb = Class.forName(\"java.lang.ProcessBuilder\"); java.lang.reflect.Constructor<?> ctor = pb.getConstructor(java.util.List.class); java.util.List<String> cmd = new java.util.ArrayList<>(); cmd.add(\"echo\"); ctor.newInstance(cmd).getClass().getMethod(\"start\").invoke(ctor.newInstance(cmd));"},
            
            {Text.zhEn("13. MethodHandle 调用 Runtime.exec", "13. MethodHandle calling Runtime.exec").text(), 
                "java.lang.invoke.MethodHandle mh = java.lang.invoke.MethodHandles.publicLookup().findVirtual(Runtime.class, \"exec\", java.lang.invoke.MethodType.methodType(Process.class, String.class)); mh.invoke(Runtime.getRuntime(), \"echo pwned\");"},
            
            {Text.zhEn("14. MethodHandle 调用 ProcessBuilder.start", "14. MethodHandle calling ProcessBuilder.start").text(), 
                "java.lang.invoke.MethodHandle mh = java.lang.invoke.MethodHandles.publicLookup().findVirtual(ProcessBuilder.class, \"start\", java.lang.invoke.MethodType.methodType(Process.class)); mh.invoke(new ProcessBuilder(\"echo\", \"pwned\"));"},
            
            {Text.zhEn("15. 通过 getClass 获取 Runtime", "15. Get Runtime via getClass").text(), 
                "Object obj = Runtime.getRuntime(); obj.getClass().getMethod(\"exec\", String.class).invoke(obj, \"echo pwned\");"},
            
            {Text.zhEn("16. 通过 Object 数组存储", "16. Store via an Object array").text(), 
                "Object[] arr = new Object[]{Runtime.getRuntime()}; arr[0].getClass().getMethod(\"exec\", String.class).invoke(arr[0], \"echo pwned\");"},
            
            {Text.zhEn("17. 尝试 /bin/sh -c", "17. Try /bin/sh -c").text(), 
                "Runtime.getRuntime().exec(\"/bin/sh -c 'echo pwned'\");"},
            
            {Text.zhEn("18. 尝试 /system/bin/sh (Android)", "18. Try /system/bin/sh (Android)").text(), 
                "Runtime.getRuntime().exec(\"/system/bin/sh -c 'echo pwned'\");"},
            
            {Text.zhEn("19. ProcessBuilder.command() 链式调用", "19. ProcessBuilder.command() fluent chain").text(), 
                "new ProcessBuilder().command(\"echo\", \"pwned\").start();"},
            
            {Text.zhEn("20. ProcessBuilder 环境变量注入", "20. ProcessBuilder environment injection").text(), 
                "ProcessBuilder pb = new ProcessBuilder(\"echo\", \"pwned\"); pb.environment().put(\"EVIL\", \"1\"); pb.start();"},
            
            {Text.zhEn("21. 反射 getMethod 链", "21. Reflection getMethod chain").text(), 
                "Runtime.class.getMethod(\"getRuntime\").invoke(null).getClass().getMethod(\"exec\", String.class).invoke(Runtime.class.getMethod(\"getRuntime\").invoke(null), \"echo pwned\");"},
            
            {Text.zhEn("22. 通过 ClassLoader 加载后反射", "22. Load via ClassLoader, then reflect").text(), 
                "Class<?> rt = String.class.getClassLoader().loadClass(\"java.lang.Runtime\"); rt.getMethod(\"getRuntime\").invoke(null).getClass().getMethod(\"exec\", String.class).invoke(rt.getMethod(\"getRuntime\").invoke(null), \"echo pwned\");"},
            
            {Text.zhEn("23. 尝试继承 ProcessBuilder", "23. Try extending ProcessBuilder").text(), 
                "new ProcessBuilder(\"echo\", \"pwned\") {}.start();"},
            
            {Text.zhEn("24. 通过 java.lang.ProcessBuilder$Redirect", "24. Via java.lang.ProcessBuilder$Redirect").text(), 
                "new ProcessBuilder(\"echo\", \"pwned\").redirectOutput(ProcessBuilder.Redirect.INHERIT).start();"},
            
            {Text.zhEn("25. 尝试 native exec (通过 JNI)", "25. Try native exec (via JNI)").text(), 
                "Class.forName(\"java.lang.UNIXProcess\");"},
        };

        for (String[] attack : attackScripts) {
            totalTests++;
            String name = attack[0];
            String code = attack[1];

            context.print(name + ": ", Colors.CYAN);

            try {
                runner.setPermissionChecker(config.getAstPermissionChecker());
                runner.clearVariables();
                Object result = BlockGuardSandbox.execute(config, () -> 
                    runner.executeWithResult(code, outputHandler, outputHandler)
                );

                if (result instanceof Process) {
                    context.println(Text.zhEn("⚠ 攻击成功! 获得了 Process 对象", "⚠ Attack succeeded! Obtained a Process object").text(), Colors.RED);
                } else {
                    context.println(Text.zhEn("✓ 被阻止 (返回: %s)", "✓ Blocked (returned: %s)").format(result != null ? result.getClass().getSimpleName() : "null"), Colors.GREEN);
                    passed++;
                }
            } catch (SecurityException e) {
                context.println(Text.zhEn("✓ 被阻止", "✓ Blocked").text(), Colors.GREEN);
                passed++;
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause instanceof SecurityException) {
                    context.println(Text.zhEn("✓ 被阻止", "✓ Blocked").text(), Colors.GREEN);
                    passed++;
                } else if (cause != null && cause.getMessage() != null && 
                           (cause.getMessage().contains("denied") || 
                            cause.getMessage().contains("禁止") ||
                            cause.getMessage().contains("not allowed") ||
                            cause.getMessage().contains("access denied"))) {
                    context.println(Text.zhEn("✓ 被阻止", "✓ Blocked").text(), Colors.GREEN);
                    passed++;
                } else {
                    String msg = cause != null ? cause.getMessage() : e.getMessage();
                    if (msg != null && (msg.contains("denied") || msg.contains("禁止") || 
                        msg.contains("not allowed") || msg.contains("access denied") ||
                        msg.contains("cannot access") || msg.contains("not found") ||
                        msg.contains("cannot resolve") || msg.contains("Unknown class"))) {
                        context.println(Text.zhEn("✓ 被阻止", "✓ Blocked").text(), Colors.GREEN);
                        passed++;
                    } else {
                        context.println(Text.zhEn("⚠ 异常: %s", "⚠ Error: %s").format(cause != null ? "[" + cause.getClass().getSimpleName() + "] "+ cause : e.getClass().getSimpleName()) , Colors.ORANGE);
                    }
                }
            }
        }

        int failed = totalTests - passed;
        context.println("", Colors.WHITE);
        context.println("─────────────────────────────", Colors.GRAY);
        context.println("", Colors.WHITE);
        context.print(Text.zhEn("测试结果: ", "Test result: ").text(), Colors.CYAN);
        context.print(Text.zhEn("%d 个攻击被阻止", "%d attacks blocked").format(passed), passed == totalTests ? Colors.GREEN : Colors.YELLOW);
        context.print(" / ", Colors.GRAY);
        context.println(Text.zhEn("%d 个攻击成功", "%d attacks succeeded").format(failed), failed == 0 ? Colors.GREEN : Colors.RED);

        if (failed == 0) {
            context.println(CONCLUSION_PREFIX.text() + Text.zhEn("沙箱安全机制有效，所有攻击都被阻止", "the sandbox security mechanisms work; every attack was blocked").text(), Colors.GREEN);
        } else if (passed > failed) {
            context.println(CONCLUSION_PREFIX.text() + Text.zhEn("沙箱存在部分漏洞，需要加固", "the sandbox has some holes and needs hardening").text(), Colors.ORANGE);
        } else {
            context.println(CONCLUSION_PREFIX.text() + Text.zhEn("沙箱存在严重安全漏洞！", "the sandbox has a severe security hole!").text(), Colors.RED);
        }
    }

    private native SeccompTestResult testSeccompNative();
    private native CloneForkTestResult testCloneForkBlocking(boolean blockThread, boolean blockProcess);
    private native String strerror(int errno);
}
