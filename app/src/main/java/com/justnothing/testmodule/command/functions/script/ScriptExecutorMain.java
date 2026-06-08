package com.justnothing.testmodule.command.functions.script;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SCRIPT_VER;

import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.functions.script.impl.ScriptCrudCommand;
import com.justnothing.testmodule.command.functions.script.impl.ScriptExecCommand;
import com.justnothing.testmodule.command.functions.script.impl.ScriptManageCommand;
import com.justnothing.testmodule.command.functions.script.impl.ScriptPermissionCommand;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.reflect.AppClassFinder;
import com.justnothing.testmodule.utils.sandbox.BlockGuardSandbox;

import com.justnothing.javainterpreter.ScriptRunner;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ParseException;
import com.justnothing.javainterpreter.security.SandboxConfig;

import com.justnothing.testmodule.command.functions.script.request.*;
import com.justnothing.testmodule.command.functions.script.impl.AbstractScriptCommand;

import java.util.Map;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.lang.reflect.Array;

@Cmd(
    name = "script",
    description = "JustNothing 脚本解释器 - 执行/管理 Java 脚本",
    defaultResultType = ScriptResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(path = "create", request = ScriptCreateRequest.class, handler = ScriptCrudCommand.class, description = "创建新脚本"),
    @CmdRoutes.Route(path = "list", request = ScriptListRequest.class, handler = ScriptManageCommand.class, description = "列出所有脚本"),
    @CmdRoutes.Route(path = "vars", request = ScriptVarsRequest.class, handler = ScriptManageCommand.class, description = "列出脚本执行器变量"),
    @CmdRoutes.Route(path = "show", request = ScriptShowRequest.class, handler = ScriptCrudCommand.class, description = "显示脚本内容"),
    @CmdRoutes.Route(path = "delete", request = ScriptDeleteRequest.class, handler = ScriptCrudCommand.class, description = "删除脚本"),
    @CmdRoutes.Route(path = "run", request = ScriptRunRequest.class, handler = ScriptExecCommand.class, description = "执行脚本"),
    @CmdRoutes.Route(path = "import", request = ScriptImportRequest.class, handler = ScriptExecCommand.class, description = "导入脚本文件"),
    @CmdRoutes.Route(path = "export", request = ScriptExportRequest.class, handler = ScriptExecCommand.class, description = "导出脚本文件"),
    @CmdRoutes.Route(path = "manage", request = ScriptManageRequest.class, handler = ScriptManageCommand.class, description = "交互式脚本管理器"),
    @CmdRoutes.Route(path = "interactive", request = ScriptInteractiveRequest.class, handler = ScriptExecCommand.class, description = "启动交互REPL执行器"),
    @CmdRoutes.Route(path = "permission/grant", request = ScriptPermGrantRequest.class, handler = ScriptPermissionCommand.class, description = "授予权限"),
    @CmdRoutes.Route(path = "permission/deny", request = ScriptPermDenyRequest.class, handler = ScriptPermissionCommand.class, description = "拒绝权限"),
    @CmdRoutes.Route(path = "permission/preset", request = ScriptPermPresetRequest.class, handler = ScriptPermissionCommand.class, description = "应用权限预设"),
    @CmdRoutes.Route(path = "permission/reset", request = ScriptPermResetRequest.class, handler = ScriptPermissionCommand.class, description = "重置权限配置"),
    @CmdRoutes.Route(path = "permission/list", request = ScriptPermListRequest.class, handler = ScriptPermissionCommand.class, description = "列出所有权限类型"),
    @CmdRoutes.Route(path = "permission/show-config", request = ScriptPermShowConfigRequest.class, handler = ScriptPermissionCommand.class, description = "显示当前权限配置"),
})
public class ScriptExecutorMain extends MainCommand<ScriptResult> {

    private final String commandName;

    public ScriptExecutorMain() {
        super("script", ScriptResult.class);
        this.commandName = "script";
    }

    public ScriptExecutorMain(String commandName) {
        super("ScriptExecutor", ScriptResult.class);
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        return switch (commandName) {
            case "sclear" -> String.format("""
                    语法: sclear

                    清空脚本执行器的所有变量.

                    示例:
                        sclear

                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            case "svars" -> String.format("""
                    语法: svars

                    显示脚本执行器的变量列表.

                    示例:
                        svars

                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            case "srun" -> String.format("""
                    语法: srun <code>

                    快捷执行脚本代码.
                    具体执行逻辑与script run相同.
                    (注: 运行script可以查看说明)

                    示例:
                        srun 'String a = "114514"; println(a);'
                        srun 'for (int i = 0; i < 10; i++) println(i);'

                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            case "sinteractive" -> String.format("""
                    语法: sinteractive

                    进入交互式脚本执行模式.

                    多行模式:
                        :multi     - 进入多行模式
                        :eval      - 执行多行代码
                        :clear     - 清空缓冲区
                        (自动检测括号未闭合时也会进入多行模式)

                    调试:
                        setPrintAST(true)  - 开启AST打印
                        setPrintAST(false) - 关闭AST打印

                    退出命令:
                        exit, quit  - 退出交互式模式

                    示例:
                        sinteractive

                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            default -> CommandRouter.getInstance().generateHelpForCommand("script");
        };
    }

    @Override
    public ScriptResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String cmdName = context.cmdName();

        switch (cmdName) {
            case "sclear" -> clearExecutorVariables(context);
            case "svars" -> listExecutorVariables(context);
            case "srun" -> executeScriptCode(context);
            case "sinteractive" -> runInteractiveMode(context);
            case "script" -> {
                String[] args = context.args();
                if (args.length == 0) {
                    context.println(getHelpText(), Colors.WHITE);
                    return makeErrorResult("参数不足，需要指定子命令");
                }
                context.println("提示: 使用 'script help' 查看路由帮助", Colors.CYAN);
                return makeErrorResult("旧路径已迁移到 @CmdRoutes，请使用新路径");
            }
        }

        ScriptResult scriptResult = new ScriptResult(java.util.UUID.randomUUID().toString());
        scriptResult.setSubCommand(cmdName);
        Map<String, Object> vars = AbstractScriptCommand.systemScriptRunner.getAllVariablesAsObject();
        if (vars != null && !vars.isEmpty()) {
            List<ScriptResult.VariableInfo> varList = new java.util.ArrayList<>();
            for (Map.Entry<String, Object> entry : vars.entrySet()) {
                varList.add(new ScriptResult.VariableInfo(entry.getKey(), entry.getValue()));
            }
            scriptResult.setVariables(varList);
        }
        return scriptResult;
    }

    public ScriptResult execute(CommandExecutor.CmdExecContext<? extends CommandRequest> ctx) {
        String[] rawArgs = ctx.args();
        if (rawArgs.length >= 2 && "-p".equals(rawArgs[0])) {
            String preset = rawArgs[1];
            SandboxConfig permConfig = resolvePreset(preset, ctx);
            if (permConfig != null) {
                AbstractScriptCommand.currentPermissionConfig.set(permConfig);
            }
            ctx.print("[权限模式: ", Colors.YELLOW);
            ctx.print(preset, Colors.ORANGE);
            ctx.println("]", Colors.YELLOW);
        }
        return null;
    }

    private SandboxConfig resolvePreset(String preset, CommandExecutor.CmdExecContext<?> ctx) {
        switch (preset.toLowerCase()) {
            case "sandbox" -> { return SandboxConfig.SANDBOX; }
            case "expression" -> { return SandboxConfig.EXPRESSION_ONLY; }
            case "minimal" -> { return SandboxConfig.MINIMAL; }
            case "full" -> { return null; }
            default -> { ctx.println("未知预设: " + preset + "，可用: sandbox, expression, minimal, full", Colors.RED); return null; }
        }
    }

    public void clearExecutorVariables(CommandExecutor.CmdExecContext<?> context) {
        AbstractScriptCommand.systemScriptRunner.clearVariables();
        context.println("已清空所有执行器的变量", Colors.GREEN);
    }

    public void listExecutorVariables(CommandExecutor.CmdExecContext<?> context) {

        context.println("", Colors.WHITE);
        context.println("脚本执行器的变量列表:", Colors.CYAN);

        Map<String, Object> scriptVars = AbstractScriptCommand.systemScriptRunner.getAllVariablesAsObject();
        if (scriptVars.isEmpty()) {
            context.println("  (空)", Colors.GRAY);
        } else {
            for (Map.Entry<String, Object> entry : scriptVars.entrySet()) {
                Object value = entry.getValue();
                context.print("  " + entry.getKey() + " = ", Colors.CYAN);
                context.print(String.valueOf(value), Colors.GREEN);
                context.println(" (" + (value != null ? value.getClass().getSimpleName() : "null") + ")", Colors.GRAY);
        }
        }
    }

    private void executeScriptCode(CommandExecutor.CmdExecContext<?> context) {
        String code = context.origCommand();
        context.println("[脚本执行] 在独立线程中执行...", Colors.CYAN);
        context.println("", Colors.WHITE);

        AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

        Future<?> future = ThreadPoolManager.submitIOCallable(() -> {
            try {
                SandboxConfig config = AbstractScriptCommand.currentPermissionConfig.get();

                if (config != null) {
                    BlockGuardSandbox.execute(config, () -> {
                        ScriptRunner runner = new ScriptRunner(context.classLoader());
                        runner.setClassFinder(new AppClassFinder());
                        if (config.getAstPermissionChecker() != null) {
                            runner.getExecutionContext().setPermissionChecker(config.getAstPermissionChecker());
                        }
                        runner.execute(code, context.output(), context.output());
                        return null;
                    });
                } else {
                    ScriptRunner runner = new ScriptRunner(context.classLoader());
                    runner.setClassFinder(new AppClassFinder());
                    runner.execute(code, context.output(), context.output());
                }
            } catch (Throwable e) {
                errorRef.set(e);
            }
            return null;
        });

        try {
            assert future != null : "无法获取用于执行脚本代码的Future";
            future.get(5, TimeUnit.MINUTES);

            if (errorRef.get() != null) {
                Throwable e = errorRef.get();
                Throwable cause = e;
                while (cause instanceof RuntimeException && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                if (cause instanceof SecurityException) {
                    context.print("权限拒绝: ", Colors.RED);
                    context.println(cause.getMessage() != null ? cause.getMessage() : "没有详细信息", Colors.ORANGE);
                } else {
                    CommandExceptionHandler.handleException("script", e, context, "脚本执行失败");
                }
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            context.println("脚本执行超时（5分钟），已取消", Colors.RED);
        } catch (Exception e) {
            context.print("等待脚本执行结果时发生异常: ", Colors.RED);
            context.println(e.getMessage() != null ? e.getMessage() : "没有错误信息", Colors.ORANGE);
        }
    }

    private void runInteractiveMode(CommandExecutor.CmdExecContext<?> context) {
        ScriptRunner runner = new ScriptRunner(context.classLoader());
        runner.setClassFinder(new AppClassFinder());

        context.println("====== 脚本交互执行模式 =====", Colors.CYAN);
        context.println("输入 'exit' 或 'quit' 退出", Colors.GRAY);
        context.println("输入 ':multi' 进入多行模式, ':eval' 执行, ':clear' 清空", Colors.GRAY);
        context.println("", Colors.WHITE);

        StringBuilder multiLineBuffer = new StringBuilder();
        boolean multiLineMode = false;
        boolean autoMultiLine = false;

        try {
            label: while (true) {
                String prompt = (multiLineMode || autoMultiLine) ? "... " : ">>> ";
                String code = context.readLine(prompt);

                if (code == null) {
                    context.println("", Colors.WHITE);
                    break;
                }

                code = code.trim();

                switch (code) {
                    case "exit":
                    case "quit":
                        context.println("Say goodbye~~~", Colors.GREEN);
                        break label;
                    case "":
                        if (autoMultiLine) continue;
                        continue;
                    case ":multi":
                        multiLineMode = true;
                        autoMultiLine = false;
                        context.println("进入多行模式, 输入 ':eval' 执行, ':clear' 清空, ':exit' 退出多行模式", Colors.CYAN);
                        continue;
                }

                if (code.equals(":exit") && (multiLineMode || autoMultiLine)) {
                    multiLineMode = false;
                    autoMultiLine = false;
                    multiLineBuffer.setLength(0);
                    context.println("退出多行模式", Colors.CYAN);
                    continue;
                }

                if (code.equals(":clear") && (multiLineMode || autoMultiLine)) {
                    multiLineBuffer.setLength(0);
                    context.println("已清空", Colors.GREEN);
                    continue;
                }

                if (code.equals(":eval") && (multiLineMode || autoMultiLine)) {
                    String fullCode = multiLineBuffer.toString();
                    multiLineBuffer.setLength(0);
                    multiLineMode = false;
                    autoMultiLine = false;
                    if (!fullCode.isEmpty()) {
                        executeCodeInRunner(runner, fullCode, context);
                    } else {
                        context.println("没有代码可执行", Colors.GRAY);
                    }
                    continue;
                }

                if (multiLineMode || autoMultiLine) {
                    multiLineBuffer.append(code).append("\n");
                    if (autoMultiLine && isCodeComplete(multiLineBuffer.toString())) {
                        String fullCode = multiLineBuffer.toString();
                        multiLineBuffer.setLength(0);
                        autoMultiLine = false;
                        executeCodeInRunner(runner, fullCode, context);
                    }
                    continue;
                }

                if (!isCodeComplete(code)) {
                    multiLineBuffer.append(code).append("\n");
                    autoMultiLine = true;
                    continue;
                }

                executeCodeInRunner(runner, code, context);
            }
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String errorMessage = (cause != null) ? cause.getMessage() : e.getMessage();
            if (errorMessage != null && errorMessage.toLowerCase().contains("unterminated string")) {
                context.println("语法错误: " + errorMessage, Colors.RED);
                multiLineBuffer.setLength(0);
            } else if (e.getMessage() != null && (e.getMessage().contains("超时") || e.getMessage().contains("中断") || e.getMessage().contains("通信失败"))) {
                context.println("输入处理出错: " + errorMessage, Colors.RED);
                multiLineBuffer.setLength(0);
            } else {
                handleExecutionException(e, context, runner.getExecutionContext());
            }
        }
        context.println("交互式模式已退出", Colors.GREEN);
    }

    private void executeCodeInRunner(ScriptRunner runner, String code, CommandExecutor.CmdExecContext<?> context) {
        SandboxConfig config = AbstractScriptCommand.currentPermissionConfig.get();
        try {
            if (config != null) {
                if (config.getAstPermissionChecker() != null) {
                    runner.getExecutionContext().setPermissionChecker(config.getAstPermissionChecker());
                }
                BlockGuardSandbox.execute(config, () -> {
                    Object result = runner.executeWithResult(code, context.output(), context.output());
                    if (result != null) {
                        context.println(formatValue(result, new HashSet<>()), Colors.GRAY);
                    }
                });
            } else {
                runner.getExecutionContext().setPermissionChecker(null);
                Object result = runner.executeWithResult(code, context.output(), context.output());
                if (result != null) {
                    context.println(formatValue(result, new HashSet<>()), Colors.GRAY);
                }
            }
        } catch (Throwable e) {
            handleExecutionException(e, context, runner.getExecutionContext());
        }
    }

    private void handleExecutionException(Throwable e, CommandExecutor.CmdExecContext<?> context,
            com.justnothing.javainterpreter.evaluator.ExecutionContext executionContext) {
        Throwable cause = e.getCause();
        String message = e.getMessage();
        boolean isParseError = message != null && message.startsWith("Parse error:");
        byte errorColor = isParseError ? Colors.ORANGE : Colors.RED;

        if (cause instanceof EvaluationException evalEx) {
            Throwable innerCause = evalEx.getCause();
            context.print("执行错误: ", errorColor);
            context.println(evalEx.getMessage(), errorColor);
            if (executionContext.isPrintAST() && evalEx.getNode() != null) {
                context.println("出错的AST: ", Colors.GRAY);
                context.println(evalEx.getNode().formatString(), Colors.GRAY);
            }
            if (innerCause != null) {
                context.output().printStackTrace(innerCause, Colors.GRAY);
            }
        } else if (cause instanceof ParseException parseEx) {
            context.print("语法错误: ", errorColor);
            context.println(parseEx.getMessage(), errorColor);
        } else if (cause != null) {
            context.println((isParseError ? "语法错误: " : "错误: ") + cause.getMessage(), errorColor);
            context.output().printStackTrace(cause, Colors.GRAY);
        } else {
            context.println((isParseError ? "语法错误: " : "错误: ") + message, errorColor);
            context.output().printStackTrace(e, Colors.GRAY);
        }
    }

    private static boolean isCodeComplete(String code) {
        int braceCount = 0, parenCount = 0, bracketCount = 0, preprocessorConditionCount = 0;
        boolean inString = false, inChar = false, inFString = false, escape = false;

        String[] lines = code.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#ifdef") || trimmed.startsWith("#ifndef") || trimmed.startsWith("#if ")) {
                preprocessorConditionCount++;
            } else if (trimmed.equals("#endif")) {
                preprocessorConditionCount--;
            }
        }

        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            if (escape) { escape = false; continue; }
            if (c == '\\' && (inString || inChar || inFString)) { escape = true; continue; }
            if (c == '"' && !inChar) {
                if (i > 0 && code.charAt(i - 1) == 'f' && !inString && !inFString) inFString = true;
                else if (inFString) inFString = false;
                else inString = !inString;
                continue;
            }
            if (c == '\'' && !inString && !inFString) { inChar = !inChar; continue; }
            if (inString || inChar || inFString) continue;
            switch (c) {
                case '{': braceCount++; break;
                case '}': braceCount--; break;
                case '(': parenCount++; break;
                case ')': parenCount--; break;
                case '[': bracketCount++; break;
                case ']': bracketCount--; break;
            }
        }
        if (inString || inChar || inFString) throw new RuntimeException("Unterminated string literal detected");
        return braceCount <= 0 && parenCount <= 0 && bracketCount <= 0 && preprocessorConditionCount <= 0;
    }

    private static String formatValue(Object value, Set<Object> seen) {
        if (seen == null) seen = new HashSet<>();
        if (value == null) return "null";
        if (!value.getClass().isArray()) return String.valueOf(value);
        if (seen.contains(value)) return "~";
        seen.add(value);
        StringBuilder sb = new StringBuilder("[");
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(formatValue(Array.get(value, i), seen));
        }
        sb.append("]");
        return sb.toString();
    }

    private ScriptResult makeErrorResult(String msg) {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setOutput("ERROR: " + msg);
        return r;
    }
}
