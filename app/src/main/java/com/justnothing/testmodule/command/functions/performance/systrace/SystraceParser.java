package com.justnothing.testmodule.command.functions.performance.systrace;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SystraceParser {
    private static final String TAG = "SystraceParser";

    public static SystraceData parse(String file) {
        try {
            File traceFile = new File(file);
            if (!traceFile.exists()) {
                throw new IllegalArgumentException("Systrace 文件不存在: " + file);
            }

            Log.i(TAG, "解析 Systrace 文件: " + file);

            long duration = parseDuration(traceFile);
            SystraceData.CPUData cpuData = parseCpuData(traceFile);
            SystraceData.GPUData gpuData = parseGpuData(traceFile);
            SystraceData.MemoryData memoryData = parseMemoryData(traceFile);
            List<SystraceData.ThreadData> threadData = parseThreadData(traceFile);
            List<SystraceData.IOData> ioData = parseIoData(traceFile);

            return new SystraceData(file, duration, cpuData, gpuData, memoryData, threadData, ioData);

        } catch (Exception e) {
            Log.e(TAG, "解析 Systrace 文件失败", e);
            throw new RuntimeException("解析 Systrace 文件失败: " + e.getMessage(), e);
        }
    }

    public static String generateReport(SystraceData data) {
        StringBuilder report = new StringBuilder();
        report.append("=== Systrace 报告 ===\n");
        report.append("文件: ").append(data.file()).append("\n");
        report.append("持续时间: ").append(data.duration() / 1000.0).append(" 秒\n");
        report.append("\n");

        if (data.cpuData() != null) {
            report.append("CPU 数据:\n");
            report.append("  平均使用率: ").append(String.format(Locale.getDefault(), "%.2f%%", data.cpuData().averageUsage() * 100)).append("\n");
            if (!data.cpuData().cpuUsage().isEmpty()) {
                report.append("  各核心使用率:\n");
                for (Map.Entry<Integer, Double> entry : data.cpuData().cpuUsage().entrySet()) {
                    report.append("    核心 ").append(entry.getKey()).append(": ")
                          .append(String.format(Locale.getDefault(), "%.2f%%", entry.getValue() * 100)).append("\n");
                }
            }
            report.append("\n");
        }

        if (data.gpuData() != null) {
            report.append("GPU 数据:\n");
            report.append("  使用率: ").append(String.format(Locale.getDefault(), "%.2f%%", data.gpuData().usage() * 100)).append("\n");
            report.append("  FPS: ").append(data.gpuData().fps()).append("\n");
            report.append("  丢帧数: ").append(data.gpuData().droppedFrames()).append("\n");
            report.append("\n");
        }

        if (data.memoryData() != null) {
            report.append("内存数据:\n");
            report.append("  总内存: ").append(formatBytes(data.memoryData().totalMemory())).append("\n");
            report.append("  堆内存: ").append(formatBytes(data.memoryData().heapMemory())).append("\n");
            report.append("  GC 次数: ").append(data.memoryData().gcCount()).append("\n");
            report.append("  GC 总耗时: ").append(data.memoryData().gcDuration() / 1_000_000.0).append(" ms\n");
            report.append("\n");
        }

        if (data.threadData() != null && !data.threadData().isEmpty()) {
            report.append("线程数据:\n");
            for (SystraceData.ThreadData thread : data.threadData()) {
                report.append("  ").append(thread.threadName()).append(" (ID: ").append(thread.threadId()).append(")\n");
                report.append("    状态: ").append(thread.state()).append("\n");
                report.append("    CPU 使用率: ").append(String.format(Locale.getDefault(), "%.2f%%", thread.cpuUsage() * 100)).append("\n");
            }
            report.append("\n");
        }

        if (data.ioData() != null && !data.ioData().isEmpty()) {
            report.append("I/O 数据:\n");
            for (SystraceData.IOData io : data.ioData()) {
                report.append("  ").append(io.operation()).append("\n");
                report.append("    字节数: ").append(formatBytes(io.bytes())).append("\n");
                report.append("    耗时: ").append(io.duration() / 1_000_000.0).append(" ms\n");
            }
            report.append("\n");
        }

        report.append("说明:\n");
        report.append("  - Systrace 数据来自系统级性能分析\n");
        report.append("  - 可以使用 Chrome 浏览器打开 HTML 文件查看详细信息\n");
        report.append("  - 建议使用 Systrace HTML 文件进行详细分析\n");

        return report.toString();
    }

    private static SystraceData.CPUData parseCpuData(File file) {
        Map<Integer, Double> cpuUsage = new HashMap<>();
        double totalUsage = 0.0;
        int coreCount = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("cpu") && line.contains("usage")) {
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.matches("\\d+\\.\\d+%")) {
                            double usage = Double.parseDouble(part.replace("%", "")) / 100.0;
                            cpuUsage.put(coreCount++, usage);
                            totalUsage += usage;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "解析 CPU 数据失败", e);
        }

        double averageUsage = coreCount > 0 ? totalUsage / coreCount : 0.0;
        return new SystraceData.CPUData(cpuUsage, averageUsage);
    }

    private static SystraceData.GPUData parseGpuData(File file) {
        double usage = 0.0;
        int fps = 0;
        int droppedFrames = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("gpu") && line.contains("usage")) {
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.matches("\\d+\\.\\d+%")) {
                            usage = Double.parseDouble(part.replace("%", "")) / 100.0;
                            break;
                        }
                    }
                } else if (line.contains("fps")) {
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.matches("\\d+")) {
                            fps = Integer.parseInt(part);
                            break;
                        }
                    }
                } else if (line.contains("dropped") || line.contains("jank")) {
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.matches("\\d+")) {
                            droppedFrames = Integer.parseInt(part);
                            break;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "解析 GPU 数据失败", e);
        }

        return new SystraceData.GPUData(usage, fps, droppedFrames);
    }

    private static SystraceData.MemoryData parseMemoryData(File file) {
        long totalMemory = 0;
        long heapMemory = 0;
        int gcCount = 0;
        long gcDuration = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("memory") || line.contains("mem")) {
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.matches("\\d+[KMGT]?B")) {
                            long bytes = parseBytes(part);
                            if (totalMemory == 0) {
                                totalMemory = bytes;
                            } else if (heapMemory == 0) {
                                heapMemory = bytes;
                            }
                        }
                    }
                } else if (line.contains("gc") || line.contains("GC")) {
                    gcCount++;
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        if (part.matches("\\d+ms")) {
                            gcDuration += Long.parseLong(part.replace("ms", "")) * 1_000_000;
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "解析内存数据失败", e);
        }

        return new SystraceData.MemoryData(totalMemory, heapMemory, gcCount, gcDuration);
    }

    private static List<SystraceData.ThreadData> parseThreadData(File file) {
        List<SystraceData.ThreadData> threadData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("thread") && line.contains("state")) {
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 4) {
                        String threadName = parts[1];
                        int threadId = Integer.parseInt(parts[2].replace("ID:", ""));
                        String state = parts[3];
                        double cpuUsage = 0.0;
                        
                        for (String part : parts) {
                            if (part.matches("\\d+\\.\\d+%")) {
                                cpuUsage = Double.parseDouble(part.replace("%", "")) / 100.0;
                                break;
                            }
                        }
                        
                        threadData.add(new SystraceData.ThreadData(threadId, threadName, state, cpuUsage));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "解析线程数据失败", e);
        }

        return threadData;
    }

    private static List<SystraceData.IOData> parseIoData(File file) {
        List<SystraceData.IOData> ioData = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("read") || line.contains("write") || line.contains("io")) {
                    String[] parts = line.split("\\s+");
                    String operation = parts[0];
                    long bytes = 0;
                    long duration = 0;
                    
                    for (String part : parts) {
                        if (part.matches("\\d+[KMGT]?B")) {
                            bytes = parseBytes(part);
                        } else if (part.matches("\\d+ms")) {
                            duration = Long.parseLong(part.replace("ms", "")) * 1_000_000;
                        }
                    }
                    
                    if (bytes > 0 || duration > 0) {
                        ioData.add(new SystraceData.IOData(operation, bytes, duration));
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "解析 I/O 数据失败", e);
        }

        return ioData;
    }

    private static long parseDuration(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("duration") || line.contains("time")) {
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        part = part.trim();
                        if (part.matches("\\d+\\.?\\d*[sm]?")) {
                            if (part.endsWith("s")) {
                                return (long) (Double.parseDouble(part.replace("s", "")) * 1000);
                            } else if (part.endsWith("m")) {
                                return (long) (Double.parseDouble(part.replace("m", "")) * 60000);
                            } else {
                                return (long) (Double.parseDouble(part) * 1000);
                            }
                        }
                    }
                }
                
                if (line.contains("var traceEvents")) {
                    int startIndex = line.indexOf("\"duration\"");
                    if (startIndex != -1) {
                        int valueStart = line.indexOf(":", startIndex);
                        int valueEnd = line.indexOf(",", startIndex);
                        if (valueStart != -1 && valueEnd != -1) {
                            String durationStr = line.substring(valueStart + 1, valueEnd).trim();
                            try {
                                return (long) (Double.parseDouble(durationStr) * 1000);
                            } catch (NumberFormatException e) {
                                Log.w(TAG, "无法解析 duration: " + durationStr);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "解析持续时间失败", e);
        }

        return 0;
    }

    private static long parseBytes(String str) {
        str = str.toUpperCase();
        if (str.endsWith("KB")) {
            return Long.parseLong(str.replace("KB", "")) * 1024;
        } else if (str.endsWith("MB")) {
            return Long.parseLong(str.replace("MB", "")) * 1024 * 1024;
        } else if (str.endsWith("GB")) {
            return Long.parseLong(str.replace("GB", "")) * 1024 * 1024 * 1024;
        } else if (str.endsWith("TB")) {
            return Long.parseLong(str.replace("TB", "")) * 1024L * 1024L * 1024L * 1024L;
        } else if (str.endsWith("B")) {
            return Long.parseLong(str.replace("B", ""));
        }
        return 0;
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024.0));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
        }
    }
}
