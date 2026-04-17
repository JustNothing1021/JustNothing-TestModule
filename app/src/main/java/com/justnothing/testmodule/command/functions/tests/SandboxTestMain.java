package com.justnothing.testmodule.command.functions.tests;

import com.justnothing.javainterpreter.ScriptRunner;
import com.justnothing.javainterpreter.api.DefaultOutputHandler;
import com.justnothing.javainterpreter.evaluator.ExecutionContext;
import com.justnothing.javainterpreter.security.SandboxConfig;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;
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

public class SandboxTestMain extends CommandBase {

    private static volatile boolean nativeLoaded = false;
    private static volatile Throwable nativeLoadError = null;
    private static volatile String nativeLibPath = null;

    public SandboxTestMain() {
        super("SandboxTest");
    }

    @Override
    public String getHelpText() {
        return """
                ===== 沙箱测试命令 =====
                
                用法: sandboxtest <子命令>
                
                子命令:
                    load             - 加载 native 库
                    blockguard       - 测试 BlockGuard I/O 拦截
                    seccomp          - 测试 seccomp-bpf 进程拦截
                    clonefork        - 测试 clone/fork 拦截
                    penetration      - 安全渗透测试（尝试攻破沙箱）
                    all              - 运行所有测试
                    info             - 显示环境信息
                
                说明:
                    此命令用于测试 Android 沙箱机制的可用性。
                    - BlockGuard: 拦截磁盘 I/O 和网络操作
                    - seccomp-bpf: 拦截进程创建和线程创建
                    - penetration: 尝试各种方法绕过安全限制
                    
                相关命令:
                    anonclasstest    - 匿名类生成诊断 (独立工具)
                
                """;
    }

    @Override
    public void runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        if (args.length < 1) {
            context.println(getHelpText());
            return;
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
                context.print("未知子命令: ", Colors.RED);
                context.println(subCommand, Colors.YELLOW);
                context.println(getHelpText());
            }
        }
    }

    private interface TestRunnable {
        void run() throws Exception;
    }

    private void executeInIsolatedThread(CommandExecutor.CmdExecContext context, String testName, TestRunnable test) {
        context.println("[" + testName + " 测试] 在独立线程中执行...", Colors.CYAN);
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
                context.print("测试过程中发生异常: ", Colors.RED);
                context.println(e.getMessage(), Colors.ORANGE);
                context.output().printStackTrace(e);
            } else if (!completed.get()) {
                context.println("测试未完成（未知状态）", Colors.ORANGE);
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            context.println("测试超时（60秒），已取消", Colors.RED);
        } catch (Exception e) {
            context.print("等待测试结果时发生异常: ", Colors.RED);
            context.println(e.getMessage(), Colors.ORANGE);
        }
    }

    private void showEnvironmentInfo(CommandExecutor.CmdExecContext context) {
        context.println("===== 环境信息 =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        
        context.print("进程 ID: ", Colors.CYAN);
        context.println(String.valueOf(android.os.Process.myPid()), Colors.WHITE);
        
        context.print("用户 ID: ", Colors.CYAN);
        context.println(String.valueOf(android.os.Process.myUid()), Colors.WHITE);
        
        context.println("", Colors.WHITE);
        context.print("Native 库状态: ", Colors.CYAN);
        if (nativeLoaded) {
            context.println("已加载 (" + nativeLibPath + ")", Colors.GREEN);
        } else if (nativeLoadError != null) {
            context.println("加载失败", Colors.RED);
        } else {
            context.println("未加载", Colors.GRAY);
        }
        
        context.println("", Colors.WHITE);
        context.print("模块路径: ", Colors.CYAN);
        String modulePath = DataBridge.getModulePath();
        context.println(modulePath != null ? modulePath : "未知", Colors.GRAY);
        
        context.println("", Colors.WHITE);
        context.print("BlockGuard: ", Colors.CYAN);
        context.println(BlockGuardSandbox.isBlockGuardAvailable() ? "可用" : "不可用", 
                BlockGuardSandbox.isBlockGuardAvailable() ? Colors.GREEN : Colors.RED);
    }

    private void loadNativeLibrary(CommandExecutor.CmdExecContext context) {
        if (nativeLoaded) {
            context.println("Native 库已加载", Colors.GREEN);
            context.println("路径: " + nativeLibPath, Colors.GRAY);
            return;
        }

        if (nativeLoadError != null) {
            context.println("Native 库加载已失败，不再重试", Colors.RED);
            context.println("错误: " + nativeLoadError.getMessage(), Colors.GRAY);
            return;
        }

        synchronized (SandboxTestMain.class) {
            if (nativeLoaded) {
                context.println("Native 库已加载", Colors.GREEN);
                return;
            }

            try {
                String apkPath = DataBridge.getModulePath();
                if (apkPath == null) {
                    throw new RuntimeException("无法获取模块 APK 路径");
                }
                context.println("模块 APK: " + apkPath, Colors.GRAY);

                String abi = getAbi();
                context.println("当前 ABI: " + abi, Colors.GRAY);

                File libFile = extractNativeLib(apkPath, abi, context);
                if (libFile == null) {
                    throw new RuntimeException("无法从 APK 提取 native 库");
                }

                context.println("提取到: " + libFile.getAbsolutePath(), Colors.GRAY);

                System.load(libFile.getAbsolutePath());
                nativeLibPath = libFile.getAbsolutePath();
                nativeLoaded = true;

                context.println("Native 库加载成功!", Colors.GREEN);
            } catch (Throwable e) {
                nativeLoadError = e;
                context.println("Native 库加载失败", Colors.RED);
                context.println("错误: " + e.getMessage(), Colors.GRAY);
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
                context.println("APK 中未找到: " + entryName, Colors.RED);
                for (String supportedAbi : android.os.Build.SUPPORTED_ABIS) {
                    String altEntry = "lib/" + supportedAbi + "/" + libName;
                    entry = zipFile.getEntry(altEntry);
                    if (entry != null) {
                        entryName = altEntry;
                        context.println("使用备用 ABI: " + supportedAbi, Colors.GRAY);
                        break;
                    }
                }
            }

            if (entry == null) {
                context.println("APK 中未找到任何 native 库", Colors.RED);
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
                context.println("警告: 无法设置可执行权限", Colors.ORANGE);
            }

            return libFile;
        } catch (Exception e) {
            context.println("提取失败: " + e.getMessage(), Colors.RED);
            return null;
        }
    }

    private void testBlockGuardInternal(CommandExecutor.CmdExecContext context) {
        context.println("===== 测试 BlockGuard I/O 拦截 =====", Colors.CYAN);
        context.println("", Colors.WHITE);

        boolean available = BlockGuardSandbox.isBlockGuardAvailable();
        context.print("BlockGuard 可用性: ", Colors.CYAN);
        context.println(available ? "可用" : "不可用", available ? Colors.GREEN : Colors.RED);

        if (!available) {
            context.println("", Colors.WHITE);
            context.println("结论: BlockGuard 不可用（非 Android 环境？）", Colors.RED);
            return;
        }

        File testFile = new File(DataBridge.getDataDir(), "sandbox_test_" + System.currentTimeMillis() + ".tmp");
        
        BlockGuardSandbox.bypass(() -> {
            try {
                java.io.FileOutputStream fos = new java.io.FileOutputStream(testFile);
                fos.write("test".getBytes());
                fos.close();
            } catch (Exception e) {
                context.println("警告: 无法创建测试文件: " + e.getMessage(), Colors.ORANGE);
            }
        });
        
        context.println("", Colors.WHITE);
        context.println("测试磁盘写入拦截...", Colors.CYAN);
        context.println("测试文件: " + testFile.getAbsolutePath(), Colors.GRAY);
        
        boolean[] writeBlocked = {false};
        String[] writeError = {null};
        
        try {
            SandboxConfig writeConfig = SandboxConfig.builder()
                    .allowDiskRead()
                    .denyDiskWrite()
                    .allowNetwork()
                    .allowLocalSocket()
                    .allowThreadCreate()
                    .allowThreadModify()
                    .allowProcessCreate()
                    .build();
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

        context.print("磁盘写入拦截: ", Colors.CYAN);
        context.println(writeBlocked[0] ? "已拦截 ✓" : "未拦截 ✗", writeBlocked[0] ? Colors.GREEN : Colors.RED);
        if (writeError[0] != null) {
            context.println("  信息: " + writeError[0], Colors.GRAY);
        }
        
        context.println("", Colors.WHITE);
        context.println("测试磁盘读取拦截...", Colors.CYAN);
        
        boolean[] readBlocked = {false};
        String[] readError = {null};
        
        try {
            SandboxConfig readConfig = SandboxConfig.builder()
                    .denyDiskRead()
                    .allowDiskWrite()
                    .allowNetwork()
                    .allowLocalSocket()
                    .allowThreadCreate()
                    .allowThreadModify()
                    .allowProcessCreate()
                    .build();
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

        context.print("磁盘读取拦截: ", Colors.CYAN);
        context.println(readBlocked[0] ? "已拦截 ✓" : "未拦截 ✗", readBlocked[0] ? Colors.GREEN : Colors.RED);
        if (readError[0] != null) {
            context.println("  信息: " + readError[0], Colors.GRAY);
        }

        BlockGuardSandbox.bypass(() -> {
            if (testFile.exists()) {
                testFile.delete();
            }
        });

        context.println("", Colors.WHITE);
        if (writeBlocked[0] && readBlocked[0]) {
            context.println("结论: BlockGuard I/O 拦截正常工作", Colors.GREEN);
        } else if (writeBlocked[0] || readBlocked[0]) {
            context.println("结论: BlockGuard 部分工作", Colors.ORANGE);
        } else {
            context.println("结论: BlockGuard 未拦截任何操作", Colors.RED);
            context.println("提示: 可能需要检查 BlockGuard 策略设置", Colors.GRAY);
        }
    }

    private void testSeccompInternal(CommandExecutor.CmdExecContext context) {
        if (!ensureNativeLoaded(context)) {
            return;
        }

        context.println("===== 测试 seccomp-bpf 进程拦截 =====", Colors.CYAN);
        context.println("", Colors.WHITE);

        try {
            SeccompTestResult result = testSeccompNative();

            context.print("prctl(PR_SET_NO_NEW_PRIVS): ", Colors.CYAN);
            context.println(result.prctlSuccess ? "成功" : "失败", result.prctlSuccess ? Colors.GREEN : Colors.RED);
            if (result.prctlError != 0) {
                context.println("  错误码: " + result.prctlError + " (" + strerror(result.prctlError) + ")", Colors.GRAY);
            }

            context.print("seccomp(SECCOMP_SET_MODE_FILTER): ", Colors.CYAN);
            context.println(result.seccompSuccess ? "成功" : "失败", result.seccompSuccess ? Colors.GREEN : Colors.RED);
            if (result.seccompError != 0) {
                context.println("  错误码: " + result.seccompError + " (" + strerror(result.seccompError) + ")", Colors.GRAY);
            }

            context.print("execve 拦截测试: ", Colors.CYAN);
            context.println(result.execveBlocked ? "已拦截 ✓" : "未拦截 ✗", result.execveBlocked ? Colors.GREEN : Colors.RED);

            context.println("", Colors.WHITE);
            if (result.seccompSuccess && result.execveBlocked) {
                context.println("结论: seccomp-bpf 可用于进程创建拦截", Colors.GREEN);
            } else if (result.prctlSuccess && !result.seccompSuccess) {
                context.println("结论: prctl 可用但 seccomp 被阻止，可能是 SELinux 限制", Colors.ORANGE);
            } else {
                context.println("结论: seccomp-bpf 不可用", Colors.RED);
            }
        } catch (Throwable e) {
            context.println("测试失败: " + e.getMessage(), Colors.RED);
        }
    }

    private void testCloneForkInternal(CommandExecutor.CmdExecContext context) {
        if (!ensureNativeLoaded(context)) {
            return;
        }

        context.println("===== 测试 clone/fork 拦截 =====", Colors.CYAN);
        context.println("", Colors.WHITE);

        context.println("测试场景: 同时拦截线程和进程创建", Colors.CYAN);
        context.println("", Colors.WHITE);

        try {
            CloneForkTestResult result = testCloneForkBlocking(true, true);

            context.print("seccomp 过滤器安装: ", Colors.CYAN);
            context.println(result.seccompSuccess ? "成功" : "失败", result.seccompSuccess ? Colors.GREEN : Colors.RED);

            if (!result.seccompSuccess) {
                context.print("错误信息: ", Colors.RED);
                context.println(result.errorMsg != null ? result.errorMsg : "未知错误", Colors.GRAY);
                return;
            }

            if (result.errorMsg != null) {
                context.print("警告: ", Colors.ORANGE);
                context.println(result.errorMsg, Colors.GRAY);
            }

            context.println("", Colors.WHITE);
            
            context.print("fork() 拦截: ", Colors.CYAN);
            context.println(result.forkBlocked ? "已拦截 ✓" : "未拦截 ✗", result.forkBlocked ? Colors.GREEN : Colors.RED);

            context.print("pthread_create() 拦截: ", Colors.CYAN);
            context.println(result.threadBlocked ? "已拦截 ✓" : "未拦截 ✗", result.threadBlocked ? Colors.GREEN : Colors.RED);

            context.println("", Colors.WHITE);
            
            if (result.forkBlocked && result.threadBlocked) {
                context.println("结论: seccomp-bpf 可完全拦截线程和进程创建", Colors.GREEN);
            } else if (result.errorMsg != null) {
                context.println("结论: 测试未完成，子进程在测试期间崩溃", Colors.ORANGE);
                context.println("提示: 可能是 BPF 过滤器配置问题或设备兼容性问题", Colors.GRAY);
            } else if (result.forkBlocked || result.threadBlocked) {
                context.println("结论: seccomp-bpf 部分有效，需要进一步调试", Colors.ORANGE);
            } else {
                context.println("结论: seccomp-bpf 未拦截任何操作", Colors.RED);
            }

            context.println("", Colors.WHITE);
            context.println("技术说明:", Colors.CYAN);
            context.println("  - clone() 系统调用通过 CLONE_VM 标志区分线程/进程", Colors.GRAY);
            context.println("  - fork()/vfork() 是进程创建的传统方式", Colors.GRAY);
            context.println("  - pthread_create() 内部使用 clone() + CLONE_VM", Colors.GRAY);

        } catch (Throwable e) {
            context.println("测试失败: " + e.getMessage(), Colors.RED);
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean ensureNativeLoaded(CommandExecutor.CmdExecContext context) {
        if (!nativeLoaded) {
            context.println("Native 库未加载，正在加载...", Colors.ORANGE);
            BlockGuardSandbox.bypass(() -> loadNativeLibrary(context));
        }

        if (!nativeLoaded) {
            context.println("错误: Native 库未加载", Colors.RED);
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
        context.println("===== 安全渗透测试 =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        context.println("尝试各种方法获取 Process 对象...", Colors.GRAY);
        context.println("", Colors.WHITE);

        ScriptRunner runner = new ScriptRunner(context.classLoader());
        SandboxConfig config = SandboxConfig.SANDBOX;
        DefaultOutputHandler outputHandler = new DefaultOutputHandler(System.out, System.in);

        int totalTests = 0;
        int passed = 0;

        String[][] attackScripts = {
            {"1. 直接调用 Runtime.exec()", 
                "Runtime.getRuntime().exec(\"echo pwned\");"},
            
            {"2. Runtime.exec() 数组参数", 
                "Runtime.getRuntime().exec(new String[]{\"echo\", \"pwned\"});"},
            
            {"3. Runtime.exec() 带环境变量", 
                "Runtime.getRuntime().exec(\"echo pwned\", null);"},
            
            {"4. Runtime.exec() 带工作目录", 
                "Runtime.getRuntime().exec(\"echo pwned\", null, new java.io.File(\"/tmp\"));"},
            
            {"5. 变量存储后调用", 
                "Runtime rt = Runtime.getRuntime(); Process p = rt.exec(\"echo pwned\"); p;"},
            
            {"6. ProcessBuilder 单命令", 
                "new ProcessBuilder(\"echo\", \"pwned\").start();"},
            
            {"7. ProcessBuilder 命令列表", 
                "java.util.List<String> cmd = new java.util.ArrayList<>(); cmd.add(\"echo\"); cmd.add(\"pwned\"); new ProcessBuilder(cmd).start();"},
            
            {"8. ProcessBuilder.directory()", 
                "new ProcessBuilder(\"echo\", \"pwned\").directory(new java.io.File(\"/tmp\")).start();"},
            
            {"9. ProcessBuilder.redirectErrorStream()", 
                "new ProcessBuilder(\"echo\", \"pwned\").redirectErrorStream(true).start();"},
            
            {"10. ProcessBuilder 数组命令", 
                "new ProcessBuilder(new String[]{\"echo\", \"pwned\"}).start();"},
            
            {"11. 反射获取 Runtime 后 exec", 
                "Class.forName(\"java.lang.Runtime\").getMethod(\"getRuntime\").invoke(null).getClass().getMethod(\"exec\", String.class).invoke(Class.forName(\"java.lang.Runtime\").getMethod(\"getRuntime\").invoke(null), \"echo pwned\");"},
            
            {"12. 反射获取 ProcessBuilder", 
                "Class<?> pb = Class.forName(\"java.lang.ProcessBuilder\"); java.lang.reflect.Constructor<?> ctor = pb.getConstructor(java.util.List.class); java.util.List<String> cmd = new java.util.ArrayList<>(); cmd.add(\"echo\"); ctor.newInstance(cmd).getClass().getMethod(\"start\").invoke(ctor.newInstance(cmd));"},
            
            {"13. MethodHandle 调用 Runtime.exec", 
                "java.lang.invoke.MethodHandle mh = java.lang.invoke.MethodHandles.publicLookup().findVirtual(Runtime.class, \"exec\", java.lang.invoke.MethodType.methodType(Process.class, String.class)); mh.invoke(Runtime.getRuntime(), \"echo pwned\");"},
            
            {"14. MethodHandle 调用 ProcessBuilder.start", 
                "java.lang.invoke.MethodHandle mh = java.lang.invoke.MethodHandles.publicLookup().findVirtual(ProcessBuilder.class, \"start\", java.lang.invoke.MethodType.methodType(Process.class)); mh.invoke(new ProcessBuilder(\"echo\", \"pwned\"));"},
            
            {"15. 通过 getClass 获取 Runtime", 
                "Object obj = Runtime.getRuntime(); obj.getClass().getMethod(\"exec\", String.class).invoke(obj, \"echo pwned\");"},
            
            {"16. 通过 Object 数组存储", 
                "Object[] arr = new Object[]{Runtime.getRuntime()}; arr[0].getClass().getMethod(\"exec\", String.class).invoke(arr[0], \"echo pwned\");"},
            
            {"17. 尝试 /bin/sh -c", 
                "Runtime.getRuntime().exec(\"/bin/sh -c 'echo pwned'\");"},
            
            {"18. 尝试 /system/bin/sh (Android)", 
                "Runtime.getRuntime().exec(\"/system/bin/sh -c 'echo pwned'\");"},
            
            {"19. ProcessBuilder.command() 链式调用", 
                "new ProcessBuilder().command(\"echo\", \"pwned\").start();"},
            
            {"20. ProcessBuilder 环境变量注入", 
                "ProcessBuilder pb = new ProcessBuilder(\"echo\", \"pwned\"); pb.environment().put(\"EVIL\", \"1\"); pb.start();"},
            
            {"21. 反射 getMethod 链", 
                "Runtime.class.getMethod(\"getRuntime\").invoke(null).getClass().getMethod(\"exec\", String.class).invoke(Runtime.class.getMethod(\"getRuntime\").invoke(null), \"echo pwned\");"},
            
            {"22. 通过 ClassLoader 加载后反射", 
                "Class<?> rt = String.class.getClassLoader().loadClass(\"java.lang.Runtime\"); rt.getMethod(\"getRuntime\").invoke(null).getClass().getMethod(\"exec\", String.class).invoke(rt.getMethod(\"getRuntime\").invoke(null), \"echo pwned\");"},
            
            {"23. 尝试继承 ProcessBuilder", 
                "new ProcessBuilder(\"echo\", \"pwned\") {}.start();"},
            
            {"24. 通过 java.lang.ProcessBuilder$Redirect", 
                "new ProcessBuilder(\"echo\", \"pwned\").redirectOutput(ProcessBuilder.Redirect.INHERIT).start();"},
            
            {"25. 尝试 native exec (通过 JNI)", 
                "Class.forName(\"java.lang.UNIXProcess\");"},
        };

        for (String[] attack : attackScripts) {
            totalTests++;
            String name = attack[0];
            String code = attack[1];

            context.print(name + ": ", Colors.CYAN);

            try {
                runner.getExecutionContext().setPermissionChecker(config.getAstPermissionChecker());
                runner.getExecutionContext().clearVariables();
                Object result = BlockGuardSandbox.execute(config, () -> 
                    runner.executeWithResult(code, outputHandler, outputHandler)
                );

                if (result instanceof Process) {
                    context.println("⚠ 攻击成功! 获得了 Process 对象", Colors.RED);
                } else {
                    context.println("✓ 被阻止 (返回: " + (result != null ? result.getClass().getSimpleName() : "null") + ")", Colors.GREEN);
                    passed++;
                }
            } catch (SecurityException e) {
                context.println("✓ 被阻止", Colors.GREEN);
                passed++;
            } catch (Exception e) {
                Throwable cause = e.getCause();
                if (cause instanceof SecurityException) {
                    context.println("✓ 被阻止", Colors.GREEN);
                    passed++;
                } else if (cause != null && cause.getMessage() != null && 
                           (cause.getMessage().contains("denied") || 
                            cause.getMessage().contains("禁止") ||
                            cause.getMessage().contains("not allowed") ||
                            cause.getMessage().contains("access denied"))) {
                    context.println("✓ 被阻止", Colors.GREEN);
                    passed++;
                } else {
                    String msg = cause != null ? cause.getMessage() : e.getMessage();
                    if (msg != null && (msg.contains("denied") || msg.contains("禁止") || 
                        msg.contains("not allowed") || msg.contains("access denied") ||
                        msg.contains("cannot access") || msg.contains("not found") ||
                        msg.contains("cannot resolve") || msg.contains("Unknown class"))) {
                        context.println("✓ 被阻止", Colors.GREEN);
                        passed++;
                    } else {
                        context.println("⚠ 异常: " + (cause != null ? "[" + cause.getClass().getSimpleName() + "] "+ cause : e.getClass().getSimpleName()) , Colors.ORANGE);
                    }
                }
            }
        }

        int failed = totalTests - passed;
        context.println("", Colors.WHITE);
        context.println("─────────────────────────────", Colors.GRAY);
        context.println("", Colors.WHITE);
        context.print("测试结果: ", Colors.CYAN);
        context.print(passed + " 个攻击被阻止", passed == totalTests ? Colors.GREEN : Colors.YELLOW);
        context.print(" / ", Colors.GRAY);
        context.println(failed + " 个攻击成功", failed == 0 ? Colors.GREEN : Colors.RED);

        if (failed == 0) {
            context.println("结论: 沙箱安全机制有效，所有攻击都被阻止", Colors.GREEN);
        } else if (passed > failed) {
            context.println("结论: 沙箱存在部分漏洞，需要加固", Colors.ORANGE);
        } else {
            context.println("结论: 沙箱存在严重安全漏洞！", Colors.RED);
        }
    }

    private native SeccompTestResult testSeccompNative();
    private native CloneForkTestResult testCloneForkBlocking(boolean blockThread, boolean blockProcess);
    private native String strerror(int errno);
}
