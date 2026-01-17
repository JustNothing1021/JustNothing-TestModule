package com.justnothing.methodsclient;

import static com.justnothing.testmodule.constants.CommandClient.CLIENT_VER;

import com.justnothing.methodsclient.executor.FileCommandExecutor;
import com.justnothing.methodsclient.executor.SocketCommandExecutor;
import com.justnothing.methodsclient.monitor.ClientPortManager;
import com.justnothing.methodsclient.monitor.PerformanceMonitor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class StreamClient {



    public static final PrintStream origOut;
    public static final PrintStream origErr;

    public static final PrintStream disabledOut = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) { }
    });

    public static final PrintStream disabledErr = new PrintStream(new OutputStream() {
        @Override
        public void write(int b) { }
    });



    public static class ClientLogger extends Logger {
        @Override
        public String getTag() {
            return "StreamClient";
        }
    }

    public static final ClientLogger logger = new ClientLogger();

    private final ExecutorService executor = Executors.newCachedThreadPool();

    public static boolean checkSocketServer() {
        return ClientPortManager.checkSocketServer();
    }

    private static boolean tryConnect(int port) {
        return ClientPortManager.checkSocketServer();
    }

    public static int getSocketPort() {
        return ClientPortManager.getSocketPort();
    }

    public boolean executeInteractiveSocketCommandB(String command) {
        SocketCommandExecutor executor = new SocketCommandExecutor();
        return executor.executeInteractiveSocketCommand(command);
    }

    public boolean executeSocketCommand(String command) {
        SocketCommandExecutor socketExecutor = new SocketCommandExecutor();
        return socketExecutor.executeSocketCommand(command);
    }

    public static boolean executeFile(String command) {
        return FileCommandExecutor.executeFile(command);
    }

    public static boolean executeSocketOnly(String command) {
        StreamClient client = new StreamClient();
        boolean success = client.executeSocketCommand(command);

        client.executor.shutdown();
        try {
            if (!client.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                client.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            client.executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return success;
    }

    public static boolean executeSocketCommandStatic(String command) {
        if (!checkSocketServer()) {
            logger.warn("Socket服务不可用");
            return false;
        }
        return executeSocketOnly(command);
    }

    public static SocketCommandExecutor.ExecutionResult executeSocketWithOutput(String command) {
        if (!checkSocketServer()) {
            logger.warn("Socket服务不可用");
            return new SocketCommandExecutor.ExecutionResult(false, "", "Socket服务不可用");
        }
        SocketCommandExecutor executor = new SocketCommandExecutor();
        return executor.executeSocketCommandWithOutput(command);
    }

    public static FileCommandExecutor.ExecutionResult executeFileWithOutput(String command) {
        return FileCommandExecutor.executeFileWithOutput(command);
    }

    public boolean executeAutoCommand(String command) {
        if (checkSocketServer()) {
            logger.info("使用Socket模式");
            boolean result = executeSocketCommand(command);
            if (!result) {
                logger.warn("Socket模式失败，回退到文件模式");
                executeFile(command);
            }
        } else {
            logger.warn("Socket服务不可用，使用文件模式");
            executeFile(command);
        }
        return true;
    }

    public static void executeSocket(String command) {
        StreamClient client = new StreamClient();
        boolean success = client.executeSocketCommand(command);

        client.executor.shutdown();
        try {
            if (!client.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                client.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            client.executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (!success) {
            System.exit(1);
        }
    }

    public static void executeAuto(String command) {
        StreamClient client = new StreamClient();
        boolean success = client.executeAutoCommand(command);

        client.executor.shutdown();
        try {
            if (!client.executor.awaitTermination(5, TimeUnit.SECONDS)) {
                client.executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            client.executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        if (!success) {
            System.exit(1);
        }
    }


    public static boolean updateSocketPort(int newPort) {
        return ClientPortManager.updateSocketPort(newPort);
    }

    public static String readOutputFile(File src) {
        return FileCommandExecutor.readOutputFile(src.getAbsolutePath());
    }

    public static boolean requestChmod(String targetPath, String permissions, boolean recursive) {
        return FileCommandExecutor.chmodFile(targetPath, permissions, recursive);
    }

    public static boolean writeHookData(boolean fixPermissions) {
        return FileCommandExecutor.writeHookData(fixPermissions);
    }

    public static String getHelpText() {
        return String.format("""
                JustNothing XposedModule Java Client
                
                用法: StreamClient [options] <command>
                
                通过Java访问服务端来执行methods代码。
                
                可选项:
                    -s, --socket            通过Socket文本协议执行命令
                    -b, --binary            通过Socket二进制交互式协议执行命令
                    -f, --file              通过文件中转命令(原始模式)
                    --update-port <port>    更新Socket服务器端口
                    --auto                  自动选择最佳模式(默认)
                    --check-socket          检查Socket服务器状态
                    --quick-test            快速连接测试
                    --perf-stats            打印性能统计信息
                    --clear-perf-data       清除性能统计数据
                    --help                  显示此帮助信息
                
                示例:
                    StreamClient "invoke java.lang.System currentTimeMillis"
                    StreamClient -j "invoke java.lang.System currentTimeMillis"
                    StreamClient -b "invoke java.lang.System currentTimeMillis"
                    StreamClient --file "invoke java.lang.System currentTimeMillis"
                    StreamClient --update-port 12345
                    StreamClient --check-socket
                    StreamClient --perf-stats
                    StreamClient --clear-perf-data
                
                (JavaClient %s)
                """, CLIENT_VER);
    }

    static {
        origOut = System.out;
        origErr = System.err;
        System.setOut(disabledOut);
        System.setErr(disabledErr);
    }

    /**
     * 主方法，通过app_process执行。
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        FileCommandExecutor.isInAppProcess = true;
        
        System.setOut(origOut);
        System.setErr(origErr);
        if (args.length > 0 && args[0].equals("--perf-stats")) {
            PerformanceMonitor.printStats();
            return;
        }

        if (args.length > 0 && args[0].equals("--clear-perf-data")) {
            PerformanceMonitor.clearStats();
            System.err.println("性能统计数据已清除");
            return;
        }

        if (args.length > 0 && args[0].equals("--quick-test")) {
            boolean result = tryConnect(getSocketPort());
            System.exit(result ? 0 : 1);
            return;
        }

        if (args.length > 0 && args[0].equals("--update-port")) {
            if (args.length < 2) {
                System.err.println("错误: 需要提供端口号");
                System.exit(1);
            }
            try {
                int port = Integer.parseInt(args[1]);
                boolean success = updateSocketPort(port);
                System.exit(success ? 0 : 1);
            } catch (NumberFormatException e) {
                System.err.println("错误: 端口号必须是数字");
                System.exit(1);
            }
            return;
        }

        if (args.length == 0) {
            System.err.println(getHelpText());
            return;
        }

        String firstArg = args[0];

        switch (firstArg) {
            case "--check-socket" -> {
                boolean running = checkSocketServer();
                System.err.println("Socket模式服务器状态: " + (running ? "运行中" : "未运行"));
                System.exit(running ? 0 : 1);
            }
            case "--help" -> System.err.println(getHelpText());
            case "--socket", "-s" -> {
                if (args.length < 2) {
                    System.err.println("错误: 需要提供命令");
                    System.exit(1);
                }
                String command = joinArgs(args, 1);
                executeSocket(command);
            }
            case "--binary", "-b" -> {
                if (args.length < 2) {
                    System.err.println("错误: 需要提供命令");
                    System.exit(1);
                }
                String command = joinArgs(args, 1);
                boolean success = new StreamClient().executeInteractiveSocketCommandB(command);
                System.exit(success ? 0 : 1);
            }
            case "--auto" -> {
                if (args.length < 2) {
                    System.err.println("错误: 需要提供命令");
                    System.exit(1);
                }
                String command = joinArgs(args, 1);
                executeAuto(command);
            }
            case "--file", "-f" -> {
                if (args.length < 2) {
                    System.err.println("错误: 需要提供命令");
                    System.exit(1);
                }
                String command = joinArgs(args, 1);
                boolean success = executeFile(command);
                if (!success) {
                    System.exit(1);
                }
            }

            default -> {
                String command = joinArgs(args, 0);
                executeAuto(command);
            }
        }
    }

    private static String joinArgs(String[] args, int start) {
        StringBuilder sb = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(args[i]);
        }
        return sb.toString();
    }
}
