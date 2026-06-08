package com.justnothing.testmodule.command.functions.bsh.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshClearRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshExecuteRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptCreateRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptDeleteRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptEditRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptExportRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptImportRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptRunRequest;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public class BshManageCommand extends AbstractBeanShellCommand<CommandRequest> {

    public BshManageCommand() {
        super("bsh manage", CommandRequest.class);
    }

    @Override
    protected BeanShellResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        CommandRequest request = context.getRequest();

        if (request instanceof BshExecuteRequest req) {
            return handleExecute(req, context);
        } else if (request instanceof BshClearRequest req) {
            return handleClear(req, context);
        } else if (request instanceof BshScriptCreateRequest req) {
            return handleScriptCreate(req, context);
        } else if (request instanceof BshScriptEditRequest req) {
            return handleScriptEdit(req, context);
        } else if (request instanceof BshScriptDeleteRequest req) {
            return handleScriptDelete(req, context);
        } else if (request instanceof BshScriptRunRequest req) {
            return handleScriptRun(req, context);
        } else if (request instanceof BshScriptImportRequest req) {
            return handleScriptImport(req, context);
        } else if (request instanceof BshScriptExportRequest req) {
            return handleScriptExport(req, context);
        }

        return buildErrorResult("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    public BeanShellResult handleExecute(BshExecuteRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) {
        String code = request.getCode();
        ClassLoader classLoader = context.classLoader();

        if (code == null || code.isEmpty()) {
            context.println(getHelpText(), Colors.WHITE);
            return buildErrorResult("参数不足，需要提供BeanShell代码");
        }

        try {
            logger.info("执行BeanShell代码: " + code);
            String result = getBeanShellExecutor(classLoader).execute(code, beanShellExecutionContext);
            context.println("BeanShell执行器结果:", Colors.CYAN);
            context.println("", Colors.WHITE);
            context.println(result, Colors.GRAY);

            BeanShellResult bshResult = buildVariableResult("bsh", classLoader);
            bshResult.setCode(code);
            bshResult.setOutput(result);
            return bshResult;

        } catch (Exception e) {
            CommandExceptionHandler.handleException("bsh", e, context, "BeanShell执行出错");
            return buildErrorResult("BeanShell执行出错: " + e.getMessage());
        }
    }

    public BeanShellResult handleClear(BshClearRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) {
        ClassLoader classLoader = context.classLoader();

        getBeanShellExecutor(classLoader).clearVariables();
        context.println("已清空BeanShell执行器的所有变量", Colors.GREEN);
        context.print("提示: 只清空了", Colors.GRAY);
        context.println("的ClassLoader的执行器的变量，其他ClassLoader的并没有被清空", Colors.GRAY);

        return buildSuccessResult("bclear", "已清空变量");
    }

    public BeanShellResult handleScriptCreate(BshScriptCreateRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) throws IOException {
        String scriptName = request.getName();

        if (!isValidScriptName(scriptName)) {
            context.println("错误: 脚本名称只能包含字母、数字和下划线", Colors.RED);
            return buildErrorResult("脚本名称无效");
        }

        File scriptFile = getBeanShellScriptFile(scriptName);
        if (scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 已存在", Colors.RED);
            return buildErrorResult("脚本已存在");
        }

        IOManager.createDirectory(Objects.requireNonNull(scriptFile.getParentFile()).getAbsolutePath());

        String content = "# BeanShell Script: " + scriptName + "\n" +
                "# Created by: " + System.currentTimeMillis() + "\n" +
                "\n" +
                "# 在这里编写你的BeanShell脚本代码...\n";

        IOManager.writeFile(scriptFile.getAbsolutePath(), content);

        context.println("BeanShell脚本创建成功", Colors.GREEN);
        context.print("名称: ", Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print("路径: ", Colors.CYAN);
        context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
        context.println("提示: 使用 'bscript edit " + scriptName + "' 编辑脚本", Colors.GRAY);

        return buildSuccessResult("bscript:create", "脚本创建成功: " + scriptName);
    }

    public BeanShellResult handleScriptEdit(BshScriptEditRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) {
        String scriptName = request.getName();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return buildErrorResult("脚本不存在");
        }

        context.println("BeanShell脚本已准备好编辑", Colors.GREEN);
        context.print("名称: ", Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print("路径: ", Colors.CYAN);
        context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
        context.println("提示: 使用外部编辑器编辑脚本文件", Colors.GRAY);

        return buildSuccessResult("bscript:edit", "脚本已准备好编辑: " + scriptName);
    }

    public BeanShellResult handleScriptDelete(BshScriptDeleteRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) {
        String scriptName = request.getName();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return buildErrorResult("脚本不存在");
        }

        if (IOManager.deleteFile(scriptFile.getAbsolutePath())) {
            context.println("BeanShell脚本已删除", Colors.GREEN);
            context.print("名称: ", Colors.CYAN);
            context.println(scriptName, Colors.YELLOW);
            return buildSuccessResult("bscript:delete", "脚本已删除: " + scriptName);
        } else {
            context.println("错误: 无法删除脚本 '" + scriptName + "'", Colors.RED);
            return buildErrorResult("无法删除脚本");
        }
    }

    public BeanShellResult handleScriptRun(BshScriptRunRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) throws IOException {
        String scriptName = request.getName();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return buildErrorResult("脚本不存在");
        }

        String content = IOManager.readFile(scriptFile.getAbsolutePath());

        context.println("===== 执行BeanShell脚本: " + scriptName + " =====", Colors.CYAN);
        context.println("", Colors.WHITE);

        try {
            String execResult = getBeanShellExecutor(context.classLoader()).execute(content, beanShellExecutionContext);
            context.println("执行结果:", Colors.GREEN);
            context.println(execResult, Colors.GRAY);

            BeanShellResult result = buildVariableResult("bscript:run", context.classLoader());
            result.setOutput(execResult);
            return result;
        } catch (Exception e) {
            CommandExceptionHandler.handleException("bsh", e, context, "BeanShell执行出错");
            return buildErrorResult("BeanShell执行出错: " + e.getMessage());
        }
    }

    public BeanShellResult handleScriptImport(BshScriptImportRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) throws IOException {
        String filePath = request.getFilePath();
        File sourceFile = new File(filePath);

        if (!sourceFile.exists()) {
            context.println("错误: 文件 '" + filePath + "' 不存在", Colors.RED);
            return buildErrorResult("文件不存在");
        }

        String content = IOManager.readFile(sourceFile.getAbsolutePath());
        String scriptName = extractScriptName(sourceFile.getName());
        File destFile = getBeanShellScriptFile(scriptName);

        if (destFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 已存在", Colors.RED);
            context.println("提示: 使用 'bscript delete " + scriptName + "' 删除旧脚本", Colors.GRAY);
            return buildErrorResult("脚本已存在");
        }

        IOManager.createDirectory(Objects.requireNonNull(destFile.getParentFile()).getAbsolutePath());
        IOManager.writeFile(destFile.getAbsolutePath(), content);

        context.println("BeanShell脚本导入成功", Colors.GREEN);
        context.print("源文件: ", Colors.CYAN);
        context.println(sourceFile.getAbsolutePath(), Colors.GRAY);
        context.print("脚本名称: ", Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print("目标路径: ", Colors.CYAN);
        context.println(destFile.getAbsolutePath(), Colors.GRAY);

        return buildSuccessResult("bscript:import", "导入成功: " + scriptName);
    }

    public BeanShellResult handleScriptExport(BshScriptExportRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) throws IOException {
        String scriptName = request.getName();
        String exportPath = request.getExportPath();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return buildErrorResult("脚本不存在");
        }

        File exportFile = new File(exportPath);
        String content = IOManager.readFile(scriptFile.getAbsolutePath());

        IOManager.writeFile(exportFile.getAbsolutePath(), content);

        context.println("BeanShell脚本导出成功", Colors.GREEN);
        context.print("脚本: ", Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print("导出路径: ", Colors.CYAN);
        context.println(exportFile.getAbsolutePath(), Colors.GRAY);

        return buildSuccessResult("bscript:export", "导出成功: " + scriptName);
    }
}
