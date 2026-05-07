package com.justnothing.testmodule.command.functions.performance.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.performance.PerfTaskManager;
import com.justnothing.testmodule.command.functions.performance.PerformanceCommand;
import com.justnothing.testmodule.command.functions.performance.PerformanceRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public abstract class AbstractPerfCommand<Req extends PerformanceRequest, Res extends CommandResult>
        extends AbstractCommand<Req, Res>
        implements PerformanceCommand<Req, Res> {

    protected static final Logger logger = Logger.getLoggerForName("Performance");
    protected CommandExecutor.CmdExecContext<Req> context;

    protected AbstractPerfCommand(String name, Class<Req> reqType, Class<Res> resType) {
        super(name, reqType, resType);
    }

    protected void out(Object obj, byte color) {
        if (context != null) {
            context.print(obj, color);
        }
    }

    protected void outln(Object obj, byte color) {
        if (context != null) {
            context.println(obj, color);
        }
    }

    protected PerfTaskManager getTaskManager() {
        return PerfTaskManager.getInstance();
    }

    protected int parseId(String[] args, int index) throws NumberFormatException {
        if (args.length <= index) {
            throw new IllegalArgumentException("参数不足，需要 ID");
        }
        return Integer.parseInt(args[index]);
    }

    protected int parseSampleRate(String[] args, int index) {
        if (args.length > index) {
            try {
                int rate = Integer.parseInt(args[index]);
                if (rate <= 0) { outln("错误: 采样频率必须大于 0", Colors.RED); return -1; }
                if (rate > 10000) outln("警告: 采样频率过高", Colors.YELLOW);
                return rate;
            } catch (NumberFormatException e) {
                outln("错误: 无效的采样频率: " + args[index], Colors.RED);
                return -1;
            }
        }
        return 100;
    }

    protected String formatDurationNs(long durationNs) {
        if (durationNs < 1000) return durationNs + "ns";
        else if (durationNs < 1_000_000) return String.format(Locale.getDefault(), "%.2fus", durationNs / 1000.0);
        else if (durationNs < 1_000_000_000) return String.format(Locale.getDefault(), "%.2fms", durationNs / 1_000_000.0);
        else return String.format(Locale.getDefault(), "%.2fs", durationNs / 1_000_000_000.0);
    }

    protected boolean writeToFile(String filePath, String content) {
        try {
            File file = new File(filePath);
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) IOManager.createDirectory(parentDir.getAbsolutePath());
            IOManager.writeFile(filePath, content);
            return true;
        } catch (IOException e) {
            logger.error("写入文件失败: " + filePath, e);
            return false;
        }
    }

    protected boolean isStructuredMode() {
        return context != null;
    }

    @Override
    public Res executeInternal(CommandExecutor.CmdExecContext<Req> ctx) throws Exception {
        this.context = ctx;
        try {
            return executePerfCommand(ctx.getRequest());
        } finally {
            this.context = null;
        }
    }

    protected abstract Res executePerfCommand(Req request) throws Exception;
}
