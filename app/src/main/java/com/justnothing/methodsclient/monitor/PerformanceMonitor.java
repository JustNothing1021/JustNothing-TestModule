package com.justnothing.methodsclient.monitor;

import static com.justnothing.testmodule.constants.CommandClient.PERFORMANCE_DATA_FILE;

import com.justnothing.methodsclient.StreamClient;
import com.justnothing.testmodule.utils.functions.CmdUtils;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMonitor {

    private static final AtomicLong totalCommands = new AtomicLong(0);
    private static final AtomicLong commandsCompleted = new AtomicLong(0);
    private static final AtomicLong commandsFailed = new AtomicLong(0);

    private static final AtomicLong socketCommands = new AtomicLong(0);
    private static final AtomicLong socketCommandsCompleted = new AtomicLong(0);
    private static final AtomicLong socketCommandsFailed = new AtomicLong(0);
    private static final AtomicLong totalSocketTime = new AtomicLong(0);
    private static final AtomicLong fastestSocketTime = new AtomicLong(Long.MAX_VALUE);
    private static final AtomicLong slowestSocketTime = new AtomicLong(0);
    private static final AtomicLong totalBytesRead = new AtomicLong(0);
    private static final AtomicLong totalCharsRead = new AtomicLong(0);
    private static final AtomicLong totalConnections = new AtomicLong(0);

    private static final AtomicLong fileCommands = new AtomicLong(0);
    private static final AtomicLong fileCommandsCompleted = new AtomicLong(0);
    private static final AtomicLong fileCommandsFailed = new AtomicLong(0);
    private static final AtomicLong totalFileTime = new AtomicLong(0);
    private static final AtomicLong fastestFileTime = new AtomicLong(Long.MAX_VALUE);
    private static final AtomicLong slowestFileTime = new AtomicLong(0);


    private static final StreamClient.ClientLogger logger = new StreamClient.ClientLogger();

    private static volatile boolean statsLoaded = false;

    private static void ensureStatsLoaded() {
        if (!statsLoaded) {
            synchronized (PerformanceMonitor.class) {
                if (!statsLoaded) {
                    loadStats();
                    statsLoaded = true;
                }
            }
        }
    }

    private static void loadStats() {
        File file = new File(PERFORMANCE_DATA_FILE);
        if (!file.exists()) {
            return;
        }

        try {
            String content = IOManager.readFile(file.getAbsolutePath());
            if (content != null) {
                String[] lines = content.split("\n");
                for (String line : lines) {
                    String[] parts = line.split("=");
                    if (parts.length == 2) {
                        String key = parts[0].trim();
                        long value = Long.parseLong(parts[1].trim());

                        switch (key) {
                            case "totalCommands":
                                totalCommands.set(value);
                                break;
                            case "commandsCompleted":
                                commandsCompleted.set(value);
                                break;
                            case "commandsFailed":
                                commandsFailed.set(value);
                                break;
                            case "socketCommands":
                                socketCommands.set(value);
                                break;
                            case "socketCommandsCompleted":
                                socketCommandsCompleted.set(value);
                                break;
                            case "socketCommandsFailed":
                                socketCommandsFailed.set(value);
                                break;
                            case "totalSocketTime":
                                totalSocketTime.set(value);
                                break;
                            case "fastestSocketTime":
                                fastestSocketTime.set(value);
                                break;
                            case "slowestSocketTime":
                                slowestSocketTime.set(value);
                                break;
                            case "totalBytesRead":
                                totalBytesRead.set(value);
                                break;
                            case "totalCharsRead":
                                totalCharsRead.set(value);
                                break;
                            case "totalConnections":
                                totalConnections.set(value);
                                break;
                            case "fileCommands":
                                fileCommands.set(value);
                                break;
                            case "fileCommandsCompleted":
                                fileCommandsCompleted.set(value);
                                break;
                            case "fileCommandsFailed":
                                fileCommandsFailed.set(value);
                                break;
                            case "totalFileTime":
                                totalFileTime.set(value);
                                break;
                            case "fastestFileTime":
                                fastestFileTime.set(value);
                                break;
                            case "slowestFileTime":
                                slowestFileTime.set(value);
                                break;
                        }
                    }
                }
            }
        } catch (IOException | NumberFormatException e) {
            logger.warn("加载性能统计数据失败: " + e.getMessage());
        }
    }

    private static void saveStats() {
        File dir = new File(Objects.requireNonNull(new File(PERFORMANCE_DATA_FILE).getParent()));
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                logger.warn("无法创建性能统计目录: " + dir.getAbsolutePath());
                return;
            }
            try {
                CmdUtils.CommandOutput chmodResult = CmdUtils.runRootCommand("chmod 777 " + dir.getAbsolutePath(), 5000);
                if (!chmodResult.succeed()) {
                    logger.warn("设置性能统计目录权限失败: " + dir.getAbsolutePath());
                }
            } catch (Exception e) {
                logger.warn("设置性能统计目录权限异常: " + dir.getAbsolutePath() + ", " + e.getMessage());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append("totalCommands=").append(totalCommands.get()).append("\n");
        sb.append("commandsCompleted=").append(commandsCompleted.get()).append("\n");
        sb.append("commandsFailed=").append(commandsFailed.get()).append("\n");
        sb.append("socketCommands=").append(socketCommands.get()).append("\n");
        sb.append("socketCommandsCompleted=").append(socketCommandsCompleted.get()).append("\n");
        sb.append("socketCommandsFailed=").append(socketCommandsFailed.get()).append("\n");
        sb.append("totalSocketTime=").append(totalSocketTime.get()).append("\n");
        sb.append("fastestSocketTime=").append(fastestSocketTime.get()).append("\n");
        sb.append("slowestSocketTime=").append(slowestSocketTime.get()).append("\n");
        sb.append("totalBytesRead=").append(totalBytesRead.get()).append("\n");
        sb.append("totalCharsRead=").append(totalCharsRead.get()).append("\n");
        sb.append("totalConnections=").append(totalConnections.get()).append("\n");
        sb.append("fileCommands=").append(fileCommands.get()).append("\n");
        sb.append("fileCommandsCompleted=").append(fileCommandsCompleted.get()).append("\n");
        sb.append("fileCommandsFailed=").append(fileCommandsFailed.get()).append("\n");
        sb.append("totalFileTime=").append(totalFileTime.get()).append("\n");
        sb.append("fastestFileTime=").append(fastestFileTime.get()).append("\n");
        sb.append("slowestFileTime=").append(slowestFileTime.get());

        try {
            IOManager.writeFile(PERFORMANCE_DATA_FILE, sb.toString());
        } catch (IOException e) {
            logger.warn("保存性能统计数据失败: " + e.getMessage());
        }
    }

    public static void recordSocketCommand(long duration, long bytesRead, long charsRead, boolean success) {
        ensureStatsLoaded();
        totalCommands.incrementAndGet();
        socketCommands.incrementAndGet();
        if (success) {
            commandsCompleted.incrementAndGet();
            socketCommandsCompleted.incrementAndGet();
            totalSocketTime.addAndGet(duration);
            totalBytesRead.addAndGet(bytesRead);
            totalCharsRead.addAndGet(charsRead);

            long currentFastest;
            do {
                currentFastest = fastestSocketTime.get();
                if (duration >= currentFastest) break;
            } while (!fastestSocketTime.compareAndSet(currentFastest, duration));

            long currentSlowest;
            do {
                currentSlowest = slowestSocketTime.get();
                if (duration <= currentSlowest) break;
            } while (!slowestSocketTime.compareAndSet(currentSlowest, duration));
        } else {
            commandsFailed.incrementAndGet();
            socketCommandsFailed.incrementAndGet();
        }
        saveStats();
    }

    public static void recordFileCommand(long duration, boolean success) {
        ensureStatsLoaded();
        totalCommands.incrementAndGet();
        fileCommands.incrementAndGet();
        if (success) {
            commandsCompleted.incrementAndGet();
            fileCommandsCompleted.incrementAndGet();
            totalFileTime.addAndGet(duration);

            long currentFastest;
            do {
                currentFastest = fastestFileTime.get();
                if (duration >= currentFastest) break;
            } while (!fastestFileTime.compareAndSet(currentFastest, duration));

            long currentSlowest;
            do {
                currentSlowest = slowestFileTime.get();
                if (duration <= currentSlowest) break;
            } while (!slowestFileTime.compareAndSet(currentSlowest, duration));
        } else {
            commandsFailed.incrementAndGet();
            fileCommandsFailed.incrementAndGet();
        }
        saveStats();
    }

    public static void clearStats() {
        totalCommands.set(0);
        commandsCompleted.set(0);
        commandsFailed.set(0);

        socketCommands.set(0);
        socketCommandsCompleted.set(0);
        socketCommandsFailed.set(0);
        totalSocketTime.set(0);
        fastestSocketTime.set(Long.MAX_VALUE);
        slowestSocketTime.set(0);
        totalBytesRead.set(0);
        totalCharsRead.set(0);
        totalConnections.set(0);

        fileCommands.set(0);
        fileCommandsCompleted.set(0);
        fileCommandsFailed.set(0);
        totalFileTime.set(0);
        fastestFileTime.set(Long.MAX_VALUE);
        slowestFileTime.set(0);

        File file = new File(PERFORMANCE_DATA_FILE);
        if (file.exists()) {
            if (!file.delete()) logger.warn("性能统计文件删除失败");
            logger.info("性能统计数据已清除");
        }
    }

    public static void printStats() {
        ensureStatsLoaded();
        long commands = totalCommands.get();
        if (commands > 0) {
            logger.info("\n============ 性能统计 ============");
            logger.info("总命令数: " + commands);
            logger.info("成功: " + commandsCompleted.get() +
                    ", 失败: " + commandsFailed.get());
            logger.info("成功率: " +
                    String.format(Locale.getDefault(),
                             "%.1f%%", commandsCompleted.get() * 100.0 / commands));
            logger.info("总连接数: " + totalConnections.get());

            long socketCmd = socketCommands.get();
            if (socketCmd > 0) {
                logger.info("Socket模式:");
                logger.info("   命令数: " + socketCmd);
                logger.info("   成功: " + socketCommandsCompleted.get() +
                        ", 失败: " + socketCommandsFailed.get());
                logger.info("   总耗时: " + totalSocketTime.get() + "ms");
                logger.info("   平均耗时: " + (totalSocketTime.get() / socketCmd) + "ms");
                logger.info("   最快耗时: " +
                        (fastestSocketTime.get() == Long.MAX_VALUE ? 0 : fastestSocketTime.get()) + "ms");
                logger.info("   最慢耗时: " + slowestSocketTime.get() + "ms");
                logger.info("   总读取字节: " + totalBytesRead.get());
                logger.info("   总读取字符: " + totalCharsRead.get());
                logger.info("   平均吞吐: " +
                        (totalBytesRead.get() / (totalSocketTime.get() > 0 ? totalSocketTime.get()/1000.0 : 1)) + " B/s");
            }

            long fileCmd = fileCommands.get();
            if (fileCmd > 0) {
                logger.info("文件模式:");
                logger.info("   命令数: " + fileCmd);
                logger.info("   成功: " + fileCommandsCompleted.get() +
                        ", 失败: " + fileCommandsFailed.get());
                logger.info("   总耗时: " + totalFileTime.get() + "ms");
                logger.info("   平均耗时: " + (totalFileTime.get() / fileCmd) + "ms");
                logger.info("   最快耗时: " +
                        (fastestFileTime.get() == Long.MAX_VALUE ? 0 : fastestFileTime.get()) + "ms");
                logger.info("   最慢耗时: " + slowestFileTime.get() + "ms");
            }
            logger.info("=========================\n");

            System.err.println("\n============ 性能统计 ============");
            System.err.println("总命令数: " + commands);
            System.err.println("成功: " + commandsCompleted.get() +
                    ", 失败: " + commandsFailed.get());
            System.err.println("成功率: " +
                    String.format(Locale.getDefault(),
                            "%.1f%%", commandsCompleted.get() * 100.0 / commands));
            System.err.println("总连接数: " + totalConnections.get());

            if (socketCmd > 0) {
                System.err.println("Socket模式:");
                System.err.println("   命令数: " + socketCmd);
                System.err.println("   成功: " + socketCommandsCompleted.get() +
                        ", 失败: " + socketCommandsFailed.get());
                System.err.println("   总耗时: " + totalSocketTime.get() + "ms");
                System.err.println("   平均耗时: " + (totalSocketTime.get() / socketCmd) + "ms");
                System.err.println("   最快耗时: " +
                        (fastestSocketTime.get() == Long.MAX_VALUE ? 0 : fastestSocketTime.get()) + "ms");
                System.err.println("   最慢耗时: " + slowestSocketTime.get() + "ms");
                System.err.println("   总读取字节: " + totalBytesRead.get());
                System.err.println("   总读取字符: " + totalCharsRead.get());
                System.err.println("   平均吞吐: " +
                        (totalBytesRead.get() / (totalSocketTime.get() > 0 ? totalSocketTime.get()/1000.0 : 1)) + " B/s");
            }

            if (fileCmd > 0) {
                System.err.println("文件模式:");
                System.err.println("   命令数: " + fileCmd);
                System.err.println("   成功: " + fileCommandsCompleted.get() +
                        ", 失败: " + fileCommandsFailed.get());
                System.err.println("   总耗时: " + totalFileTime.get() + "ms");
                System.err.println("   平均耗时: " + (totalFileTime.get() / fileCmd) + "ms");
                System.err.println("   最快耗时: " +
                        (fastestFileTime.get() == Long.MAX_VALUE ? 0 : fastestFileTime.get()) + "ms");
                System.err.println("   最慢耗时: " + slowestFileTime.get() + "ms");
            }
            System.err.println("=========================\n");
        } else {
            System.err.println("暂无性能统计数据");
            System.err.println("提示: 请先执行一些命令后再查看统计信息");
        }
    }
}
