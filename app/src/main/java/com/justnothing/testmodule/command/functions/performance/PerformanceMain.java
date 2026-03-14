package com.justnothing.testmodule.command.functions.performance;

import static com.justnothing.testmodule.constants.CommandServer.CMD_PERFORMANCE_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.performance.hook.HookData;
import com.justnothing.testmodule.command.functions.performance.hook.MethodStats;
import com.justnothing.testmodule.command.functions.performance.hook.PerformanceHook;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampler;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampler;
import com.justnothing.testmodule.command.functions.performance.sampler.SampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.SimpleSampler;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceParser;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceRunner;
import com.justnothing.testmodule.command.functions.performance.trace.TraceData;
import com.justnothing.testmodule.command.functions.performance.trace.Tracer;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.reflect.ClassResolver;
import com.justnothing.testmodule.utils.functions.Logger;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMain extends CommandBase {
    private static final Logger staticLogger = Logger.getLoggerForName("Performance");
    private static final Map<Integer, SimpleSampler> samplers = new ConcurrentHashMap<>();
    private static final Map<Integer, SampleData> sampleDataMap = new ConcurrentHashMap<>();
    private static final Map<Integer, MultiThreadSampler> multiThreadSamplers = new ConcurrentHashMap<>();
    private static final Map<Integer, MultiThreadSampleData> multiThreadSampleDataMap = new ConcurrentHashMap<>();
    private static final Map<Integer, HierarchicalSampler> hierarchicalSamplers = new ConcurrentHashMap<>();
    private static final Map<Integer, HierarchicalSampleData> hierarchicalSampleDataMap = new ConcurrentHashMap<>();
    private static final Map<Integer, Tracer> tracers = new ConcurrentHashMap<>();
    private static final Map<Integer, List<TraceData>> traceDataMap = new ConcurrentHashMap<>();
    private static final Map<Integer, SystraceRunner> systraceRunners = new ConcurrentHashMap<>();
    private static final Map<Integer, SystraceData> systraceDataMap = new ConcurrentHashMap<>();
    private static final Map<Integer, HookData> hookDataMap = new ConcurrentHashMap<>();
    private static final AtomicInteger nextId = new AtomicInteger(1);

    public PerformanceMain() {
        super("Performance");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: performance <subcmd> [args...]
                
                性能分析命令，支持多种分析方式。
                
                子命令:
                    sample start [rate]                    - 开始采样
                    sample stop <id>                       - 停止采样
                    sample report [id]                      - 查看采样报告
                    sample export <id> <file>              - 导出采样数据
                
                    multithread start [rate]               - 开始多线程采样
                    multithread stop <id>                  - 停止多线程采样
                    multithread report [id]                - 查看多线程采样报告
                    multithread export <id> <file>         - 导出多线程采样数据
                
                    hierarchical start [rate]              - 开始分层采样
                    hierarchical stop <id>                 - 停止分层采样
                    hierarchical report [id]               - 查看分层采样报告
                    hierarchical export <id> <file>        - 导出分层采样数据
                
                    trace start                            - 开始 Trace
                    trace stop <id>                        - 停止 Trace
                    trace report [id]                       - 查看 Trace 报告
                    trace export <id> <file>                - 导出 Trace 数据
                
                    systrace start [duration] [categories]  - 开始 Systrace
                    systrace stop <id>                      - 停止 Systrace
                    systrace report [id]                     - 查看 Systrace 报告
                    systrace export <id> <file>              - 导出 Systrace 数据
                
                    hook <class> [method] [sig]            - Hook 方法
                    hook stop <id>                         - 停止 Hook
                    hook report [id]                        - 查看 Hook 报告
                    hook export <id> <file>                - 导出 Hook 数据
                
                    list                                    - 列出所有任务
                    clear                                   - 清除所有任务
                
                选项:
                    rate   - 采样频率（Hz），默认 100
                    class  - 类名
                    method - 方法名（可选）
                    sig    - 方法签名（可选）
                
                示例:
                    performance sample start 100
                    performance sample stop 1
                    performance sample report 1
                
                    performance multithread start 100
                    performance multithread report 1
                
                    performance hierarchical start 100
                    performance hierarchical report 1
                
                    performance hook com.example.MyClass
                    performance hook com.example.MyClass myMethod
                    performance hook stop 1
                    performance hook report 1
                
                注意:
                    - 采样方式会定期获取调用栈，统计方法调用次数
                    - 多线程采样会分别统计每个线程的方法调用
                    - 分层采样会记录方法的调用关系和调用者
                    - Hook 方式会精确记录方法执行时间
                    - 默认采样频率为 100 Hz，平衡开销和精度
                    - 采样频率越高，精度越高，但开销也越大
                
                (Submodule performance %s)
                """, CMD_PERFORMANCE_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        if (args.length < 1) {
            return getHelpText();
        }

        String subCommand = args[0];

        try {
            return switch (subCommand) {
                case "sample" -> handleSample(args);
                case "multithread" -> handleMultiThread(args);
                case "hierarchical" -> handleHierarchical(args);
                case "trace" -> handleTrace(args);
                case "systrace" -> handleSystrace(args);
                case "hook" -> handleHook(args, context.classLoader());
                case "list" -> handleList();
                case "clear" -> handleClear();
                default -> "未知子命令: " + subCommand + "\n" + getHelpText();
            };
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("performance", e, logger);
        }
    }

    private String handleSample(String[] args) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: performance sample <subcmd> [args...]";
        }

        String sampleSubCmd = args[1];

        return switch (sampleSubCmd) {
            case "start" -> handleSampleStart(args);
            case "stop" -> handleSampleStop(args);
            case "report" -> handleSampleReport(args);
            case "export" -> handleSampleExport(args);
            default -> "未知子命令: " + sampleSubCmd;
        };
    }

    private String handleSampleStart(String[] args) {
        int sampleRate = 100;
        if (args.length >= 3) {
            try {
                sampleRate = Integer.parseInt(args[2]);
                if (sampleRate <= 0) {
                    return "错误: 采样频率必须大于 0";
                }
                if (sampleRate > 10000) {
                    return "警告: 采样频率过高（" + sampleRate + " Hz），可能影响性能";
                }
            } catch (NumberFormatException e) {
                return "错误: 无效的采样频率: " + args[2];
            }
        }

        int id = nextId.getAndIncrement();
        SimpleSampler sampler = new SimpleSampler(sampleRate);
        sampler.start();
        samplers.put(id, sampler);

        logger.info("采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        staticLogger.info("采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        
        return "采样器已启动\n" +
                "ID: " + id + "\n" +
                "采样频率: " + sampleRate + " Hz\n" +
                "使用 'performance sample stop " + id + "' 停止采样\n" +
                "使用 'performance sample report " + id + "' 查看报告";
    }

    private String handleSampleStop(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance sample stop <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        SimpleSampler sampler = samplers.get(id);
        if (sampler == null) {
            return "错误: 采样器不存在 (ID: " + id + ")";
        }

        sampler.stop();
        long stopTime = sampler.getStopTime();
        int totalSamples = sampler.getTotalSamples();
        Map<String, Integer> report = sampler.getReport();

        SampleData data = new SampleData(id, sampler.getSampleRate(), 
            sampler.getStartTime(), stopTime, totalSamples, report);
        sampleDataMap.put(id, data);

        logger.info("采样器已停止 (ID: " + id + ")");
        staticLogger.info("采样器已停止 (ID: " + id + ", 采样次数: " + totalSamples + ")");
        
        return "采样器已停止 (ID: " + id + ")\n" +
                "采样次数: " + totalSamples + "\n" +
                "持续时间: " + data.getDurationString() + "\n" +
                "使用 'performance sample report " + id + "' 查看报告";
    }

    private String handleSampleReport(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance sample report <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        SampleData data = sampleDataMap.get(id);
        if (data == null) {
            return "错误: 采样数据不存在 (ID: " + id + ")";
        }

        if (data.methodCounts().isEmpty()) {
            return "没有采样数据";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== 采样报告 ===\n");
        result.append("ID: ").append(data.id()).append("\n");
        result.append("采样频率: ").append(data.sampleRate()).append(" Hz\n");
        result.append("采样次数: ").append(data.totalSamples()).append("\n");
        result.append("持续时间: ").append(data.getDurationString()).append("\n");
        result.append("\n");
        result.append("热点方法:\n");

        data.methodCounts().entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .forEach(entry -> {
                int count = entry.getValue();
                double percentage = (count * 100.0) / data.totalSamples();
                result.append(String.format(Locale.getDefault(),
                        "  %-60s %6d 次 (%5.1f%%)%n",
                    entry.getKey(), count, percentage));
            });

        result.append("\n");
        result.append("建议: 使用 'performance hook' 精确分析热点方法\n");

        return result.toString();
    }

    private String handleSampleExport(String[] args) {
        if (args.length < 4) {
            return "错误: 参数不足\n用法: performance sample export <id> <file>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        String filePath = args[3];
        SampleData data = sampleDataMap.get(id);
        if (data == null) {
            return "错误: 采样数据不存在 (ID: " + id + ")";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": ").append(data.id()).append(",\n");
        json.append("  \"sampleRate\": ").append(data.sampleRate()).append(",\n");
        json.append("  \"startTime\": ").append(data.startTime()).append(",\n");
        json.append("  \"stopTime\": ").append(data.stopTime()).append(",\n");
        json.append("  \"totalSamples\": ").append(data.totalSamples()).append(",\n");
        json.append("  \"duration\": ").append(data.getDuration()).append(",\n");
        json.append("  \"methodCounts\": {\n");

        boolean first = true;
        for (Map.Entry<String, Integer> entry : data.methodCounts().entrySet()) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            json.append("    \"").append(entry.getKey()).append("\": ").append(entry.getValue());
        }

        json.append("\n  }\n");
        json.append("}\n");

        if (!writeToFile(filePath, json.toString())) {
            return "导出采样数据失败: 无法写入文件 " + filePath;
        }

        logger.info("采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        return "采样数据已导出\n" +
                "ID: " + id + "\n" +
                "文件: " + filePath + "\n" +
                "采样次数: " + data.totalSamples();
    }

    private String handleHook(String[] args, ClassLoader classLoader) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: performance hook <subcmd> [args...]";
        }

        String hookSubCmd = args[1];

        return switch (hookSubCmd) {
            case "start" -> handleHookStart(args, classLoader);
            case "stop" -> handleHookStop(args);
            case "report" -> handleHookReport(args);
            case "export" -> handleHookExport(args);
            default -> handleHookMethod(args, classLoader);
        };
    }

    private String handleHookMethod(String[] args, ClassLoader classLoader) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: performance hook <class> [method] [sig]";
        }

        String className = args[1];
        String methodName = args.length >= 3 ? args[2] : null;
        String methodSig = args.length >= 4 ? args[3] : null;

        Class<?> clazz = ClassResolver.findClass(className, classLoader);
        if (clazz == null) {
            return "错误: 未找到类: " + className;
        }

        int id;
        long startTime = System.currentTimeMillis();

        if (methodName == null) {
            id = PerformanceHook.hookAllMethods(clazz, "*");
        } else {
            Class<?>[] paramTypes = null;
            if (methodSig != null) {
                paramTypes = parseMethodSignature(methodSig);
            }
            id = PerformanceHook.hookMethod(clazz, methodName, paramTypes);
        }

        HookData data = new HookData(id, className, methodName != null ? methodName : "*", 
            methodName != null ? (methodSig != null ? methodSig : "()") : "*",
            startTime, -1, 0, 0, 0, 0, 0.0);
        hookDataMap.put(id, data);

        logger.info("Hook 已添加 (ID: " + id + ", 类: " + className + ", 方法: " + methodName + ")");
        staticLogger.info("Hook 已添加 (ID: " + id + ", 类: " + className + ", 方法: " + methodName + ")");

        return "Hook 已添加\n" +
                "ID: " + id + "\n" +
                "类: " + className + "\n" +
                "方法: " + (methodName != null ? methodName : "所有方法") + "\n" +
                "使用 'performance hook stop " + id + "' 停止 Hook\n" +
                "使用 'performance hook report " + id + "' 查看报告";
    }

    private String handleHookStart(String[] args, ClassLoader classLoader) {
        return handleHookMethod(args, classLoader);
    }

    private String handleHookStop(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance hook stop <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        if (!PerformanceHook.hasHook(id)) {
            return "错误: Hook 不存在 (ID: " + id + ")";
        }

        PerformanceHook.unhookMethod(id);
        HookData data = hookDataMap.get(id);
        if (data != null) {
            MethodStats stats = PerformanceHook.getStats(id);
            if (stats != null) {
                data = HookData.fromMethodStats(id, stats, data.startTime(), System.currentTimeMillis());
                hookDataMap.put(id, data);
            }
        }

        logger.info("Hook 已停止 (ID: " + id + ")");
        staticLogger.info("Hook 已停止 (ID: " + id + ")");

        return "Hook 已停止 (ID: " + id + ")";
    }

    private String handleHookReport(String[] args) {
        int id;
        if (args.length >= 3) {
            try {
                id = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                return "错误: 无效的 ID: " + args[2];
            }
            
            HookData data = hookDataMap.get(id);
            if (data == null) {
                return "错误: Hook 数据不存在 (ID: " + id + ")";
            }

            MethodStats stats = PerformanceHook.getStats(id);
            if (stats == null) {
                return "没有 Hook 数据";
            }

            StringBuilder result = new StringBuilder();
            result.append("=== Hook 报告 ===\n");
            result.append("ID: ").append(data.id()).append("\n");
            result.append("类: ").append(data.className()).append("\n");
            result.append("方法: ").append(data.methodName()).append("\n");
            result.append("签名: ").append(data.signature()).append("\n");
            result.append("调用次数: ").append(stats.callCount.get()).append("\n");
            result.append("总耗时: ").append(data.getDurationStringNs(stats.totalDuration.get())).append("\n");
            result.append("平均耗时: ").append(data.getDurationStringNs((long) stats.getAverageDuration())).append("\n");
            result.append("最小耗时: ").append(data.getDurationStringNs(stats.minDuration.get())).append("\n");
            result.append("最大耗时: ").append(data.getDurationStringNs(stats.maxDuration.get())).append("\n");
            result.append("\n");
            result.append("性能建议:\n");
            
            if (stats.getAverageDuration() > 1_000_000) {
                result.append("  - 平均耗时较长（>1ms），建议优化\n");
            }
            if (stats.maxDuration.get() > 10_000_000) {
                result.append("  - 最大耗时较长（>10ms），可能存在性能瓶颈\n");
            }
            if (stats.callCount.get() > 10000) {
                result.append("  - 调用次数较多，考虑缓存或优化算法\n");
            }

            return result.toString();
        } else {
            Map<Integer, MethodStats> allStats = PerformanceHook.getStats();
            if (allStats.isEmpty()) {
                return "没有 Hook 数据";
            }

            StringBuilder result = new StringBuilder();
            result.append("=== 所有 Hook 报告 ===\n");
            result.append("Hook 数量: ").append(allStats.size()).append("\n\n");

            for (Map.Entry<Integer, MethodStats> entry : allStats.entrySet()) {
                MethodStats stats = entry.getValue();
                HookData data = hookDataMap.get(entry.getKey());
                if (data != null) {
                    result.append("ID: ").append(entry.getKey()).append("\n");
                    result.append("  类: ").append(stats.className).append("\n");
                    result.append("  方法: ").append(stats.methodName).append("\n");
                    result.append("  调用次数: ").append(stats.callCount.get()).append("\n");
                    result.append("  平均耗时: ").append(data.getDurationStringNs((long) stats.getAverageDuration())).append("\n");
                    result.append("\n");
                }
            }

            return result.toString();
        }
    }

    private String handleHookExport(String[] args) {
        if (args.length < 4) {
            return "错误: 参数不足\n用法: performance hook export <id> <file>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        String filePath = args[3];
        HookData data = hookDataMap.get(id);
        if (data == null) {
            return "错误: Hook 数据不存在 (ID: " + id + ")";
        }

        MethodStats stats = PerformanceHook.getStats(id);
        if (stats == null) {
            return "错误: Hook 统计数据不存在 (ID: " + id + ")";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": ").append(data.id()).append(",\n");
        json.append("  \"className\": \"").append(data.className()).append("\",\n");
        json.append("  \"methodName\": \"").append(data.methodName()).append("\",\n");
        json.append("  \"signature\": \"").append(data.signature()).append("\",\n");
        json.append("  \"startTime\": ").append(data.startTime()).append(",\n");
        json.append("  \"stopTime\": ").append(data.stopTime()).append(",\n");
        json.append("  \"callCount\": ").append(stats.callCount.get()).append(",\n");
        json.append("  \"totalDuration\": ").append(stats.totalDuration.get()).append(",\n");
        json.append("  \"minDuration\": ").append(stats.minDuration.get()).append(",\n");
        json.append("  \"maxDuration\": ").append(stats.maxDuration.get()).append(",\n");
        json.append("  \"averageDuration\": ").append(stats.getAverageDuration()).append("\n");
        json.append("}\n");

        if (!writeToFile(filePath, json.toString())) {
            return "导出 Hook 数据失败: 无法写入文件 " + filePath;
        }

        logger.info("Hook 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("Hook 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        return "Hook 数据已导出\n" +
                "ID: " + id + "\n" +
                "文件: " + filePath + "\n" +
                "调用次数: " + stats.callCount.get();
    }

    private Class<?>[] parseMethodSignature(String sig) {
        return new Class<?>[0];
    }

    private String handleList() {
        StringBuilder result = new StringBuilder();
        result.append("=== 性能分析任务列表 ===\n\n");

        if (!samplers.isEmpty()) {
            result.append("运行中的采样器:\n");
            for (Map.Entry<Integer, SimpleSampler> entry : samplers.entrySet()) {
                SimpleSampler sampler = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: 单线程采样\n");
                result.append("    状态: 运行中\n");
                result.append("    采样频率: ").append(sampler.getSampleRate()).append(" Hz\n");
                result.append("    采样次数: ").append(sampler.getTotalSamples()).append("\n");
                result.append("    开始时间: ").append(new java.util.Date(sampler.getStartTime())).append("\n");
                result.append("\n");
            }
        }

        if (!multiThreadSamplers.isEmpty()) {
            result.append("运行中的多线程采样器:\n");
            for (Map.Entry<Integer, MultiThreadSampler> entry : multiThreadSamplers.entrySet()) {
                MultiThreadSampler sampler = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: 多线程采样\n");
                result.append("    状态: 运行中\n");
                result.append("    采样频率: ").append(sampler.getSampleRate()).append(" Hz\n");
                result.append("    采样次数: ").append(sampler.getTotalSamples()).append("\n");
                result.append("    线程数量: ").append(sampler.getThreadCount()).append("\n");
                result.append("    开始时间: ").append(new java.util.Date(sampler.getStartTime())).append("\n");
                result.append("\n");
            }
        }

        if (!hierarchicalSamplers.isEmpty()) {
            result.append("运行中的分层采样器:\n");
            for (Map.Entry<Integer, HierarchicalSampler> entry : hierarchicalSamplers.entrySet()) {
                HierarchicalSampler sampler = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: 分层采样\n");
                result.append("    状态: 运行中\n");
                result.append("    采样频率: ").append(sampler.getSampleRate()).append(" Hz\n");
                result.append("    采样次数: ").append(sampler.getTotalSamples()).append("\n");
                result.append("    方法数量: ").append(sampler.getMethodCount()).append("\n");
                result.append("    开始时间: ").append(new java.util.Date(sampler.getStartTime())).append("\n");
                result.append("\n");
            }
        }

        if (!tracers.isEmpty()) {
            result.append("运行中的 Tracer:\n");
            for (Map.Entry<Integer, Tracer> entry : tracers.entrySet()) {
                Tracer tracer = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: Trace\n");
                result.append("    状态: 运行中\n");
                result.append("    Trace 数据: ").append(tracer.getSectionCount()).append("\n");
                result.append("    开始时间: ").append(new java.util.Date(tracer.getStartTime())).append("\n");
                result.append("\n");
            }
        }

        if (!sampleDataMap.isEmpty()) {
            result.append("已完成的采样数据:\n");
            for (Map.Entry<Integer, SampleData> entry : sampleDataMap.entrySet()) {
                SampleData data = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: 单线程采样\n");
                result.append("    状态: 已完成\n");
                result.append("    采样频率: ").append(data.sampleRate()).append(" Hz\n");
                result.append("    采样次数: ").append(data.totalSamples()).append("\n");
                result.append("    持续时间: ").append(data.getDurationString()).append("\n");
                result.append("    开始时间: ").append(new java.util.Date(data.startTime())).append("\n");
                result.append("    结束时间: ").append(new java.util.Date(data.stopTime())).append("\n");
                result.append("\n");
            }
        }

        if (!multiThreadSampleDataMap.isEmpty()) {
            result.append("已完成的多线程采样数据:\n");
            for (Map.Entry<Integer, MultiThreadSampleData> entry : multiThreadSampleDataMap.entrySet()) {
                MultiThreadSampleData data = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: 多线程采样\n");
                result.append("    状态: 已完成\n");
                result.append("    采样频率: ").append(data.sampleRate()).append(" Hz\n");
                result.append("    采样次数: ").append(data.totalSamples()).append("\n");
                result.append("    线程数量: ").append(data.threadCount()).append("\n");
                result.append("    持续时间: ").append(data.getDurationString()).append("\n");
                result.append("    开始时间: ").append(new java.util.Date(data.startTime())).append("\n");
                result.append("    结束时间: ").append(new java.util.Date(data.stopTime())).append("\n");
                result.append("\n");
            }
        }

        if (!hierarchicalSampleDataMap.isEmpty()) {
            result.append("已完成的分层采样数据:\n");
            for (Map.Entry<Integer, HierarchicalSampleData> entry : hierarchicalSampleDataMap.entrySet()) {
                HierarchicalSampleData data = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: 分层采样\n");
                result.append("    状态: 已完成\n");
                result.append("    采样频率: ").append(data.sampleRate()).append(" Hz\n");
                result.append("    采样次数: ").append(data.totalSamples()).append("\n");
                result.append("    方法数量: ").append(data.methodCount()).append("\n");
                result.append("    持续时间: ").append(data.getDurationString()).append("\n");
                result.append("    开始时间: ").append(new java.util.Date(data.startTime())).append("\n");
                result.append("    结束时间: ").append(new java.util.Date(data.stopTime())).append("\n");
                result.append("\n");
            }
        }

        if (!traceDataMap.isEmpty()) {
            result.append("已完成的 Trace 数据:\n");
            for (Map.Entry<Integer, List<TraceData>> entry : traceDataMap.entrySet()) {
                List<TraceData> data = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: Trace\n");
                result.append("    状态: 已完成\n");
                result.append("    Trace 数据: ").append(data.size()).append("\n");
                result.append("\n");
            }
        }

        if (!systraceRunners.isEmpty()) {
            result.append("运行中的 Systrace:\n");
            for (Map.Entry<Integer, SystraceRunner> entry : systraceRunners.entrySet()) {
                SystraceRunner runner = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: Systrace\n");
                result.append("    状态: 运行中\n");
                result.append("    持续时间: ").append(runner.getDuration() / 1000.0).append(" 秒\n");
                result.append("    开始时间: ").append(new java.util.Date(runner.getStartTime())).append("\n");
                result.append("\n");
            }
        }

        if (!systraceDataMap.isEmpty()) {
            result.append("已完成的 Systrace 数据:\n");
            for (Map.Entry<Integer, SystraceData> entry : systraceDataMap.entrySet()) {
                SystraceData data = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    类型: Systrace\n");
                result.append("    状态: 已完成\n");
                result.append("    文件: ").append(data.file()).append("\n");
                result.append("    持续时间: ").append(data.duration() / 1000.0).append(" 秒\n");
                result.append("\n");
            }
        }

        if (PerformanceHook.getHookCount() > 0) {
            result.append("运行中的 Hook:\n");
            for (Map.Entry<Integer, HookData> entry : hookDataMap.entrySet()) {
                HookData data = entry.getValue();
                result.append("  ID: ").append(entry.getKey()).append("\n");
                result.append("    状态: ").append(data.stopTime() == -1 ? "运行中" : "已停止").append("\n");
                result.append("    类: ").append(data.className()).append("\n");
                result.append("    方法: ").append(data.methodName()).append("\n");
                result.append("    开始时间: ").append(new java.util.Date(data.startTime())).append("\n");
                if (data.stopTime() != -1) {
                    result.append("    结束时间: ").append(new java.util.Date(data.stopTime())).append("\n");
                }
                result.append("\n");
            }
        }

        if (result.toString().equals("=== 性能分析任务列表 ===\n\n")) {
            return "没有运行中的性能分析任务";
        }

        return result.toString();
    }

    private String handleClear() {
        int samplerCount = samplers.size();
        int dataCount = sampleDataMap.size();
        int multiThreadSamplerCount = multiThreadSamplers.size();
        int multiThreadDataCount = multiThreadSampleDataMap.size();
        int hierarchicalSamplerCount = hierarchicalSamplers.size();
        int hierarchicalDataCount = hierarchicalSampleDataMap.size();
        int tracerCount = tracers.size();
        int traceDataCount = traceDataMap.size();
        int systraceRunnerCount = systraceRunners.size();
        int systraceDataCount = systraceDataMap.size();
        int hookCount = PerformanceHook.getHookCount();

        for (SimpleSampler sampler : samplers.values()) {
            sampler.stop();
        }

        for (MultiThreadSampler sampler : multiThreadSamplers.values()) {
            sampler.stop();
        }

        for (HierarchicalSampler sampler : hierarchicalSamplers.values()) {
            sampler.stop();
        }

        for (Tracer tracer : tracers.values()) {
            if (tracer.isRunning()) {
                tracer.stop();
            }
        }

        for (SystraceRunner runner : systraceRunners.values()) {
            if (runner.isRunning()) {
                runner.stop();
            }
        }

        samplers.clear();
        sampleDataMap.clear();
        multiThreadSamplers.clear();
        multiThreadSampleDataMap.clear();
        hierarchicalSamplers.clear();
        hierarchicalSampleDataMap.clear();
        tracers.clear();
        traceDataMap.clear();
        systraceRunners.clear();
        systraceDataMap.clear();
        hookDataMap.clear();
        PerformanceHook.unhookAll();
        nextId.set(1);

        logger.info("已清除所有性能分析任务");
        staticLogger.info("已清除所有性能分析任务 (采样器: " + samplerCount + ", 数据: " + dataCount + 
            ", 多线程采样器: " + multiThreadSamplerCount + ", 多线程数据: " + multiThreadDataCount +
            ", 分层采样器: " + hierarchicalSamplerCount + ", 分层数据: " + hierarchicalDataCount +
            ", Tracer: " + tracerCount + ", Trace 数据: " + traceDataCount +
            ", Systrace: " + systraceRunnerCount + ", Systrace 数据: " + systraceDataCount +
            ", Hook: " + hookCount + ")");

        return "已清除所有性能分析任务\n" +
                "清除的采样器: " + samplerCount + "\n" +
                "清除的采样数据: " + dataCount + "\n" +
                "清除的多线程采样器: " + multiThreadSamplerCount + "\n" +
                "清除的多线程数据: " + multiThreadDataCount + "\n" +
                "清除的分层采样器: " + hierarchicalSamplerCount + "\n" +
                "清除的分层数据: " + hierarchicalDataCount + "\n" +
                "清除的 Tracer: " + tracerCount + "\n" +
                "清除的 Trace 数据: " + traceDataCount + "\n" +
                "清除的 Systrace: " + systraceRunnerCount + "\n" +
                "清除的 Systrace 数据: " + systraceDataCount + "\n" +
                "清除的 Hook: " + hookCount;
    }

    public static Map<Integer, SampleData> getSampleDataMap() {
        return new ConcurrentHashMap<>(sampleDataMap);
    }

    public static Map<Integer, HookData> getHookDataMap() {
        return new ConcurrentHashMap<>(hookDataMap);
    }

    private String handleMultiThread(String[] args) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: performance multithread <subcmd> [args...]";
        }

        String subCmd = args[1];

        return switch (subCmd) {
            case "start" -> handleMultiThreadStart(args);
            case "stop" -> handleMultiThreadStop(args);
            case "report" -> handleMultiThreadReport(args);
            case "export" -> handleMultiThreadExport(args);
            default -> "未知子命令: " + subCmd;
        };
    }

    private String handleMultiThreadStart(String[] args) {
        int sampleRate = 100;
        if (args.length >= 3) {
            try {
                sampleRate = Integer.parseInt(args[2]);
                if (sampleRate <= 0) {
                    return "错误: 采样频率必须大于 0";
                }
                if (sampleRate > 10000) {
                    return "警告: 采样频率过高（" + sampleRate + " Hz），可能影响性能";
                }
            } catch (NumberFormatException e) {
                return "错误: 无效的采样频率: " + args[2];
            }
        }

        int id = nextId.getAndIncrement();
        MultiThreadSampler sampler = new MultiThreadSampler(sampleRate);
        sampler.start();
        multiThreadSamplers.put(id, sampler);

        logger.info("多线程采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        staticLogger.info("多线程采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        
        return "多线程采样器已启动\n" +
                "ID: " + id + "\n" +
                "采样频率: " + sampleRate + " Hz\n" +
                "使用 'performance multithread stop " + id + "' 停止采样\n" +
                "使用 'performance multithread report " + id + "' 查看报告";
    }

    private String handleMultiThreadStop(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance multithread stop <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        MultiThreadSampler sampler = multiThreadSamplers.get(id);
        if (sampler == null) {
            return "错误: 多线程采样器不存在 (ID: " + id + ")";
        }

        sampler.stop();
        long stopTime = sampler.getStopTime();
        int totalSamples = sampler.getTotalSamples();
        Map<String, Map<String, Integer>> report = sampler.getReport();
        Map<String, Integer> threadSampleCounts = sampler.getThreadSampleCounts();

        MultiThreadSampleData data = new MultiThreadSampleData(id, sampler.getSampleRate(),
            sampler.getStartTime(), stopTime, totalSamples, report, threadSampleCounts, sampler.getThreadCount());
        multiThreadSampleDataMap.put(id, data);

        logger.info("多线程采样器已停止 (ID: " + id + ")");
        staticLogger.info("多线程采样器已停止 (ID: " + id + ", 采样次数: " + totalSamples + ")");
        
        return "多线程采样器已停止 (ID: " + id + ")\n" +
                "采样次数: " + totalSamples + "\n" +
                "线程数量: " + sampler.getThreadCount() + "\n" +
                "持续时间: " + data.getDurationString() + "\n" +
                "使用 'performance multithread report " + id + "' 查看报告";
    }

    private String handleMultiThreadReport(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance multithread report <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        MultiThreadSampleData data = multiThreadSampleDataMap.get(id);
        if (data == null) {
            return "错误: 多线程采样数据不存在 (ID: " + id + ")";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== 多线程采样报告 ===\n");
        result.append("ID: ").append(data.id()).append("\n");
        result.append("采样频率: ").append(data.sampleRate()).append(" Hz\n");
        result.append("采样次数: ").append(data.totalSamples()).append("\n");
        result.append("线程数量: ").append(data.threadCount()).append("\n");
        result.append("持续时间: ").append(data.getDurationString()).append("\n");
        result.append("\n");

        for (Map.Entry<String, Map<String, Integer>> threadEntry : data.threadMethodCounts().entrySet()) {
            String threadName = threadEntry.getKey();
            Map<String, Integer> methodCounts = threadEntry.getValue();
            int threadSamples = Objects.requireNonNull(data.threadSampleCounts().getOrDefault(threadName, 0));

            result.append("线程: ").append(threadName).append("\n");
            result.append("  采样次数: ").append(threadSamples).append("\n");
            result.append("  热点方法:\n");

            methodCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .forEach(entry -> {
                    int count = entry.getValue();
                    double percentage = (count * 100.0) / threadSamples;
                    result.append(String.format(
                            Locale.getDefault(),
                            "    %-60s %6d 次 (%5.1f%%)%n",
                        entry.getKey(), count, percentage));
                });

            result.append("\n");
        }

        return result.toString();
    }

    private String handleMultiThreadExport(String[] args) {
        if (args.length < 4) {
            return "错误: 参数不足\n用法: performance multithread export <id> <file>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        String filePath = args[3];
        MultiThreadSampleData data = multiThreadSampleDataMap.get(id);
        if (data == null) {
            return "错误: 多线程采样数据不存在 (ID: " + id + ")";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": ").append(data.id()).append(",\n");
        json.append("  \"sampleRate\": ").append(data.sampleRate()).append(",\n");
        json.append("  \"startTime\": ").append(data.startTime()).append(",\n");
        json.append("  \"stopTime\": ").append(data.stopTime()).append(",\n");
        json.append("  \"totalSamples\": ").append(data.totalSamples()).append(",\n");
        json.append("  \"threadCount\": ").append(data.threadCount()).append(",\n");
        json.append("  \"threads\": {\n");

        boolean firstThread = true;
        for (Map.Entry<String, Map<String, Integer>> threadEntry : data.threadMethodCounts().entrySet()) {
            if (!firstThread) {
                json.append(",\n");
            }
            firstThread = false;
            
            json.append("    \"").append(threadEntry.getKey()).append("\": {\n");
            json.append("      \"sampleCount\": ").append(data.threadSampleCounts().get(threadEntry.getKey())).append(",\n");
            json.append("      \"methods\": {\n");

            boolean firstMethod = true;
            for (Map.Entry<String, Integer> methodEntry : threadEntry.getValue().entrySet()) {
                if (!firstMethod) {
                    json.append(",\n");
                }
                firstMethod = false;
                json.append("        \"").append(methodEntry.getKey()).append("\": ").append(methodEntry.getValue());
            }

            json.append("\n      }\n");
            json.append("    }");
        }

        json.append("\n  }\n");
        json.append("}\n");

        if (!writeToFile(filePath, json.toString())) {
            return "导出多线程采样数据失败: 无法写入文件 " + filePath;
        }

        logger.info("多线程采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("多线程采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        return "多线程采样数据已导出\n" +
                "ID: " + id + "\n" +
                "文件: " + filePath + "\n" +
                "采样次数: " + data.totalSamples();
    }

    private String handleHierarchical(String[] args) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: performance hierarchical <subcmd> [args...]";
        }

        String subCmd = args[1];

        return switch (subCmd) {
            case "start" -> handleHierarchicalStart(args);
            case "stop" -> handleHierarchicalStop(args);
            case "report" -> handleHierarchicalReport(args);
            case "export" -> handleHierarchicalExport(args);
            default -> "未知子命令: " + subCmd;
        };
    }

    private String handleHierarchicalStart(String[] args) {
        int sampleRate = 100;
        if (args.length >= 3) {
            try {
                sampleRate = Integer.parseInt(args[2]);
                if (sampleRate <= 0) {
                    return "错误: 采样频率必须大于 0";
                }
                if (sampleRate > 10000) {
                    return "警告: 采样频率过高（" + sampleRate + " Hz），可能影响性能";
                }
            } catch (NumberFormatException e) {
                return "错误: 无效的采样频率: " + args[2];
            }
        }

        int id = nextId.getAndIncrement();
        HierarchicalSampler sampler = new HierarchicalSampler(sampleRate);
        sampler.start();
        hierarchicalSamplers.put(id, sampler);

        logger.info("分层采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        staticLogger.info("分层采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        
        return "分层采样器已启动\n" +
                "ID: " + id + "\n" +
                "采样频率: " + sampleRate + " Hz\n" +
                "使用 'performance hierarchical stop " + id + "' 停止采样\n" +
                "使用 'performance hierarchical report " + id + "' 查看报告";
    }

    private String handleHierarchicalStop(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance hierarchical stop <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        HierarchicalSampler sampler = hierarchicalSamplers.get(id);
        if (sampler == null) {
            return "错误: 分层采样器不存在 (ID: " + id + ")";
        }

        sampler.stop();
        long stopTime = sampler.getStopTime();
        int totalSamples = sampler.getTotalSamples();
        Map<String, HierarchicalSampler.MethodCallInfo> report = sampler.getReport();
        Map<String, Integer> callerCounts = sampler.getCallerCounts();

        HierarchicalSampleData data = new HierarchicalSampleData(id, sampler.getSampleRate(),
            sampler.getStartTime(), stopTime, totalSamples, report, callerCounts, sampler.getMethodCount());
        hierarchicalSampleDataMap.put(id, data);

        logger.info("分层采样器已停止 (ID: " + id + ")");
        staticLogger.info("分层采样器已停止 (ID: " + id + ", 采样次数: " + totalSamples + ")");
        
        return "分层采样器已停止 (ID: " + id + ")\n" +
                "采样次数: " + totalSamples + "\n" +
                "方法数量: " + sampler.getMethodCount() + "\n" +
                "持续时间: " + data.getDurationString() + "\n" +
                "使用 'performance hierarchical report " + id + "' 查看报告";
    }

    private String handleHierarchicalReport(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance hierarchical report <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        HierarchicalSampleData data = hierarchicalSampleDataMap.get(id);
        if (data == null) {
            return "错误: 分层采样数据不存在 (ID: " + id + ")";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== 分层采样报告 ===\n");
        result.append("ID: ").append(data.id()).append("\n");
        result.append("采样频率: ").append(data.sampleRate()).append(" Hz\n");
        result.append("采样次数: ").append(data.totalSamples()).append("\n");
        result.append("方法数量: ").append(data.methodCount()).append("\n");
        result.append("持续时间: ").append(data.getDurationString()).append("\n");
        result.append("\n");
        result.append("热点方法:\n");

        data.methodCallInfos().entrySet().stream()
            .sorted((a, b) -> b.getValue().getSampleCount() - a.getValue().getSampleCount())
            .limit(20)
            .forEach(entry -> {
                HierarchicalSampler.MethodCallInfo info = entry.getValue();
                int count = info.getSampleCount();
                double percentage = (count * 100.0) / data.totalSamples();
                double avgDepth = info.getAverageDepth();
                
                result.append(String.format(
                        Locale.getDefault(),
                        "  %-60s %6d 次 (%5.1f%%), 平均深度: %.1f%n",
                    info.methodKey, count, percentage, avgDepth));
                
                Map<String, Integer> callers = info.getCallers();
                if (!callers.isEmpty()) {
                    result.append("    调用者:\n");
                    callers.entrySet().stream()
                        .sorted((a, b) -> b.getValue() - a.getValue())
                        .limit(5)
                        .forEach(callerEntry -> {
                            int callerCount = callerEntry.getValue();
                            double callerPercentage = (callerCount * 100.0) / count;
                            result.append(String.format(
                                    Locale.getDefault(),
                                    "      %-60s %6d 次 (%5.1f%%)%n",
                                callerEntry.getKey(), callerCount, callerPercentage));
                        });
                }
                result.append("\n");
            });

        return result.toString();
    }

    private String handleHierarchicalExport(String[] args) {
        if (args.length < 4) {
            return "错误: 参数不足\n用法: performance hierarchical export <id> <file>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        String filePath = args[3];
        HierarchicalSampleData data = hierarchicalSampleDataMap.get(id);
        if (data == null) {
            return "错误: 分层采样数据不存在 (ID: " + id + ")";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": ").append(data.id()).append(",\n");
        json.append("  \"sampleRate\": ").append(data.sampleRate()).append(",\n");
        json.append("  \"startTime\": ").append(data.startTime()).append(",\n");
        json.append("  \"stopTime\": ").append(data.stopTime()).append(",\n");
        json.append("  \"totalSamples\": ").append(data.totalSamples()).append(",\n");
        json.append("  \"methodCount\": ").append(data.methodCount()).append(",\n");
        json.append("  \"methods\": {\n");

        boolean firstMethod = true;
        for (Map.Entry<String, HierarchicalSampler.MethodCallInfo> entry : data.methodCallInfos().entrySet()) {
            if (!firstMethod) {
                json.append(",\n");
            }
            firstMethod = false;
            
            HierarchicalSampler.MethodCallInfo info = entry.getValue();
            json.append("    \"").append(entry.getKey()).append("\": {\n");
            json.append("      \"sampleCount\": ").append(info.getSampleCount()).append(",\n");
            json.append("      \"averageDepth\": ").append(info.getAverageDepth()).append(",\n");
            json.append("      \"callers\": {\n");

            boolean firstCaller = true;
            for (Map.Entry<String, Integer> callerEntry : info.getCallers().entrySet()) {
                if (!firstCaller) {
                    json.append(",\n");
                }
                firstCaller = false;
                json.append("        \"").append(callerEntry.getKey()).append("\": ").append(callerEntry.getValue());
            }

            json.append("\n      }\n");
            json.append("    }");
        }

        json.append("\n  }\n");
        json.append("}\n");

        if (!writeToFile(filePath, json.toString())) {
            return "导出分层采样数据失败: 无法写入文件 " + filePath;
        }

        logger.info("分层采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("分层采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        return "分层采样数据已导出\n" +
                "ID: " + id + "\n" +
                "文件: " + filePath + "\n" +
                "采样次数: " + data.totalSamples();
    }

    private String handleTrace(String[] args) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: performance trace <subcmd> [args...]";
        }

        String subCmd = args[1];

        return switch (subCmd) {
            case "start" -> handleTraceStart(args);
            case "stop" -> handleTraceStop(args);
            case "report" -> handleTraceReport(args);
            case "export" -> handleTraceExport(args);
            default -> "未知子命令: " + subCmd;
        };
    }

    private String handleTraceStart(String[] args) {
        int id = nextId.getAndIncrement();
        Tracer tracer = new Tracer();
        tracer.start();
        tracers.put(id, tracer);

        logger.info("Tracer 已启动 (ID: " + id + ")");
        staticLogger.info("Tracer 已启动 (ID: " + id + ")");
        
        return "Tracer 已启动\n" +
                "ID: " + id + "\n" +
                "使用 'performance trace stop " + id + "' 停止 Trace\n" +
                "使用 'performance trace report " + id + "' 查看报告";
    }

    private String handleTraceStop(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance trace stop <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        Tracer tracer = tracers.get(id);
        if (tracer == null) {
            return "错误: Tracer 不存在 (ID: " + id + ")";
        }

        tracer.stop();
        List<TraceData> traceData = tracer.getTraceData();
        traceDataMap.put(id, traceData);

        logger.info("Tracer 已停止 (ID: " + id + ")");
        staticLogger.info("Tracer 已停止 (ID: " + id + ", Trace 数据: " + traceData.size() + ")");
        
        return "Tracer 已停止 (ID: " + id + ")\n" +
                "Trace 数据: " + traceData.size() + "\n" +
                "使用 'performance trace report " + id + "' 查看报告";
    }

    private String handleTraceReport(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance trace report <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        List<TraceData> traceData = traceDataMap.get(id);
        if (traceData == null || traceData.isEmpty()) {
            return "错误: Trace 数据不存在 (ID: " + id + ")";
        }

        StringBuilder result = new StringBuilder();
        result.append("=== Trace 报告 ===\n");
        result.append("ID: ").append(id).append("\n");
        result.append("Trace 数据: ").append(traceData.size()).append("\n");
        result.append("\n");
        result.append("Trace 段:\n");

        for (TraceData data : traceData) {
            result.append(String.format(
                    Locale.getDefault(),
                    "  %-40s %s (线程: %s, ID: %d)%n",
                    data.name(), data.getDurationString(), data.threadName(), data.threadId()));
        }

        result.append("\n");
        result.append("说明:\n");
        result.append("  - Trace 数据来自应用的 Trace.beginSection() 和 Trace.endSection() 调用\n");
        result.append("  - 需要应用代码支持才能收集 Trace 数据\n");
        result.append("  - 可以使用 Systrace 工具查看详细的 Trace 信息\n");

        return result.toString();
    }

    private String handleTraceExport(String[] args) {
        if (args.length < 4) {
            return "错误: 参数不足\n用法: performance trace export <id> <file>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        String filePath = args[3];
        List<TraceData> traceData = traceDataMap.get(id);
        if (traceData == null || traceData.isEmpty()) {
            return "错误: Trace 数据不存在 (ID: " + id + ")";
        }

        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": ").append(id).append(",\n");
        json.append("  \"traceCount\": ").append(traceData.size()).append(",\n");
        json.append("  \"traces\": [\n");

        boolean first = true;
        for (TraceData data : traceData) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            
            json.append("    {\n");
            json.append("      \"name\": \"").append(data.name()).append("\",\n");
            json.append("      \"startTime\": ").append(data.startTime()).append(",\n");
            json.append("      \"duration\": ").append(data.duration()).append(",\n");
            json.append("      \"threadId\": ").append(data.threadId()).append(",\n");
            json.append("      \"threadName\": \"").append(data.threadName()).append("\"\n");
            json.append("    }");
        }

        json.append("\n  ]\n");
        json.append("}\n");

        if (!writeToFile(filePath, json.toString())) {
            return "导出 Trace 数据失败: 无法写入文件 " + filePath;
        }

        logger.info("Trace 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("Trace 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        return "Trace 数据已导出\n" +
                "ID: " + id + "\n" +
                "文件: " + filePath + "\n" +
                "Trace 数据: " + traceData.size();
    }

    private String handleSystrace(String[] args) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: performance systrace <subcmd> [args...]";
        }

        String subCmd = args[1];

        return switch (subCmd) {
            case "start" -> handleSystraceStart(args);
            case "stop" -> handleSystraceStop(args);
            case "report" -> handleSystraceReport(args);
            case "export" -> handleSystraceExport(args);
            default -> "未知子命令: " + subCmd;
        };
    }

    private String handleSystraceStart(String[] args) {
        int id = nextId.getAndIncrement();
        int duration = 10;
        String[] categories = null;

        if (args.length >= 3) {
            try {
                duration = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                return "错误: 无效的持续时间: " + args[2];
            }
        }

        if (args.length >= 4) {
            categories = new String[args.length - 3];
            System.arraycopy(args, 3, categories, 0, categories.length);
        }

        String outputDir = "/data/local/tmp/systrace";
        SystraceRunner runner = new SystraceRunner(outputDir);
        runner.start(duration, categories);
        systraceRunners.put(id, runner);

        logger.info("Systrace 已启动 (ID: " + id + ", 持续时间: " + duration + " 秒)");
        staticLogger.info("Systrace 已启动 (ID: " + id + ", 持续时间: " + duration + " 秒)");
        
        return "Systrace 已启动\n" +
                "ID: " + id + "\n" +
                "持续时间: " + duration + " 秒\n" +
                "输出目录: " + outputDir + "\n" +
                "使用 'performance systrace stop " + id + "' 停止 Systrace\n" +
                "使用 'performance systrace report " + id + "' 查看报告";
    }

    private String handleSystraceStop(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance systrace stop <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        SystraceRunner runner = systraceRunners.get(id);
        if (runner == null) {
            return "错误: Systrace 不存在 (ID: " + id + ")";
        }

        runner.stop();
        String outputFile = runner.getOutputFile();

        if (outputFile != null && !outputFile.isEmpty()) {
            try {
                SystraceData data = SystraceParser.parse(outputFile);
                systraceDataMap.put(id, data);

                logger.info("Systrace 已停止 (ID: " + id + ", 文件: " + outputFile + ")");
                staticLogger.info("Systrace 已停止 (ID: " + id + ", 文件: " + outputFile + ")");
                
                return "Systrace 已停止 (ID: " + id + ")\n" +
                        "输出文件: " + outputFile + "\n" +
                        "使用 'performance systrace report " + id + "' 查看报告";
            } catch (Exception e) {
                logger.error("解析 Systrace 文件失败", e);
                return "Systrace 已停止，但解析输出文件失败\n" +
                        "ID: " + id + "\n" +
                        "输出文件: " + outputFile + "\n" +
                        "错误: " + e.getMessage();
            }
        } else {
            logger.info("Systrace 已停止 (ID: " + id + ")");
            staticLogger.info("Systrace 已停止 (ID: " + id + ")");
            
            return "Systrace 已停止 (ID: " + id + ")";
        }
    }

    private String handleSystraceReport(String[] args) {
        if (args.length < 3) {
            return "错误: 参数不足\n用法: performance systrace report <id>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        SystraceData data = systraceDataMap.get(id);
        if (data == null) {
            return "错误: Systrace 数据不存在 (ID: " + id + ")";
        }

        return SystraceParser.generateReport(data);
    }

    private String handleSystraceExport(String[] args) {
        if (args.length < 4) {
            return "错误: 参数不足\n用法: performance systrace export <id> <file>";
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            return "错误: 无效的 ID: " + args[2];
        }

        String filePath = args[3];
        SystraceData data = systraceDataMap.get(id);
        if (data == null) {
            return "错误: Systrace 数据不存在 (ID: " + id + ")";
        }

        String report = SystraceParser.generateReport(data);

        if (!writeToFile(filePath, report)) {
            return "导出 Systrace 数据失败: 无法写入文件 " + filePath;
        }

        logger.info("Systrace 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("Systrace 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        return "Systrace 数据已导出\n" +
                "ID: " + id + "\n" +
                "文件: " + filePath + "\n" +
                "原始文件: " + data.file();
    }

    private boolean writeToFile(String filePath, String content) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            
            if (parentDir != null && !parentDir.exists()) {
                IOManager.createDirectory(parentDir.getAbsolutePath());
            }
            
            IOManager.writeFile(filePath, content);
            
            return true;
        } catch (IOException e) {
            logger.error("写入文件失败: " + filePath, e);
            return false;
        }
    }
}
