package com.justnothing.testmodule.command.functions.hook;

import static com.justnothing.testmodule.constants.CommandServer.CMD_HOOK_VER;

import com.justnothing.javainterpreter.evaluator.DynamicClassGenerator;
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
import com.justnothing.testmodule.command.handlers.hook.HookListRequestHandler;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.reflect.DexClassDefiner;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.functions.hook.request.HookAddRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookRemoveRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookListRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookInfoRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookOutputRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookEnableRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookDisableRequest;
import com.justnothing.testmodule.command.functions.hook.request.HookClearRequest;

@Cmd(
    name = "hook",
    description = "动态Hook注入器, 通过脚本实现Hook功能",
    defaultResultType = HookListResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(
        path = "add",
        request = HookAddRequest.class,
        handler = HookMain.class,
        description = "添加Hook"
    ),
    @CmdRoutes.Route(
        path = "remove",
        request = HookRemoveRequest.class,
        handler = HookMain.class,
        description = "移除指定Hook"
    ),
    @CmdRoutes.Route(
        path = "list",
        request = HookListRequest.class,
        handler = HookMain.class,
        description = "列出所有Hook"
    ),
    @CmdRoutes.Route(
        path = "info",
        request = HookInfoRequest.class,
        handler = HookMain.class,
        description = "显示Hook详细信息"
    ),
    @CmdRoutes.Route(
        path = "output",
        request = HookOutputRequest.class,
        handler = HookMain.class,
        description = "获取Hook输出"
    ),
    @CmdRoutes.Route(
        path = "enable",
        request = HookEnableRequest.class,
        handler = HookMain.class,
        description = "启用Hook"
    ),
    @CmdRoutes.Route(
        path = "disable",
        request = HookDisableRequest.class,
        handler = HookMain.class,
        description = "禁用Hook"
    ),
    @CmdRoutes.Route(
        path = "clear",
        request = HookClearRequest.class,
        handler = HookMain.class,
        description = "清除所有Hook"
    )
})
public class HookMain extends MainCommand<HookListResult> {

    private static final Logger logger = Logger.getLoggerForName("HookMain");

    static {
        DynamicClassGenerator.setDefaultClassDefiner(DexClassDefiner.getInstance());
    }

    public HookMain() {
        super("hook", HookListResult.class);
    }

    private String stripQuotes(String str) {
        if (str == null) return null;
        str = str.trim();
        if (str.startsWith("\"") && str.endsWith("\"")) return str.substring(1, str.length() - 1);
        if (str.startsWith("'") && str.endsWith("'")) return str.substring(1, str.length() - 1);
        return str;
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("hook");
    }

    @Override
    public HookListResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            return createErrorResult("参数不足");
        }

        String subCommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        try {
            switch (subCommand) {
                case "add" -> handleAdd(subArgs, context);
                case "remove" -> handleRemove(subArgs, context);
                case "list" -> handleList(context);
                case "info" -> handleInfo(subArgs, context);
                case "output" -> handleOutput(subArgs, context);
                case "enable" -> handleEnable(subArgs, context);
                case "disable" -> handleDisable(subArgs, context);
                case "clear" -> handleClear(context);
                default -> {
                    context.print("未知子命令: ", Colors.RED);
                    context.println(subCommand, Colors.YELLOW);
                }
            }
        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            CommandExceptionHandler.handleException("hook " + subCommand, e, context, "执行hook命令失败");
            return createErrorResult("执行hook命令失败: " + e.getMessage());
        }

        HookListRequestHandler handler = new HookListRequestHandler();
        HookListRequest request = new HookListRequest();
        request.setRequestId(java.util.UUID.randomUUID().toString());
        return handler.handle(request);
    }

    private void handleAdd(String[] args, CommandExecutor.CmdExecContext context) throws Exception {
        if (args.length < 2) {
            throw new IllegalCommandLineArgumentException("需要指定类名和方法名");
        }

        String className = stripQuotes(args[0]);
        String methodName = stripQuotes(args[1]);
        String signature = null;
        String beforeCode = null, afterCode = null, replaceCode = null;
        String beforeCodebase = null, afterCodebase = null, replaceCodebase = null;

        int i = 2;
        while (i < args.length) {
            String arg = args[i];

            if ((arg.equals("sig") || arg.equals("signature")) && i + 1 < args.length) {
                signature = stripQuotes(args[++i]);
                i++;
            } else if (arg.equals("before") && i + 2 < args.length) {
                if (beforeCode != null || beforeCodebase != null) {
                    throw new IllegalCommandLineArgumentException("before阶段已经指定过，不能重复指定");
                }
                String type = args[++i];
                String value = args[++i];
                if (type.equals("code")) beforeCode = value;
                else if (type.equals("codebase")) beforeCodebase = value;
                else throw new IllegalCommandLineArgumentException("before参数必须为 'code' 或 'codebase'");
                i++;
            } else if (arg.equals("after") && i + 2 < args.length) {
                if (afterCode != null || afterCodebase != null) {
                    throw new IllegalCommandLineArgumentException("after阶段已经指定过，不能重复指定");
                }
                String type = args[++i];
                String value = args[++i];
                if (type.equals("code")) afterCode = value;
                else if (type.equals("codebase")) afterCodebase = value;
                else throw new IllegalCommandLineArgumentException("after参数必须为 'code' 或 'codebase'");
                i++;
            } else if (arg.equals("replace") && i + 2 < args.length) {
                if (replaceCode != null || replaceCodebase != null) {
                    throw new IllegalCommandLineArgumentException("replace阶段已经指定过，不能重复指定");
                }
                String type = args[++i];
                String value = args[++i];
                if (type.equals("code")) replaceCode = value;
                else if (type.equals("codebase")) replaceCodebase = value;
                else throw new IllegalCommandLineArgumentException("replace参数必须为 'code' 或 'codebase'");
                i++;
            } else {
                throw new IllegalCommandLineArgumentException("无效参数: " + arg);
            }
        }

        if (beforeCode == null && afterCode == null && replaceCode == null &&
            beforeCodebase == null && afterCodebase == null && replaceCodebase == null) {
            throw new IllegalCommandLineArgumentException("需要指定至少一个Hook阶段（before/after/replace）");
        }

        logger.debug("添加Hook: " + className + "." + methodName +
            (signature != null ? "(" + signature + ")" : ""));

        HookManager.addHook(className, methodName, signature,
                beforeCode, afterCode, replaceCode,
                beforeCodebase, afterCodebase, replaceCodebase,
                context);
    }

    private void handleRemove(String[] args, CommandExecutor.CmdExecContext context) throws Exception {
        HookRemoveRequest request = new HookRemoveRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        logger.debug("移除Hook: " + request.getHookId());
        HookManager.removeHook(request.getHookId(), context);
    }

    private void handleList(CommandExecutor.CmdExecContext context) {
        HookManager.listHooks(context);
    }

    private void handleInfo(String[] args, CommandExecutor.CmdExecContext context) throws Exception {
        HookInfoRequest request = new HookInfoRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        logger.debug("查看Hook信息: " + request.getHookId());
        HookManager.getHookInfo(request.getHookId(), context);
    }

    private void handleOutput(String[] args, CommandExecutor.CmdExecContext context) throws Exception {
        HookOutputRequest request = new HookOutputRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        logger.debug("获取Hook输出: " + request.getHookId() + " count=" + request.getOutputCount());
        HookManager.getHookOutput(request.getHookId(), context, request.getOutputCount());
    }

    private void handleEnable(String[] args, CommandExecutor.CmdExecContext context) throws Exception {
        HookEnableRequest request = new HookEnableRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        logger.debug("启用Hook: " + request.getHookId());
        HookManager.enableHook(request.getHookId(), context);
    }

    private void handleDisable(String[] args, CommandExecutor.CmdExecContext context) throws Exception {
        HookDisableRequest request = new HookDisableRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        logger.debug("禁用Hook: " + request.getHookId());
        HookManager.disableHook(request.getHookId(), context);
    }

    private void handleClear(CommandExecutor.CmdExecContext context) {
        int count = HookManager.getHookCount();
        HookManager.clearAllHooks();
        context.print("已清除", Colors.LIGHT_GREEN);
        context.print(" " + count + " ", Colors.YELLOW);
        context.println("个Hook", Colors.LIGHT_GREEN);
    }
}
