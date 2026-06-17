package com.justnothing.testmodule.command.functions.script.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.reflect.AppClassFinder;
import com.justnothing.testmodule.utils.sandbox.BlockGuardSandbox;

import com.justnothing.engine.security.SandboxConfig;

import com.justnothing.engine.ScriptRunner;
import com.justnothing.engine.eval.EvalException;
import com.justnothing.engine.parser.CythavaParseException;

import com.justnothing.testmodule.command.functions.script.ScriptResult;
import com.justnothing.testmodule.command.functions.script.request.ScriptBaseRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptRunRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptImportRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptExportRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptInteractiveRequest;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

@SubCommandInfo(
    description = "脚本执行引擎 - run/import/export/interactive",
    examples = {
        "script run <name>                 执行脚本",
        "script import <path>              导入外部文件",
        "script export <name> <path>       导出脚本文件",
        "script interactive                REPL 交互模式"
    }
)
public class ScriptExecCommand extends AbstractScriptCommand<ScriptBaseRequest, ScriptResult> {

    @Override
    protected ScriptResult executeInternal(ScriptBaseRequest request) throws Exception {
        if (request instanceof ScriptRunRequest r) {
            return handleRun(r);
        }
        if (request instanceof ScriptImportRequest r) {
            return handleImport(r);
        }
        if (request instanceof ScriptExportRequest r) {
            return handleExport(r);
        }
        if (request instanceof ScriptInteractiveRequest) {
            runInteractiveMode();
            return okResult("interactive");
        }
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    protected ScriptResult handleRun(ScriptRunRequest request) throws IOException {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("run");
        String fileName = request.getName();

        if (fileName == null || fileName.isEmpty()) {
            context.println("错误: 需要指定文件名称", Colors.RED);
            context.println("用法: script [-p preset] run <name>", Colors.GRAY);
            r.setSuccess(false);
            r.setOutput("需要指定文件名称");
            return r;
        }

        File scriptsDir = DataBridge.getScriptsDirectory();
        File targetFile = new File(scriptsDir, fileName);

        if (!targetFile.exists()) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("' 不存在", Colors.RED);
            r.setSuccess(false);
            r.setOutput("文件不存在: " + fileName);
            return r;
        }

        String content = IOManager.readFile(targetFile.getAbsolutePath());
        r.setScriptName(fileName);
        r.setCode(content);

        context.print("===== 执行脚本: ", Colors.CYAN);
        context.print(fileName, Colors.YELLOW);
        context.println(" =====", Colors.CYAN);
        context.println("[脚本执行] 在独立线程中执行...", Colors.CYAN);
        context.println("", Colors.WHITE);

        long startTime = System.currentTimeMillis();
        SandboxConfig config = currentPermissionConfig.get();
        AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

        Future<?> future = ThreadPoolManager.submitIOCallable(() -> {
            try {
                ScriptRunner runner = new ScriptRunner(context.classLoader());
                runner.setClassFinder(new AppClassFinder());

                if (config != null) {
                    if (config.getAstPermissionChecker() != null) {
                        runner.setPermissionChecker(config.getAstPermissionChecker());
                    }
                    BlockGuardSandbox.execute(config, () -> {
                        try {
                            runner.execute(content, context.output(), context.output());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    runner.execute(content, context.output(), context.output());
                }
            } catch (Throwable e) {
                errorRef.set(e);
            }
            return null;
        });

        try {
            assert future != null : "无法获取用于执行代码的Future";
            future.get(5, TimeUnit.MINUTES);
            long elapsed = System.currentTimeMillis() - startTime;
            r.setExecutionTimeMs(elapsed);

            if (errorRef.get() != null) {
                Throwable e = errorRef.get();
                Throwable cause = e;
                while (cause instanceof RuntimeException && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                if (cause instanceof SecurityException) {
                    context.print("权限拒绝: ", Colors.RED);
                    context.println(Objects.requireNonNullElse(cause.getMessage(), "没有详细信息"), Colors.ORANGE);
                } else {
                    CommandExceptionHandler.handleException("script run", e, context, "脚本执行失败");
                }
                r.setSuccess(false);
                r.setOutput(cause.getMessage());
            } else {
                context.println("脚本执行成功", Colors.GREEN);
                r.setSuccess(true);
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            context.println("脚本执行超时（5分钟），已取消", Colors.RED);
            r.setSuccess(false);
            r.setOutput("脚本执行超时");
        } catch (Exception e) {
            context.print("等待脚本执行结果时发生异常: ", Colors.RED);
            context.println(Objects.requireNonNullElse(e.getMessage(), "没有详细信息"), Colors.ORANGE);
            r.setSuccess(false);
            r.setOutput(e.getMessage());
        }

        return r;
    }

    protected ScriptResult handleImport(ScriptImportRequest request) throws IOException {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("import");
        String filePath = request.getFilePath();

        if (filePath == null || filePath.isEmpty()) {
            context.println("错误: 需要指定文件路径", Colors.RED);
            context.println("用法: script import <file>", Colors.GRAY);
            r.setSuccess(false);
            r.setOutput("需要指定文件路径");
            return r;
        }

        File sourceFile = new File(filePath);

        if (!sourceFile.exists()) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(filePath, Colors.YELLOW);
            context.println("' 不存在", Colors.RED);
            r.setSuccess(false);
            r.setOutput("文件不存在: " + filePath);
            return r;
        }

        String content = IOManager.readFile(sourceFile.getAbsolutePath());
        String fileName = sourceFile.getName();
        File scriptsDir = DataBridge.getScriptsDirectory();
        IOManager.createDirectory(scriptsDir);
        File destFile = new File(scriptsDir, fileName);

        if (destFile.exists()) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("' 已存在", Colors.RED);
            context.print("提示: 使用 '", Colors.GRAY);
            context.print("script delete " + fileName, Colors.CYAN);
            context.println("' 删除旧文件", Colors.GRAY);
            r.setSuccess(false);
            r.setOutput("目标文件已存在: " + fileName);
            return r;
        }

        IOManager.createDirectory(Objects.requireNonNull(destFile.getParentFile()).getAbsolutePath());
        IOManager.writeFile(destFile.getAbsolutePath(), content);

        context.println("文件导入成功", Colors.GREEN);
        context.print("源文件: ", Colors.CYAN);
        context.println(sourceFile.getAbsolutePath(), Colors.GREEN);
        context.print("文件名称: ", Colors.CYAN);
        context.println(fileName, Colors.YELLOW);
        context.print("目标路径: ", Colors.CYAN);
        context.println(destFile.getAbsolutePath(), Colors.GREEN);

        r.setSuccess(true);
        r.setImportedName(fileName);
        return r;
    }

    protected ScriptResult handleExport(ScriptExportRequest request) throws IOException {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("export");
        String fileName = request.getName();
        String exportPath = request.getFilePath();

        if (fileName == null || fileName.isEmpty() || exportPath == null || exportPath.isEmpty()) {
            context.println("错误: 需要指定文件名称和导出路径", Colors.RED);
            context.println("用法: script export <name> <file>", Colors.GRAY);
            r.setSuccess(false);
            r.setOutput("需要指定文件名称和导出路径");
            return r;
        }

        File sourceFile = null;
        String fileType = null;

        File scriptFile = DataBridge.getScriptFile(fileName);
        if (scriptFile.exists()) {
            sourceFile = scriptFile;
            fileType = "脚本";
        } else {
            File codebaseDir = DataBridge.getScriptsDirectory();
            File codebaseFile = new File(codebaseDir, fileName);
            if (codebaseFile.exists()) {
                sourceFile = codebaseFile;
                fileType = "Codebase文件";
            }
        }

        if (sourceFile == null) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("' 不存在（已检查脚本和codebase目录）", Colors.RED);
            r.setSuccess(false);
            r.setOutput("文件不存在: " + fileName);
            return r;
        }

        File exportFile = new File(exportPath);
        String content = IOManager.readFile(sourceFile.getAbsolutePath());

        IOManager.writeFile(exportFile.getAbsolutePath(), content);

        context.print(fileType, Colors.GREEN);
        context.println("导出成功", Colors.GREEN);
        context.print("文件: ", Colors.CYAN);
        context.println(fileName, Colors.YELLOW);
        context.print("导出路径: ", Colors.CYAN);
        context.println(exportFile.getAbsolutePath(), Colors.GREEN);

        r.setSuccess(true);
        r.setScriptName(fileName);
        return r;
    }

    protected void runInteractiveMode() {
        ClassLoader classLoader = context.classLoader();
        ScriptRunner runner = getScriptExecutor(classLoader);

        logger.info("进入交互式脚本执行模式");
        context.println("====== 脚本交互执行模式 =====", Colors.CYAN);
        context.println("输入 'exit' 或 'quit' 退出 (你不会闲到拿这俩做变量名, 对吧)", Colors.GRAY);
        context.println("输入 ':multi' 进入多行模式, ':eval' 执行, ':clear' 清空", Colors.GRAY);
        context.println("输入 'setPrintAST(true)' 开启 AST 打印", Colors.GRAY);
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
                        if (autoMultiLine) {
                            continue;
                        }
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

                    if (fullCode.isEmpty()) {
                        context.println("没有代码可执行", Colors.GRAY);
                        continue;
                    }

                    executeCode(runner, fullCode);
                    continue;
                }

                if (multiLineMode || autoMultiLine) {
                    multiLineBuffer.append(code).append("\n");

                    if (autoMultiLine && isCodeComplete(multiLineBuffer.toString())) {
                        String fullCode = multiLineBuffer.toString();
                        multiLineBuffer.setLength(0);
                        autoMultiLine = false;

                        executeCode(runner, fullCode);
                    }
                    continue;
                }

                if (!isCodeComplete(code)) {
                    multiLineBuffer.append(code).append("\n");
                    autoMultiLine = true;
                    continue;
                }

                executeCode(runner, code);
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
                handleExecutionException(e, runner);
            }
        }
        context.println("交互式模式已退出", Colors.GREEN);
    }

    protected void executeCode(ScriptRunner runner, String code) {
        SandboxConfig config = currentPermissionConfig.get();

        if (config != null) {
            if (config.getAstPermissionChecker() != null) {
                runner.setPermissionChecker(config.getAstPermissionChecker());
            }
            try {
                BlockGuardSandbox.execute(config, () -> {
                    Object result = runner.executeWithResult(code, context.output(), context.output());
                    if (result != null) {
                        context.println(formatValue(result, new HashSet<>()), Colors.GRAY);
                    }
                });
            } catch (Throwable e) {
                handleExecutionException(e, runner);
            }
        } else {
            runner.setPermissionChecker(null);
            try {
                Object result = runner.executeWithResult(code, context.output(), context.output());
                if (result != null) {
                    context.println(formatValue(result, new HashSet<>()), Colors.GRAY);
                }
            } catch (Throwable e) {
                handleExecutionException(e, runner);
            }
        }
    }

    protected void handleExecutionException(Throwable e, ScriptRunner runner) {
        Throwable cause = e.getCause();
        String message = e.getMessage();
        boolean isParseError = message != null && message.startsWith("Parse error:");

        byte errorColor = isParseError ? Colors.ORANGE : Colors.RED;

        if (cause instanceof EvalException evalEx) {
            Throwable innerCause = evalEx.getCause();
            context.print("执行错误: ", errorColor);
            context.println(evalEx.getMessage(), errorColor);
            if (runner.isPrintAST()) {
                context.println("（AST 节点信息在此版本中不可用）", Colors.GRAY);
            }
            if (innerCause != null) {
                context.output().printStackTrace(innerCause, Colors.GRAY);
            }
        } else if (cause instanceof CythavaParseException parseEx) {
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

    protected static boolean isCodeComplete(String code) {
        int braceCount = 0;
        int parenCount = 0;
        int bracketCount = 0;
        int preprocessorConditionCount = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean inFString = false;
        boolean escape = false;

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

            if (escape) {
                escape = false;
                continue;
            }

            if (c == '\\' && (inString || inChar || inFString)) {
                escape = true;
                continue;
            }

            if (c == '"' && !inChar) {
                if (i > 0 && code.charAt(i - 1) == 'f' && !inString && !inFString) {
                    inFString = true;
                } else if (inFString) {
                    inFString = false;
                } else {
                    inString = !inString;
                }
                continue;
            }

            if (c == '\'' && !inString && !inFString) {
                inChar = !inChar;
                continue;
            }

            if (inString || inChar || inFString) {
                continue;
            }

            switch (c) {
                case '{':
                    braceCount++;
                    break;
                case '}':
                    braceCount--;
                    break;
                case '(':
                    parenCount++;
                    break;
                case ')':
                    parenCount--;
                    break;
                case '[':
                    bracketCount++;
                    break;
                case ']':
                    bracketCount--;
                    break;
            }
        }

        if (inString || inChar || inFString) {
            throw new RuntimeException("Unterminated string literal detected");
        }

        return braceCount <= 0 && parenCount <= 0 && bracketCount <= 0 && preprocessorConditionCount <= 0;
    }

    private static String formatValue(Object value, Set<Object> seen) {
        if (seen == null)
            seen = new HashSet<>();

        if (value == null) {
            return "null";
        }

        if (!value.getClass().isArray()) {
            return String.valueOf(value);
        }

        if (seen.contains(value)) {
            return "~";
        }
        seen.add(value);

        value.getClass().isArray();
        StringBuilder sb = new StringBuilder("[");
        int length = Array.getLength(value);
        for (int i = 0; i < length; i++) {
            if (i > 0)
                sb.append(", ");
            Object elem = Array.get(value, i);
            sb.append(formatValue(elem, seen));
        }
        sb.append("]");
        return sb.toString();

    }

    private ScriptResult okResult(String subCmd) {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand(subCmd);
        return r;
    }
}
