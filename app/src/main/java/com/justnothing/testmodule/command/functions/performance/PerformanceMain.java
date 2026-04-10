package com.justnothing.testmodule.command.functions.performance;

import static com.justnothing.testmodule.constants.CommandServer.CMD_PERFORMANCE_VER;

import androidx.annotation.NonNull;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.functions.intercept.PerformanceInterceptTask;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.HierarchicalSampler;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.MultiThreadSampler;
import com.justnothing.testmodule.command.functions.performance.sampler.SimpleSampleData;
import com.justnothing.testmodule.command.functions.performance.sampler.SimpleSampler;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceData;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceParser;
import com.justnothing.testmodule.command.functions.performance.systrace.SystraceRunner;
import com.justnothing.testmodule.command.functions.performance.trace.TraceData;
import com.justnothing.testmodule.command.functions.performance.trace.Tracer;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.functions.Logger;
import com.justnothing.testmodule.utils.reflect.SignatureUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class PerformanceMain extends CommandBase {
    private static final Logger staticLogger = Logger.getLoggerForName("Performance");
    private static final Map<Integer, SimpleSampler> samplers = new ConcurrentHashMap<>();
    private static final Map<Integer, SimpleSampleData> sampleDataMap = new ConcurrentHashMap<>();
    private static final Map<Integer, MultiThreadSampler> multiThreadSamplers = new ConcurrentHashMap<>();
    private static final Map<Integer, MultiThreadSampleData> multiThreadSampleDataMap = new ConcurrentHashMap<>();
    private static final Map<Integer, HierarchicalSampler> hierarchicalSamplers = new ConcurrentHashMap<>();
    private static final Map<Integer, HierarchicalSampleData> hierarchicalSampleDataMap = new ConcurrentHashMap<>();
    private static final Map<Integer, Tracer> tracers = new ConcurrentHashMap<>();
    private static final Map<Integer, List<TraceData>> traceDataMap = new ConcurrentHashMap<>();
    private static final Map<Integer, SystraceRunner> systraceRunners = new ConcurrentHashMap<>();
    private static final Map<Integer, SystraceData> systraceDataMap = new ConcurrentHashMap<>();
    private static final AtomicInteger nextId = new AtomicInteger(1);

    private CommandExecutor.CmdExecContext execContext;

    private void out(Object obj, byte color) {
        if (execContext != null) {
            execContext.print(obj, color);
        }
    }

    private void outln(Object obj, byte color) {
        if (execContext != null) {
            execContext.println(obj, color);
        }
    }

    public PerformanceMain() {
        super("Performance");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: performance <subcmd> [args...]
                
                性能分析命令，支持多种分析方式。
                
                子命令:
                    sample start [rate] [-e <exclude>]       - 开始采样
                    sample stop <id>                        - 停止采样
                    sample report [id]                       - 查看采样报告
                    sample export <id> <file>               - 导出采样数据
                
                    multithread start [rate] [-e <exclude>]  - 开始多线程采样
                    multithread stop <id>                   - 停止多线程采样
                    multithread report [id]                 - 查看多线程采样报告
                    multithread export <id> <file>          - 导出多线程采样数据
                
                    hierarchical start [rate] [-e <exclude>] - 开始分层采样
                    hierarchical stop <id>                  - 停止分层采样
                    hierarchical report [id]                - 查看分层采样报告
                    hierarchical export <id> <file>         - 导出分层采样数据
                
                    trace start                             - 开始 Trace
                    trace stop <id>                         - 停止 Trace
                    trace report [id]                       - 查看 Trace 报告
                    trace export <id> <file>                - 导出 Trace 数据
                
                    systrace start [duration] [categories]  - 开始 Systrace
                    systrace stop <id>                     - 停止 Systrace
                    systrace report [id]                    - 查看 Systrace 报告
                    systrace export <id> <file>             - 导出 Systrace 数据
                
                    hook <class> [method] [sig]             - Hook 方法
                    hook stop <id>                          - 停止 Hook
                    hook report [id]                        - 查看 Hook 报告
                    hook export <id> <file>                 - 导出 Hook 数据
                
                    hook start <class> [method] [sig]       - Hook 方法 (显式start)
                
                    list                                    - 列出所有任务
                    clear                                   - 清除所有任务
                
                选项:
                    rate   - 采样频率（Hz），默认 100
                    class  - 类名
                    method - 方法名（可选）
                    sig    - 方法签名（可选）

                关于sig (signature)参数:
                    可以是用逗号分割的类名列表，如 "String,int" 表示(String, int)参数的方法
                    也可以是JVM内部格式的签名，如 "(Ljava/lang/String;I;)V" 也是一样
                    （不过因为理论上不会有同参数列表不同返回类型的情况，所以返回类型的解析不会有实际作用）
                
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
    public void runMain(CommandExecutor.CmdExecContext context) {
        this.execContext = context;
        String[] args = context.args();
        
        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            return;
        }

        String subCommand = args[0];

        try {
            switch (subCommand) {
                case "sample" -> handleSample(args);
                case "multithread" -> handleMultiThread(args);
                case "hierarchical" -> handleHierarchical(args);
                case "trace" -> handleTrace(args);
                case "systrace" -> handleSystrace(args);
                case "hook" -> handleHook(args, context.classLoader());
                case "list" -> handleList();
                case "clear" -> handleClear();
                default -> {
                    outln("错误: 未知子命令", Colors.RED);
                    outln("用法: performance <subcmd> [args...]", Colors.GRAY);
                    outln("可用子命令:", Colors.GRAY);
                    outln(getHelpText(), Colors.WHITE);
                }
            }

        } catch (Exception e) {
            CommandExceptionHandler.handleException("performance " + subCommand, e, context, "执行performance子命令时遇到错误");
        } finally {
            this.execContext = null;
        }
    }

    private void handleSample(String[] args) throws JSONException {
        if (args.length < 2) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance sample <subcmd> [args...]", Colors.GRAY);
            return;
        }

        String sampleSubCmd = args[1];

        switch (sampleSubCmd) {
            case "start" -> handleSampleStart(args);
            case "stop" -> handleSampleStop(args);
            case "report" -> handleSampleReport(args);
            case "export" -> handleSampleExport(args);
            default -> outln("未知子命令: " + sampleSubCmd, Colors.RED);
        }
    }

    private void handleSampleStart(String[] args) {
        int sampleRate = 100;
        if (args.length >= 3) {
            try {
                sampleRate = Integer.parseInt(args[2]);
                if (sampleRate <= 0) {
                    outln("错误: 采样频率必须大于 0", Colors.RED);
                    return;
                }
                if (sampleRate > 10000) {
                    outln("警告: 采样频率过高（" + sampleRate + " Hz），可能影响性能", Colors.YELLOW);
                }
            } catch (NumberFormatException e) {
                outln("错误: 无效的采样频率: " + args[2], Colors.RED);
                return;
            }
        }

        int id = nextId.getAndIncrement();
        SimpleSampler sampler = new SimpleSampler(sampleRate);
        sampler.start();
        samplers.put(id, sampler);

        logger.info("采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        staticLogger.info("采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        
        outln("采样器已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("采样频率: ", Colors.CYAN);
        outln(sampleRate + " Hz", Colors.YELLOW);
        outln("使用 'performance sample stop " + id + "' 停止采样", Colors.GRAY);
        outln("使用 'performance sample report " + id + "' 查看报告", Colors.GRAY);
    }

    private void handleSampleStop(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance sample stop <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        SimpleSampler sampler = samplers.get(id);
        if (sampler == null) {
            outln("错误: 采样器不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        sampler.stop();
        long stopTime = sampler.getStopTime();
        int totalSamples = sampler.getTotalSamples();
        Map<String, Integer> report = sampler.getReport();

        SimpleSampleData data = new SimpleSampleData(id, sampler.getSampleRate(), 
            sampler.getStartTime(), stopTime, totalSamples, report);
        sampleDataMap.put(id, data);

        logger.info("采样器已停止 (ID: " + id + ")");
        staticLogger.info("采样器已停止 (ID: " + id + ", 采样次数: " + totalSamples + ")");
        
        outln("采样器已停止", Colors.YELLOW);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("采样次数: ", Colors.CYAN);
        outln(String.valueOf(totalSamples), Colors.YELLOW);
        out("持续时间: ", Colors.CYAN);
        outln(data.getDurationString(), Colors.YELLOW);
        outln("使用 'performance sample report " + id + "' 查看报告", Colors.GRAY);
    }

    private void handleSampleReport(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance sample report <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        SimpleSampleData data = sampleDataMap.get(id);
        if (data == null) {
            outln("错误: 采样数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        if (data.methodCounts().isEmpty()) {
            outln("没有采样数据", Colors.GRAY);
            return;
        }

        outln("=== 采样报告 ===", Colors.CYAN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(data.id()), Colors.YELLOW);
        out("采样频率: ", Colors.CYAN);
        outln(data.sampleRate() + " Hz", Colors.YELLOW);
        out("采样次数: ", Colors.CYAN);
        outln(String.valueOf(data.totalSamples()), Colors.YELLOW);
        out("持续时间: ", Colors.CYAN);
        outln(data.getDurationString(), Colors.YELLOW);
        outln("", Colors.WHITE);
        outln("热点方法:", Colors.CYAN);

        data.methodCounts().entrySet().stream()
            .sorted((a, b) -> b.getValue() - a.getValue())
            .forEach(entry -> {
                int count = entry.getValue();
                double percentage = (count * 100.0) / data.totalSamples();
                out(String.format(Locale.getDefault(),
                        "  %-60s ", entry.getKey()), Colors.WHITE);
                out(String.format(Locale.getDefault(), "%6d 次 ", count), Colors.YELLOW);
                outln(String.format(Locale.getDefault(), "(%5.1f%%)", percentage), Colors.GREEN);
            });

        outln("", Colors.WHITE);
        outln("建议: 使用 'performance hook' 精确分析热点方法", Colors.GRAY);
    }

    private void handleSampleExport(String[] args) throws JSONException {
        if (args.length < 4) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance sample export <id> <file>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        String filePath = args[3];
        SimpleSampleData data = sampleDataMap.get(id);
        if (data == null) {
            outln("错误: 采样数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        JSONObject json = new JSONObject();
        json.put("id", data.id());
        json.put("sampleRate", data.sampleRate());
        json.put("startTime", data.startTime());
        json.put("stopTime", data.stopTime());
        json.put("totalSamples", data.totalSamples());
        json.put("duration", data.getDuration());
        
        JSONObject methodCounts = new JSONObject();
        for (Map.Entry<String, Integer> entry : data.methodCounts().entrySet()) {
            methodCounts.put(entry.getKey(), entry.getValue());
        }
        json.put("methodCounts", methodCounts);

        if (!writeToFile(filePath, json.toString(2))) {
            outln("导出采样数据失败: 无法写入文件 " + filePath, Colors.RED);
            return;
        }

        logger.info("采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        outln("采样数据已导出", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("文件: ", Colors.CYAN);
        outln(filePath, Colors.GREEN);
        out("采样次数: ", Colors.CYAN);
        outln(String.valueOf(data.totalSamples()), Colors.YELLOW);
    }

    private void handleHook(String[] args, ClassLoader classLoader) throws JSONException {
        if (args.length < 2) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance hook <subcmd> [args...]", Colors.GRAY);
            return;
        }

        String hookSubCmd = args[1];

        switch (hookSubCmd) {
            case "start" -> handleHookStart(args, classLoader);
            case "stop" -> handleHookStop(args);
            case "report" -> handleHookReport(args);
            case "export" -> handleHookExport(args);
            default -> handleHookMethod(args, classLoader);
        }
    }

    private void handleHookMethod(String[] args, ClassLoader classLoader) {
        if (args.length < 2) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance hook <class> [method] [sig]", Colors.GRAY);
            return;
        }

        String className = args[1];
        String methodName = args.length >= 3 ? args[2] : null;
        String methodSig = args.length >= 4 ? args[3] : null;

        try {
            int id = PerformanceManager.getInstance().addPerformanceHook(
                    className, 
                    methodName != null ? methodName : "*", 
                    methodSig, 
                    classLoader
            );

            logger.info("Hook 已添加 (ID: " + id + ", 类: " + className + ", 方法: " + methodName + ")");
            staticLogger.info("Hook 已添加 (ID: " + id + ", 类: " + className + ", 方法: " + methodName + ")");

            outln("Hook 已添加", Colors.GREEN);
            out("ID: ", Colors.CYAN);
            outln(String.valueOf(id), Colors.YELLOW);
            out("类: ", Colors.CYAN);
            outln(className, Colors.GREEN);
            out("方法: ", Colors.CYAN);
            outln(methodName != null ? methodName : "所有方法", Colors.GREEN);
            outln("使用 'performance hook stop " + id + "' 停止 Hook", Colors.GRAY);
            outln("使用 'performance hook report " + id + "' 查看报告", Colors.GRAY);
        } catch (Exception e) {
            outln("错误: " + e.getMessage(), Colors.RED);
            logger.error("添加 Hook 失败", e);
        }
    }

    private void handleHookStart(String[] args, ClassLoader classLoader) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance hook start <class> [method] [sig]", Colors.GRAY);
            return;
        }

        String[] hookArgs = new String[args.length - 1];
        hookArgs[0] = args[0];
        System.arraycopy(args, 2, hookArgs, 1, args.length - 2);
        
        handleHookMethod(hookArgs, classLoader);
    }

    private void handleHookStop(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance hook stop <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        if (!PerformanceManager.getInstance().hasPerformanceHook(id)) {
            outln("错误: Hook 不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        PerformanceManager.getInstance().stopPerformanceHook(id);

        logger.info("Hook 已停止 (ID: " + id + ")");
        staticLogger.info("Hook 已停止 (ID: " + id + ")");

        outln("Hook 已停止", Colors.YELLOW);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
    }

    private void handleHookReport(String[] args) {
        int id;
        if (args.length >= 3) {
            try {
                id = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                outln("错误: 无效的 ID: " + args[2], Colors.RED);
                return;
            }
            
            PerformanceInterceptTask.PerformanceStats stats = PerformanceManager.getInstance().getStats(id);
            if (stats == null) {
                outln("错误: Hook 数据不存在 (ID: " + id + ")", Colors.RED);
                return;
            }

            outln("=== Hook 报告 ===", Colors.CYAN);
            out("ID: ", Colors.CYAN);
            outln(String.valueOf(stats.id()), Colors.YELLOW);
            out("类: ", Colors.CYAN);
            outln(stats.className(), Colors.GREEN);
            out("方法: ", Colors.CYAN);
            outln(stats.methodName(), Colors.GREEN);
            out("签名: ", Colors.CYAN);
            outln(stats.signature() != null ? stats.signature() : "无", Colors.GRAY);
            out("调用次数: ", Colors.CYAN);
            outln(String.valueOf(stats.callCount()), Colors.YELLOW);
            out("总耗时: ", Colors.CYAN);
            outln(formatDurationNs(stats.totalDurationNs()), Colors.YELLOW);
            out("平均耗时: ", Colors.CYAN);
            outln(formatDurationNs((long) stats.avgDurationNs()), Colors.YELLOW);
            out("最小耗时: ", Colors.CYAN);
            outln(formatDurationNs(stats.minDurationNs()), Colors.GREEN);
            out("最大耗时: ", Colors.CYAN);
            outln(formatDurationNs(stats.maxDurationNs()), Colors.RED);
            out("监控时长: ", Colors.CYAN);
            outln(stats.getDurationString(), Colors.GRAY);
            outln("", Colors.WHITE);
            outln("性能建议:", Colors.CYAN);
            
            if (stats.avgDurationNs() > 1_000_000) {
                outln("  - 平均耗时较长（>1ms），建议优化", Colors.ORANGE);
            }
            if (stats.maxDurationNs() > 10_000_000) {
                outln("  - 最大耗时较长（>10ms），可能存在性能瓶颈", Colors.ORANGE);
            }
            if (stats.callCount() > 10000) {
                outln("  - 调用次数较多，考虑缓存或优化算法", Colors.ORANGE);
            }
        } else {
            List<PerformanceInterceptTask> hooks = PerformanceManager.getInstance().listPerformanceHooks();
            if (hooks.isEmpty()) {
                outln("没有 Hook 数据", Colors.GRAY);
                return;
            }

            outln("=== 所有 Hook 报告 ===", Colors.CYAN);
            out("Hook 数量: ", Colors.CYAN);
            outln(String.valueOf(hooks.size()), Colors.YELLOW);
            outln("", Colors.WHITE);

            for (PerformanceInterceptTask task : hooks) {
                PerformanceInterceptTask.PerformanceStats stats = task.getStats();
                out("ID: ", Colors.CYAN);
                outln(String.valueOf(stats.id()), Colors.YELLOW);
                out("  类: ", Colors.CYAN);
                outln(stats.className(), Colors.GREEN);
                out("  方法: ", Colors.CYAN);
                outln(stats.methodName(), Colors.GREEN);
                out("  调用次数: ", Colors.CYAN);
                outln(String.valueOf(stats.callCount()), Colors.YELLOW);
                out("  平均耗时: ", Colors.CYAN);
                outln(formatDurationNs((long) stats.avgDurationNs()), Colors.YELLOW);
                outln("", Colors.WHITE);
            }
        }
    }

    private String formatDurationNs(long durationNs) {
        if (durationNs < 1000) {
            return durationNs + "ns";
        } else if (durationNs < 1_000_000) {
            return String.format(Locale.getDefault(), "%.2fus", durationNs / 1000.0);
        } else if (durationNs < 1_000_000_000) {
            return String.format(Locale.getDefault(), "%.2fms", durationNs / 1_000_000.0);
        } else {
            return String.format(Locale.getDefault(), "%.2fs", durationNs / 1_000_000_000.0);
        }
    }

    private void handleHookExport(String[] args) throws JSONException {
        if (args.length < 4) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance hook export <id> <file>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        String filePath = args[3];
        PerformanceInterceptTask.PerformanceStats stats = PerformanceManager.getInstance().getStats(id);
        if (stats == null) {
            outln("错误: Hook 数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        JSONObject json = new JSONObject();
        json.put("id", stats.id());
        json.put("className", stats.className());
        json.put("methodName", stats.methodName());
        json.put("signature", stats.signature());
        json.put("startTime", stats.startTime());
        json.put("stopTime", stats.stopTime());
        json.put("callCount", stats.callCount());
        json.put("totalDurationNs", stats.totalDurationNs());
        json.put("minDurationNs", stats.minDurationNs());
        json.put("maxDurationNs", stats.maxDurationNs());
        json.put("averageDurationNs", stats.avgDurationNs());
        json.put("totalDurationMs", stats.getTotalDurationMs());
        json.put("averageDurationMs", stats.getAvgDurationMs());

        if (!writeToFile(filePath, json.toString(2))) {
            outln("导出 Hook 数据失败: 无法写入文件 " + filePath, Colors.RED);
            return;
        }

        logger.info("Hook 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("Hook 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        outln("Hook 数据已导出", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("文件: ", Colors.CYAN);
        outln(filePath, Colors.GREEN);
        out("调用次数: ", Colors.CYAN);
        outln(String.valueOf(stats.callCount()), Colors.YELLOW);
    }

    private Class<?>[] parseMethodSignature(String sig) {
        if (sig == null || sig.isEmpty()) {
            return new Class<?>[0];
        }
        try {
            return SignatureUtils.parseParamList(sig);
        } catch (Exception e) {
            logger.warn("解析方法签名失败: " + sig + ", " + e.getMessage());
            return new Class<?>[0];
        }
    }

    private void handleList() {
        outln("=== 性能分析任务列表 ===", Colors.CYAN);
        outln("", Colors.WHITE);

        if (!samplers.isEmpty()) {
            outln("运行中的采样器:", Colors.CYAN);
            for (Map.Entry<Integer, SimpleSampler> entry : samplers.entrySet()) {
                SimpleSampler sampler = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: 单线程采样", Colors.WHITE);
                outln("    状态: 运行中", Colors.GREEN);
                out("    采样频率: ", Colors.CYAN);
                outln(sampler.getSampleRate() + " Hz", Colors.YELLOW);
                out("    采样次数: ", Colors.CYAN);
                outln(String.valueOf(sampler.getTotalSamples()), Colors.YELLOW);
                out("    开始时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(sampler.getStartTime())), Colors.GRAY);
                outln("", Colors.WHITE);
            }
        }

        if (!multiThreadSamplers.isEmpty()) {
            outln("运行中的多线程采样器:", Colors.CYAN);
            for (Map.Entry<Integer, MultiThreadSampler> entry : multiThreadSamplers.entrySet()) {
                MultiThreadSampler sampler = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: 多线程采样", Colors.WHITE);
                outln("    状态: 运行中", Colors.GREEN);
                out("    采样频率: ", Colors.CYAN);
                outln(sampler.getSampleRate() + " Hz", Colors.YELLOW);
                out("    采样次数: ", Colors.CYAN);
                outln(String.valueOf(sampler.getTotalSamples()), Colors.YELLOW);
                out("    线程数量: ", Colors.CYAN);
                outln(String.valueOf(sampler.getThreadCount()), Colors.YELLOW);
                out("    开始时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(sampler.getStartTime())), Colors.GRAY);
                outln("", Colors.WHITE);
            }
        }

        if (!hierarchicalSamplers.isEmpty()) {
            outln("运行中的分层采样器:", Colors.CYAN);
            for (Map.Entry<Integer, HierarchicalSampler> entry : hierarchicalSamplers.entrySet()) {
                HierarchicalSampler sampler = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: 分层采样", Colors.WHITE);
                outln("    状态: 运行中", Colors.GREEN);
                out("    采样频率: ", Colors.CYAN);
                outln(sampler.getSampleRate() + " Hz", Colors.YELLOW);
                out("    采样次数: ", Colors.CYAN);
                outln(String.valueOf(sampler.getTotalSamples()), Colors.YELLOW);
                out("    方法数量: ", Colors.CYAN);
                outln(String.valueOf(sampler.getMethodCount()), Colors.YELLOW);
                out("    开始时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(sampler.getStartTime())), Colors.GRAY);
                outln("", Colors.WHITE);
            }
        }

        if (!tracers.isEmpty()) {
            outln("运行中的 Tracer:", Colors.CYAN);
            for (Map.Entry<Integer, Tracer> entry : tracers.entrySet()) {
                Tracer tracer = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: Trace", Colors.WHITE);
                outln("    状态: 运行中", Colors.GREEN);
                out("    Trace 数据: ", Colors.CYAN);
                outln(String.valueOf(tracer.getSectionCount()), Colors.YELLOW);
                out("    开始时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(tracer.getStartTime())), Colors.GRAY);
                outln("", Colors.WHITE);
            }
        }

        if (!sampleDataMap.isEmpty()) {
            outln("已完成的采样数据:", Colors.CYAN);
            for (Map.Entry<Integer, SimpleSampleData> entry : sampleDataMap.entrySet()) {
                SimpleSampleData data = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: 单线程采样", Colors.WHITE);
                outln("    状态: 已完成", Colors.GRAY);
                out("    采样频率: ", Colors.CYAN);
                outln(data.sampleRate() + " Hz", Colors.YELLOW);
                out("    采样次数: ", Colors.CYAN);
                outln(String.valueOf(data.totalSamples()), Colors.YELLOW);
                out("    持续时间: ", Colors.CYAN);
                outln(data.getDurationString(), Colors.YELLOW);
                out("    开始时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(data.startTime())), Colors.GRAY);
                out("    结束时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(data.stopTime())), Colors.GRAY);
                outln("", Colors.WHITE);
            }
        }

        if (!multiThreadSampleDataMap.isEmpty()) {
            outln("已完成的多线程采样数据:", Colors.CYAN);
            for (Map.Entry<Integer, MultiThreadSampleData> entry : multiThreadSampleDataMap.entrySet()) {
                MultiThreadSampleData data = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: 多线程采样", Colors.WHITE);
                outln("    状态: 已完成", Colors.GRAY);
                out("    采样频率: ", Colors.CYAN);
                outln(data.sampleRate() + " Hz", Colors.YELLOW);
                out("    采样次数: ", Colors.CYAN);
                outln(String.valueOf(data.totalSamples()), Colors.YELLOW);
                out("    线程数量: ", Colors.CYAN);
                outln(String.valueOf(data.threadCount()), Colors.YELLOW);
                out("    持续时间: ", Colors.CYAN);
                outln(data.getDurationString(), Colors.YELLOW);
                out("    开始时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(data.startTime())), Colors.GRAY);
                out("    结束时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(data.stopTime())), Colors.GRAY);
                outln("", Colors.WHITE);
            }
        }

        if (!hierarchicalSampleDataMap.isEmpty()) {
            outln("已完成的分层采样数据:", Colors.CYAN);
            for (Map.Entry<Integer, HierarchicalSampleData> entry : hierarchicalSampleDataMap.entrySet()) {
                HierarchicalSampleData data = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: 分层采样", Colors.WHITE);
                outln("    状态: 已完成", Colors.GRAY);
                out("    采样频率: ", Colors.CYAN);
                outln(data.sampleRate() + " Hz", Colors.YELLOW);
                out("    采样次数: ", Colors.CYAN);
                outln(String.valueOf(data.totalSamples()), Colors.YELLOW);
                out("    方法数量: ", Colors.CYAN);
                outln(String.valueOf(data.methodCount()), Colors.YELLOW);
                out("    持续时间: ", Colors.CYAN);
                outln(data.getDurationString(), Colors.YELLOW);
                out("    开始时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(data.startTime())), Colors.GRAY);
                out("    结束时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(data.stopTime())), Colors.GRAY);
                outln("", Colors.WHITE);
            }
        }

        if (!traceDataMap.isEmpty()) {
            outln("已完成的 Trace 数据:", Colors.CYAN);
            for (Map.Entry<Integer, List<TraceData>> entry : traceDataMap.entrySet()) {
                List<TraceData> data = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: Trace", Colors.WHITE);
                outln("    状态: 已完成", Colors.GRAY);
                out("    Trace 数据: ", Colors.CYAN);
                outln(String.valueOf(data.size()), Colors.YELLOW);
                outln("", Colors.WHITE);
            }
        }

        if (!systraceRunners.isEmpty()) {
            outln("运行中的 Systrace:", Colors.CYAN);
            for (Map.Entry<Integer, SystraceRunner> entry : systraceRunners.entrySet()) {
                SystraceRunner runner = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: Systrace", Colors.WHITE);
                outln("    状态: 运行中", Colors.GREEN);
                out("    持续时间: ", Colors.CYAN);
                outln(runner.getDuration() / 1000.0 + " 秒", Colors.YELLOW);
                out("    开始时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(runner.getStartTime())), Colors.GRAY);
                outln("", Colors.WHITE);
            }
        }

        if (!systraceDataMap.isEmpty()) {
            outln("已完成的 Systrace 数据:", Colors.CYAN);
            for (Map.Entry<Integer, SystraceData> entry : systraceDataMap.entrySet()) {
                SystraceData data = entry.getValue();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(entry.getKey()), Colors.YELLOW);
                outln("    类型: Systrace", Colors.WHITE);
                outln("    状态: 已完成", Colors.GRAY);
                out("    文件: ", Colors.CYAN);
                outln(data.file(), Colors.GREEN);
                out("    持续时间: ", Colors.CYAN);
                outln(data.duration() / 1000.0 + " 秒", Colors.YELLOW);
                outln("", Colors.WHITE);
            }
        }

        if (PerformanceManager.getInstance().getPerformanceHookCount() > 0) {
            outln("运行中的 Hook:", Colors.CYAN);
            for (PerformanceInterceptTask task : PerformanceManager.getInstance().listPerformanceHooks()) {
                PerformanceInterceptTask.PerformanceStats stats = task.getStats();
                out("  ID: ", Colors.CYAN);
                outln(String.valueOf(stats.id()), Colors.YELLOW);
                out("    状态: ", Colors.CYAN);
                outln(task.isRunning() ? "运行中" : "已停止", task.isRunning() ? Colors.GREEN : Colors.GRAY);
                out("    类: ", Colors.CYAN);
                outln(stats.className(), Colors.GREEN);
                out("    方法: ", Colors.CYAN);
                outln(stats.methodName(), Colors.GREEN);
                out("    调用次数: ", Colors.CYAN);
                outln(String.valueOf(stats.callCount()), Colors.YELLOW);
                out("    开始时间: ", Colors.CYAN);
                outln(String.valueOf(new Date(stats.startTime())), Colors.GRAY);
                outln("", Colors.WHITE);
            }
        }

        if (samplers.isEmpty() && multiThreadSamplers.isEmpty() && hierarchicalSamplers.isEmpty() 
                && tracers.isEmpty() && systraceRunners.isEmpty() && sampleDataMap.isEmpty()
                && multiThreadSampleDataMap.isEmpty() && hierarchicalSampleDataMap.isEmpty()
                && traceDataMap.isEmpty() && systraceDataMap.isEmpty() && PerformanceManager.getInstance().getPerformanceHookCount() == 0) {
            outln("没有运行中的性能分析任务", Colors.GRAY);
        }
    }

    private void handleClear() {
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
        int hookCount = PerformanceManager.getInstance().getPerformanceHookCount();

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
        PerformanceManager.getInstance().clearAll();
        nextId.set(1);

        logger.info("已清除所有性能分析任务");
        staticLogger.info("已清除所有性能分析任务 (采样器: " + samplerCount + ", 数据: " + dataCount + 
            ", 多线程采样器: " + multiThreadSamplerCount + ", 多线程数据: " + multiThreadDataCount +
            ", 分层采样器: " + hierarchicalSamplerCount + ", 分层数据: " + hierarchicalDataCount +
            ", Tracer: " + tracerCount + ", Trace 数据: " + traceDataCount +
            ", Systrace: " + systraceRunnerCount + ", Systrace 数据: " + systraceDataCount +
            ", Hook: " + hookCount + ")");

        outln("已清除所有性能分析任务", Colors.GREEN);
        out("清除的采样器: ", Colors.CYAN);
        outln(String.valueOf(samplerCount), Colors.YELLOW);
        out("清除的采样数据: ", Colors.CYAN);
        outln(String.valueOf(dataCount), Colors.YELLOW);
        out("清除的多线程采样器: ", Colors.CYAN);
        outln(String.valueOf(multiThreadSamplerCount), Colors.YELLOW);
        out("清除的多线程数据: ", Colors.CYAN);
        outln(String.valueOf(multiThreadDataCount), Colors.YELLOW);
        out("清除的分层采样器: ", Colors.CYAN);
        outln(String.valueOf(hierarchicalSamplerCount), Colors.YELLOW);
        out("清除的分层数据: ", Colors.CYAN);
        outln(String.valueOf(hierarchicalDataCount), Colors.YELLOW);
        out("清除的 Tracer: ", Colors.CYAN);
        outln(String.valueOf(tracerCount), Colors.YELLOW);
        out("清除的 Trace 数据: ", Colors.CYAN);
        outln(String.valueOf(traceDataCount), Colors.YELLOW);
        out("清除的 Systrace: ", Colors.CYAN);
        outln(String.valueOf(systraceRunnerCount), Colors.YELLOW);
        out("清除的 Systrace 数据: ", Colors.CYAN);
        outln(String.valueOf(systraceDataCount), Colors.YELLOW);
        out("清除的 Hook: ", Colors.CYAN);
        outln(String.valueOf(hookCount), Colors.YELLOW);
    }

    public static Map<Integer, SimpleSampleData> getSampleDataMap() {
        return new ConcurrentHashMap<>(sampleDataMap);
    }

    private void handleMultiThread(String[] args) throws JSONException {
        if (args.length < 2) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance multithread <subcmd> [args...]", Colors.GRAY);
            return;
        }

        String subCmd = args[1];

        switch (subCmd) {
            case "start" -> handleMultiThreadStart(args);
            case "stop" -> handleMultiThreadStop(args);
            case "report" -> handleMultiThreadReport(args);
            case "export" -> handleMultiThreadExport(args);
            default -> outln("未知子命令: " + subCmd, Colors.RED);
        }
    }

    private void handleMultiThreadStart(String[] args) {
        int sampleRate = 100;
        if (args.length >= 3) {
            try {
                sampleRate = Integer.parseInt(args[2]);
                if (sampleRate <= 0) {
                    outln("错误: 采样频率必须大于 0", Colors.RED);
                    return;
                }
                if (sampleRate > 10000) {
                    outln("警告: 采样频率过高（" + sampleRate + " Hz），可能影响性能", Colors.YELLOW);
                }
            } catch (NumberFormatException e) {
                outln("错误: 无效的采样频率: " + args[2], Colors.RED);
                return;
            }
        }

        int id = nextId.getAndIncrement();
        MultiThreadSampler sampler = new MultiThreadSampler(sampleRate);
        sampler.start();
        multiThreadSamplers.put(id, sampler);

        logger.info("多线程采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        staticLogger.info("多线程采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        
        outln("多线程采样器已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("采样频率: ", Colors.CYAN);
        outln(sampleRate + " Hz", Colors.YELLOW);
        outln("使用 'performance multithread stop " + id + "' 停止采样", Colors.GRAY);
        outln("使用 'performance multithread report " + id + "' 查看报告", Colors.GRAY);
    }

    private void handleMultiThreadStop(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance multithread stop <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        MultiThreadSampler sampler = multiThreadSamplers.get(id);
        if (sampler == null) {
            outln("错误: 多线程采样器不存在 (ID: " + id + ")", Colors.RED);
            return;
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
        
        outln("多线程采样器已停止", Colors.YELLOW);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("采样次数: ", Colors.CYAN);
        outln(String.valueOf(totalSamples), Colors.YELLOW);
        out("线程数量: ", Colors.CYAN);
        outln(String.valueOf(sampler.getThreadCount()), Colors.YELLOW);
        out("持续时间: ", Colors.CYAN);
        outln(data.getDurationString(), Colors.YELLOW);
        outln("使用 'performance multithread report " + id + "' 查看报告", Colors.GRAY);
    }

    private void handleMultiThreadReport(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance multithread report <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        MultiThreadSampleData data = multiThreadSampleDataMap.get(id);
        if (data == null) {
            outln("错误: 多线程采样数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        outln("=== 多线程采样报告 ===", Colors.CYAN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(data.id()), Colors.YELLOW);
        out("采样频率: ", Colors.CYAN);
        outln(data.sampleRate() + " Hz", Colors.YELLOW);
        out("采样次数: ", Colors.CYAN);
        outln(String.valueOf(data.totalSamples()), Colors.YELLOW);
        out("线程数量: ", Colors.CYAN);
        outln(String.valueOf(data.threadCount()), Colors.YELLOW);
        out("持续时间: ", Colors.CYAN);
        outln(data.getDurationString(), Colors.YELLOW);
        outln("", Colors.WHITE);

        for (Map.Entry<String, Map<String, Integer>> threadEntry : data.threadMethodCounts().entrySet()) {
            String threadName = threadEntry.getKey();
            Map<String, Integer> methodCounts = threadEntry.getValue();
            int threadSamples = Objects.requireNonNull(data.threadSampleCounts().getOrDefault(threadName, 0));

            out("线程: ", Colors.CYAN);
            outln(threadName, Colors.GREEN);
            out("  采样次数: ", Colors.CYAN);
            outln(String.valueOf(threadSamples), Colors.YELLOW);
            outln("  热点方法:", Colors.WHITE);

            methodCounts.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(10)
                .forEach(entry -> {
                    int count = entry.getValue();
                    double percentage = (count * 100.0) / threadSamples;
                    outln(String.format(
                            Locale.getDefault(),
                            "    %-60s %6d 次 (%5.1f%%)",
                        entry.getKey(), count, percentage), Colors.GRAY);
                });

            outln("", Colors.WHITE);
        }
    }

    private void handleMultiThreadExport(String[] args) throws JSONException {
        if (args.length < 4) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance multithread export <id> <file>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        String filePath = args[3];
        MultiThreadSampleData data = multiThreadSampleDataMap.get(id);
        if (data == null) {
            outln("错误: 多线程采样数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        JSONObject json = new JSONObject();
        json.put("id", data.id());
        json.put("sampleRate", data.sampleRate());
        json.put("startTime", data.startTime());
        json.put("stopTime", data.stopTime());
        json.put("totalSamples", data.totalSamples());
        json.put("threadCount", data.threadCount());
        
        JSONObject threads = new JSONObject();
        for (Map.Entry<String, Map<String, Integer>> threadEntry : data.threadMethodCounts().entrySet()) {
            JSONObject threadObj = new JSONObject();
            threadObj.put("sampleCount", data.threadSampleCounts().get(threadEntry.getKey()));
            
            JSONObject methods = new JSONObject();
            for (Map.Entry<String, Integer> methodEntry : threadEntry.getValue().entrySet()) {
                methods.put(methodEntry.getKey(), methodEntry.getValue());
            }
            threadObj.put("methods", methods);
            threads.put(threadEntry.getKey(), threadObj);
        }
        json.put("threads", threads);

        if (!writeToFile(filePath, json.toString(2))) {
            outln("导出多线程采样数据失败: 无法写入文件 " + filePath, Colors.RED);
            return;
        }

        logger.info("多线程采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("多线程采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        outln("多线程采样数据已导出", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("文件: ", Colors.CYAN);
        outln(filePath, Colors.GREEN);
        out("采样次数: ", Colors.CYAN);
        outln(String.valueOf(data.totalSamples()), Colors.YELLOW);
    }

    private void handleHierarchical(String[] args) throws JSONException {
        if (args.length < 2) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance hierarchical <subcmd> [args...]", Colors.GRAY);
            return;
        }

        String subCmd = args[1];

        switch (subCmd) {
            case "start" -> handleHierarchicalStart(args);
            case "stop" -> handleHierarchicalStop(args);
            case "report" -> handleHierarchicalReport(args);
            case "export" -> handleHierarchicalExport(args);
            default -> outln("未知子命令: " + subCmd, Colors.RED);
        }
    }

    private void handleHierarchicalStart(String[] args) {
        int sampleRate = 100;
        if (args.length >= 3) {
            try {
                sampleRate = Integer.parseInt(args[2]);
                if (sampleRate <= 0) {
                    outln("错误: 采样频率必须大于 0", Colors.RED);
                    return;
                }
                if (sampleRate > 10000) {
                    outln("警告: 采样频率过高（" + sampleRate + " Hz），可能影响性能", Colors.YELLOW);
                }
            } catch (NumberFormatException e) {
                outln("错误: 无效的采样频率: " + args[2], Colors.RED);
                return;
            }
        }

        int id = nextId.getAndIncrement();
        HierarchicalSampler sampler = new HierarchicalSampler(sampleRate);
        sampler.start();
        hierarchicalSamplers.put(id, sampler);

        logger.info("分层采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        staticLogger.info("分层采样器已启动 (ID: " + id + ", 频率: " + sampleRate + " Hz)");
        
        outln("分层采样器已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("采样频率: ", Colors.CYAN);
        outln(sampleRate + " Hz", Colors.YELLOW);
        outln("使用 'performance hierarchical stop " + id + "' 停止采样", Colors.GRAY);
        outln("使用 'performance hierarchical report " + id + "' 查看报告", Colors.GRAY);
    }

    private void handleHierarchicalStop(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance hierarchical stop <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        HierarchicalSampler sampler = hierarchicalSamplers.get(id);
        if (sampler == null) {
            outln("错误: 分层采样器不存在 (ID: " + id + ")", Colors.RED);
            return;
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
        
        outln("分层采样器已停止", Colors.YELLOW);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("采样次数: ", Colors.CYAN);
        outln(String.valueOf(totalSamples), Colors.YELLOW);
        out("方法数量: ", Colors.CYAN);
        outln(String.valueOf(sampler.getMethodCount()), Colors.YELLOW);
        out("持续时间: ", Colors.CYAN);
        outln(data.getDurationString(), Colors.YELLOW);
        outln("使用 'performance hierarchical report " + id + "' 查看报告", Colors.GRAY);
    }

    private void handleHierarchicalReport(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance hierarchical report <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        HierarchicalSampleData data = hierarchicalSampleDataMap.get(id);
        if (data == null) {
            outln("错误: 分层采样数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        outln("=== 分层采样报告 ===", Colors.CYAN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(data.id()), Colors.YELLOW);
        out("采样频率: ", Colors.CYAN);
        outln(data.sampleRate() + " Hz", Colors.YELLOW);
        out("采样次数: ", Colors.CYAN);
        outln(String.valueOf(data.totalSamples()), Colors.YELLOW);
        out("方法数量: ", Colors.CYAN);
        outln(String.valueOf(data.methodCount()), Colors.YELLOW);
        out("持续时间: ", Colors.CYAN);
        outln(data.getDurationString(), Colors.YELLOW);
        outln("", Colors.WHITE);
        outln("热点方法:", Colors.CYAN);

        data.methodCallInfos().entrySet().stream()
            .sorted((a, b) -> b.getValue().getSampleCount() - a.getValue().getSampleCount())
            .limit(20)
            .forEach(entry -> {
                HierarchicalSampler.MethodCallInfo info = entry.getValue();
                int count = info.getSampleCount();
                double percentage = (count * 100.0) / data.totalSamples();
                double avgDepth = info.getAverageDepth();
                
                outln(String.format(
                        Locale.getDefault(),
                        "  %-60s %6d 次 (%5.1f%%), 平均深度: %.1f",
                    info.methodKey, count, percentage, avgDepth), Colors.GRAY);
                
                Map<String, Integer> callers = info.getCallers();
                if (!callers.isEmpty()) {
                    outln("    调用者:", Colors.WHITE);
                    callers.entrySet().stream()
                        .sorted((a, b) -> b.getValue() - a.getValue())
                        .limit(5)
                        .forEach(callerEntry -> {
                            int callerCount = callerEntry.getValue();
                            double callerPercentage = (callerCount * 100.0) / count;
                            outln(String.format(
                                    Locale.getDefault(),
                                    "      %-60s %6d 次 (%5.1f%%)",
                                callerEntry.getKey(), callerCount, callerPercentage), Colors.GRAY);
                        });
                }
                outln("", Colors.WHITE);
            });
    }

    private void handleHierarchicalExport(String[] args) throws JSONException {
        if (args.length < 4) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance hierarchical export <id> <file>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        String filePath = args[3];
        HierarchicalSampleData data = hierarchicalSampleDataMap.get(id);
        if (data == null) {
            outln("错误: 分层采样数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        JSONObject json = new JSONObject();
        json.put("id", data.id());
        json.put("sampleRate", data.sampleRate());
        json.put("startTime", data.startTime());
        json.put("stopTime", data.stopTime());
        json.put("totalSamples", data.totalSamples());
        json.put("methodCount", data.methodCount());
        
        JSONObject methods = new JSONObject();
        for (Map.Entry<String, HierarchicalSampler.MethodCallInfo> entry : data.methodCallInfos().entrySet()) {
            HierarchicalSampler.MethodCallInfo info = entry.getValue();
            
            JSONObject methodObj = new JSONObject();
            methodObj.put("sampleCount", info.getSampleCount());
            methodObj.put("averageDepth", info.getAverageDepth());
            
            JSONObject callers = new JSONObject();
            for (Map.Entry<String, Integer> callerEntry : info.getCallers().entrySet()) {
                callers.put(callerEntry.getKey(), callerEntry.getValue());
            }
            methodObj.put("callers", callers);
            methods.put(entry.getKey(), methodObj);
        }
        json.put("methods", methods);

        if (!writeToFile(filePath, json.toString(2))) {
            outln("导出分层采样数据失败: 无法写入文件 " + filePath, Colors.RED);
            return;
        }

        logger.info("分层采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("分层采样数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        outln("分层采样数据已导出", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("文件: ", Colors.CYAN);
        outln(filePath, Colors.GREEN);
        out("采样次数: ", Colors.CYAN);
        outln(String.valueOf(data.totalSamples()), Colors.YELLOW);
    }

    private void handleTrace(String[] args) throws JSONException {
        if (args.length < 2) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance trace <subcmd> [args...]", Colors.GRAY);
            return;
        }

        String subCmd = args[1];

        switch (subCmd) {
            case "start" -> handleTraceStart();
            case "stop" -> handleTraceStop(args);
            case "report" -> handleTraceReport(args);
            case "export" -> handleTraceExport(args);
            default -> outln("未知子命令: " + subCmd, Colors.RED);
        }
    }

    private void handleTraceStart() {
        int id = nextId.getAndIncrement();
        Tracer tracer = new Tracer();
        tracer.start();
        tracers.put(id, tracer);

        logger.info("Tracer 已启动 (ID: " + id + ")");
        staticLogger.info("Tracer 已启动 (ID: " + id + ")");
        
        outln("Tracer 已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        outln("使用 'performance trace stop " + id + "' 停止 Trace", Colors.GRAY);
        outln("使用 'performance trace report " + id + "' 查看报告", Colors.GRAY);
    }

    private void handleTraceStop(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance trace stop <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        Tracer tracer = tracers.get(id);
        if (tracer == null) {
            outln("错误: Tracer 不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        tracer.stop();
        List<TraceData> traceData = tracer.getTraceData();
        traceDataMap.put(id, traceData);

        logger.info("Tracer 已停止 (ID: " + id + ")");
        staticLogger.info("Tracer 已停止 (ID: " + id + ", Trace 数据: " + traceData.size() + ")");
        
        outln("Tracer 已停止", Colors.YELLOW);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("Trace 数据: ", Colors.CYAN);
        outln(String.valueOf(traceData.size()), Colors.YELLOW);
        outln("使用 'performance trace report " + id + "' 查看报告", Colors.GRAY);
    }

    private void handleTraceReport(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance trace report <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        List<TraceData> traceData = traceDataMap.get(id);
        if (traceData == null || traceData.isEmpty()) {
            outln("错误: Trace 数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        outln("=== Trace 报告 ===", Colors.CYAN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("Trace 数据: ", Colors.CYAN);
        outln(String.valueOf(traceData.size()), Colors.YELLOW);
        outln("", Colors.WHITE);
        outln("Trace 段:", Colors.CYAN);

        for (TraceData data : traceData) {
            outln(String.format(
                    Locale.getDefault(),
                    "  %-40s %s (线程: %s, ID: %d)",
                    data.name(), data.getDurationString(), data.threadName(), data.threadId()), Colors.GRAY);
        }

        outln("", Colors.WHITE);
        outln("说明:", Colors.CYAN);
        outln("  - Trace 数据来自应用的 Trace.beginSection() 和 Trace.endSection() 调用", Colors.GRAY);
        outln("  - 需要应用代码支持才能收集 Trace 数据", Colors.GRAY);
        outln("  - 可以使用 Systrace 工具查看详细的 Trace 信息", Colors.GRAY);
    }

    private void handleTraceExport(String[] args) throws JSONException {
        if (args.length < 4) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance trace export <id> <file>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        String filePath = args[3];
        List<TraceData> traceData = traceDataMap.get(id);
        if (traceData == null || traceData.isEmpty()) {
            outln("错误: Trace 数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        JSONObject json = getTraceJsonObject(id, traceData);

        if (!writeToFile(filePath, json.toString(2))) {
            outln("导出 Trace 数据失败: 无法写入文件 " + filePath, Colors.RED);
            return;
        }

        logger.info("Trace 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("Trace 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        outln("Trace 数据已导出", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("文件: ", Colors.CYAN);
        outln(filePath, Colors.GREEN);
        out("Trace 数据: ", Colors.CYAN);
        outln(String.valueOf(traceData.size()), Colors.YELLOW);
    }

    @NonNull
    private static JSONObject getTraceJsonObject(int id, List<TraceData> traceData) throws JSONException {
        JSONObject json = new JSONObject();
        json.put("id", id);
        json.put("traceCount", traceData.size());

        JSONArray traces = new JSONArray();
        for (TraceData data : traceData) {
            JSONObject traceObj = new JSONObject();
            traceObj.put("name", data.name());
            traceObj.put("startTime", data.startTime());
            traceObj.put("duration", data.duration());
            traceObj.put("threadId", data.threadId());
            traceObj.put("threadName", data.threadName());
            traces.put(traceObj);
        }
        json.put("traces", traces);
        return json;
    }

    private void handleSystrace(String[] args) {
        if (args.length < 2) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance systrace <subcmd> [args...]", Colors.GRAY);
            return;
        }

        String subCmd = args[1];

        switch (subCmd) {
            case "start" -> handleSystraceStart(args);
            case "stop" -> handleSystraceStop(args);
            case "report" -> handleSystraceReport(args);
            case "export" -> handleSystraceExport(args);
            default -> outln("未知子命令: " + subCmd, Colors.RED);
        }
    }

    private void handleSystraceStart(String[] args) {
        int id = nextId.getAndIncrement();
        int duration = 10;
        String[] categories = null;

        if (args.length >= 3) {
            try {
                duration = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                outln("错误: 无效的持续时间: " + args[2], Colors.RED);
                return;
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
        
        outln("Systrace 已启动", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("持续时间: ", Colors.CYAN);
        outln(duration + " 秒", Colors.YELLOW);
        out("输出目录: ", Colors.CYAN);
        outln(outputDir, Colors.GREEN);
        outln("使用 'performance systrace stop " + id + "' 停止 Systrace", Colors.GRAY);
        outln("使用 'performance systrace report " + id + "' 查看报告", Colors.GRAY);
    }

    private void handleSystraceStop(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance systrace stop <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        SystraceRunner runner = systraceRunners.get(id);
        if (runner == null) {
            outln("错误: Systrace 不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        runner.stop();
        String outputFile = runner.getOutputFile();

        if (outputFile != null && !outputFile.isEmpty()) {
            try {
                SystraceData data = SystraceParser.parse(outputFile);
                systraceDataMap.put(id, data);

                logger.info("Systrace 已停止 (ID: " + id + ", 文件: " + outputFile + ")");
                staticLogger.info("Systrace 已停止 (ID: " + id + ", 文件: " + outputFile + ")");
                
                outln("Systrace 已停止", Colors.YELLOW);
                out("ID: ", Colors.CYAN);
                outln(String.valueOf(id), Colors.YELLOW);
                out("输出文件: ", Colors.CYAN);
                outln(outputFile, Colors.GREEN);
                outln("使用 'performance systrace report " + id + "' 查看报告", Colors.GRAY);
            } catch (Exception e) {
                logger.error("解析 Systrace 文件失败", e);
                outln("Systrace 已停止，但解析输出文件失败", Colors.YELLOW);
                out("ID: ", Colors.CYAN);
                outln(String.valueOf(id), Colors.YELLOW);
                out("输出文件: ", Colors.CYAN);
                outln(outputFile, Colors.GREEN);
                out("错误: ", Colors.CYAN);
                outln(e.getMessage(), Colors.RED);
            }
        } else {
            logger.info("Systrace 已停止 (ID: " + id + ")");
            staticLogger.info("Systrace 已停止 (ID: " + id + ")");
            
            outln("Systrace 已停止", Colors.YELLOW);
            out("ID: ", Colors.CYAN);
            outln(String.valueOf(id), Colors.YELLOW);
        }
    }

    private void handleSystraceReport(String[] args) {
        if (args.length < 3) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance systrace report <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        SystraceData data = systraceDataMap.get(id);
        if (data == null) {
            outln("错误: Systrace 数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        out(SystraceParser.generateReport(data), Colors.WHITE);
    }

    private void handleSystraceExport(String[] args) {
        if (args.length < 4) {
            outln("错误: 参数不足", Colors.RED);
            outln("用法: performance systrace export <id> <file>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            outln("错误: 无效的 ID: " + args[2], Colors.RED);
            return;
        }

        String filePath = args[3];
        SystraceData data = systraceDataMap.get(id);
        if (data == null) {
            outln("错误: Systrace 数据不存在 (ID: " + id + ")", Colors.RED);
            return;
        }

        String report = SystraceParser.generateReport(data);

        if (!writeToFile(filePath, report)) {
            outln("导出 Systrace 数据失败: 无法写入文件 " + filePath, Colors.RED);
            return;
        }

        logger.info("Systrace 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");
        staticLogger.info("Systrace 数据已导出 (ID: " + id + ", 文件: " + filePath + ")");

        outln("Systrace 数据已导出", Colors.GREEN);
        out("ID: ", Colors.CYAN);
        outln(String.valueOf(id), Colors.YELLOW);
        out("文件: ", Colors.CYAN);
        outln(filePath, Colors.GREEN);
        out("原始文件: ", Colors.CYAN);
        outln(data.file(), Colors.GREEN);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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
