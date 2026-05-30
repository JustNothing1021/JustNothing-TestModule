package com.justnothing.testmodule.command.functions.trace;

import static com.justnothing.testmodule.constants.CommandServer.CMD_TRACE_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.command.CmdParamProcessor;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.command.functions.intercept.TraceInterceptTask;
import com.justnothing.testmodule.utils.logging.Logger;

import java.util.List;

import com.justnothing.testmodule.command.functions.trace.request.TraceAddRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceListRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceShowRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceExportRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceStopRequest;
import com.justnothing.testmodule.command.functions.trace.request.TraceClearRequest;

@Cmd(
    name = "trace",
    description = "跟踪方法调用链，生成调用树",
    defaultResultType = TraceResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(path = "add", request = TraceAddRequest.class, handler = TraceMain.class, description = "添加跟踪任务"),
    @CmdRoutes.Route(path = "list", request = TraceListRequest.class, handler = TraceMain.class, description = "列出所有跟踪任务"),
    @CmdRoutes.Route(path = "show", request = TraceShowRequest.class, handler = TraceMain.class, description = "显示调用树"),
    @CmdRoutes.Route(path = "export", request = TraceExportRequest.class, handler = TraceMain.class, description = "导出调用链到文件"),
    @CmdRoutes.Route(path = "stop", request = TraceStopRequest.class, handler = TraceMain.class, description = "停止指定跟踪"),
    @CmdRoutes.Route(path = "clear", request = TraceClearRequest.class, handler = TraceMain.class, description = "清除所有跟踪")
})
public class TraceMain extends MainCommand<TraceResult> {

    private static final Logger logger = Logger.getLoggerForName("TraceMain");

    public TraceMain() {
        super("trace", TraceResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("trace");
    }

    @Override
    public TraceResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();

        logger.debug("执行 trace 命令，参数: %s", java.util.Arrays.toString(args));

        if (args.length < 1) {
            logger.warn("参数不足");
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        String subCommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        TraceManager manager = TraceManager.getInstance();

        try {
            switch (subCommand) {
                case "add" -> handleAdd(subArgs, classLoader, manager, context);
                case "list" -> handleList(manager, context);
                case "show" -> handleShow(subArgs, manager, context);
                case "export" -> handleExport(subArgs, manager, context);
                case "stop" -> handleStop(subArgs, manager, context);
                case "clear" -> handleClear(manager, context);
                default -> {
                    context.print("未知子命令: ", Colors.RED);
                    context.println(subCommand, Colors.YELLOW);
                    context.println(getHelpText(), Colors.WHITE);
                }
            }
        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            CommandExceptionHandler.handleException("trace " + subCommand, e, context, "执行trace命令失败");
            return createErrorResult("执行trace命令失败: " + e.getMessage());
        }

        return createSuccessResult("跟踪命令执行完成");
    }

    private void handleAdd(String[] args, ClassLoader classLoader, TraceManager manager, CommandExecutor.CmdExecContext context) throws Exception {
        TraceAddRequest request = new TraceAddRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        logger.info("添加 trace 任务: class=%s, method=%s, sig=%s",
                   request.getClassName(), request.getMethodName(),
                   request.getSignature() != null ? request.getSignature() : "所有");

        try {
            int id = manager.addTraceTask(request.getClassName(), request.getMethodName(), request.getSignature(), classLoader);
            context.println("添加trace任务成功", Colors.GREEN);
            context.print("ID: ", Colors.CYAN);
            context.println(String.valueOf(id), Colors.YELLOW);

            logger.debug("Trace 任务创建成功: id=%d", id);
        } catch (Exception e) {
            logger.error("添加 trace 任务失败: %s - %s", e.getClass().getSimpleName(), e.getMessage());
            throw e;
        }
    }

    private void handleList(TraceManager manager, CommandExecutor.CmdExecContext context) {
        List<TraceInterceptTask> tasks = manager.listTasks();

        if (tasks.isEmpty()) {
            context.println("没有活跃的trace任务", Colors.GRAY);
            return;
        }

        logger.debug("列出 %d 个活跃的 trace 任务", tasks.size());

        context.println("活跃的trace任务:", Colors.CYAN);
        context.println("ID\t类名\t方法名\t签名\t状态\t调用次数", Colors.GRAY);
        context.println("--------------------------------------------------", Colors.GRAY);

        for (TraceInterceptTask task : tasks) {
            context.print(String.valueOf(task.getId()), Colors.YELLOW);
            context.print("\t", Colors.WHITE);
            context.print(task.getClassName(), Colors.GREEN);
            context.print("\t", Colors.WHITE);
            context.print(task.getMethodName(), Colors.GREEN);
            context.print("\t", Colors.WHITE);
            context.print(task.getSignature() != null ? task.getSignature() : "所有", Colors.GRAY);
            context.print("\t", Colors.WHITE);
            context.print(task.isRunning() ? "运行中" : "已停止", task.isRunning() ? Colors.GREEN : Colors.RED);
            context.print("\t", Colors.WHITE);
            context.println(String.valueOf(task.getCallCount()), Colors.YELLOW);
        }
    }

    private void handleShow(String[] args, TraceManager manager, CommandExecutor.CmdExecContext context) throws Exception {
        TraceShowRequest request = new TraceShowRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        logger.info("显示 trace 调用树: id=%d", request.getTraceId());

        TraceInterceptTask task = manager.getTask(request.getTraceId());
        if (task == null) {
            throw new IllegalCommandLineArgumentException("未找到trace任务 (ID: " + request.getTraceId() + ")");
        }

        String result = task.getCallTree();
        context.println(result, Colors.WHITE);
    }

    private void handleExport(String[] args, TraceManager manager, CommandExecutor.CmdExecContext context) throws Exception {
        TraceExportRequest request = new TraceExportRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        logger.info("导出 trace 结果: id=%d, file=%s", request.getTraceId(), request.getFilePath());

        TraceInterceptTask task = manager.getTask(request.getTraceId());
        if (task == null) {
            throw new IllegalCommandLineArgumentException("未找到trace任务 (ID: " + request.getTraceId() + ")");
        }

        boolean success = task.exportToFile(request.getFilePath());
        if (success) {
            context.println("导出trace任务成功", Colors.GREEN);
            context.print("文件路径: ", Colors.CYAN);
            context.println(request.getFilePath(), Colors.GRAY);
            logger.debug("导出成功: file=%s", request.getFilePath());
        } else {
            throw new RuntimeException("导出trace任务失败");
        }
    }

    private void handleStop(String[] args, TraceManager manager, CommandExecutor.CmdExecContext context) throws Exception {
        TraceStopRequest request = new TraceStopRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        logger.info("停止 trace 任务: id=%d", request.getTraceId());

        boolean success = manager.removeTask(request.getTraceId());
        if (success) {
            context.println("停止trace任务成功", Colors.GREEN);
            logger.debug("Trace 任务已停止: id=%d", request.getTraceId());
        } else {
            throw new IllegalCommandLineArgumentException("未找到trace任务 (ID: " + request.getTraceId() + ")");
        }
    }

    private void handleClear(TraceManager manager, CommandExecutor.CmdExecContext context) {
        logger.warn("清除所有 trace 任务");
        manager.clearAll();
        context.println("清除所有trace任务成功", Colors.GREEN);
    }
}
