package com.justnothing.testmodule.command.functions.bsh.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.bsh.BshTexts;
import com.justnothing.testmodule.command.functions.bsh.request.BshClearRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshExecuteRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptCreateRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptDeleteRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptEditRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptExportRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptImportRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptRunRequest;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class BshManageCommand extends AbstractBeanShellCommand<CommandRequest<?>> {

    @SuppressWarnings("unchecked")
    public BshManageCommand() {
        super("bsh manage", (Class) CommandRequest.class);
    }

    @Override
    protected BeanShellResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        CommandRequest<?> request = context.getRequest();

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

        return buildErrorResult(BshTexts.ERR_UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }

    public BeanShellResult handleExecute(BshExecuteRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String code = request.getCode();
        ClassLoader classLoader = context.classLoader();

        if (code == null || code.isEmpty()) {
            context.println(getHelpText(), Colors.WHITE);
            return buildErrorResult(Text.zhEn("参数不足，需要提供BeanShell代码", "Not enough arguments; BeanShell code is required").text());
        }

        try {
            logger.info("执行BeanShell代码: " + code);
            String result = getBeanShellExecutor(classLoader).execute(code, beanShellExecutionContext);
            context.println(Text.zhEn("BeanShell执行器结果:", "BeanShell executor result:").text(), Colors.CYAN);
            context.println("", Colors.WHITE);
            context.println(result, Colors.GRAY);

            BeanShellResult bshResult = buildVariableResult("bsh", classLoader);
            bshResult.setCode(code);
            bshResult.setOutput(result);
            return bshResult;

        } catch (Exception e) {
            CommandExceptionHandler.handleException("bsh", e, context, BshTexts.ERR_BSH_EXEC_FAILED.text());
            return buildErrorResult(BshTexts.ERR_BSH_EXEC_FAILED.text() + ": " + e.getMessage());
        }
    }

    public BeanShellResult handleClear(BshClearRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();

        getBeanShellExecutor(classLoader).clearVariables();
        context.println(Text.zhEn("已清空BeanShell执行器的所有变量", "Cleared all variables of the BeanShell executor").text(), Colors.GREEN);
        context.print(Text.zhEn("提示: 只清空了", "Tip: only cleared the variables of ").text(), Colors.GRAY);
        context.print(targetPackage == null ? Text.zhEn("默认", "the default").text() : targetPackage, Colors.YELLOW);
        context.println(Text.zhEn("的ClassLoader的执行器的变量，其他ClassLoader的并没有被清空", " ClassLoader's executor; other ClassLoaders were left untouched").text(), Colors.GRAY);

        return buildSuccessResult("bclear", Text.zhEn("已清空变量", "Variables cleared").text());
    }

    public BeanShellResult handleScriptCreate(BshScriptCreateRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws IOException {
        String scriptName = request.getName();

        if (!isValidScriptName(scriptName)) {
            context.println(CliMessages.ERROR_PREFIX.text() + Text.zhEn("脚本名称只能包含字母、数字和下划线", "Script name may contain only letters, digits and underscores").text(), Colors.RED);
            return buildErrorResult(Text.zhEn("脚本名称无效", "Invalid script name").text());
        }

        File scriptFile = getBeanShellScriptFile(scriptName);
        if (scriptFile.exists()) {
            context.println(CliMessages.ERROR_PREFIX.text() + BshTexts.ERR_SCRIPT_EXISTS.format(scriptName), Colors.RED);
            return buildErrorResult(BshTexts.ERR_SCRIPT_ALREADY_EXISTS.text());
        }

        IOManager.createDirectory(Objects.requireNonNull(scriptFile.getParentFile()).getAbsolutePath());

        String content = "# BeanShell Script: " + scriptName + "\n" +
                "# Created by: " + System.currentTimeMillis() + "\n" +
                "\n" +
                Text.zhEn("# 在这里编写你的BeanShell脚本代码...\n", "# Write your BeanShell script code here...\n").text();

        IOManager.writeFile(scriptFile.getAbsolutePath(), content);

        context.println(Text.zhEn("BeanShell脚本创建成功", "BeanShell script created").text(), Colors.GREEN);
        context.print(BshTexts.LABEL_NAME.text(), Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print(BshTexts.LABEL_PATH.text(), Colors.CYAN);
        context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
        context.println(BshTexts.TIP_PREFIX.text() + Text.zhEn("使用 'bscript edit %s' 编辑脚本", "use 'bscript edit %s' to edit the script").format(scriptName), Colors.GRAY);

        return buildSuccessResult("bscript:create", Text.zhEn("脚本创建成功: %s", "Script created: %s").format(scriptName));
    }

    public BeanShellResult handleScriptEdit(BshScriptEditRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String scriptName = request.getName();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println(CliMessages.ERROR_PREFIX.text() + BshTexts.ERR_SCRIPT_NOT_FOUND.format(scriptName), Colors.RED);
            return buildErrorResult(BshTexts.ERR_SCRIPT_MISSING.text());
        }

        context.println(Text.zhEn("BeanShell脚本已准备好编辑", "BeanShell script is ready to edit").text(), Colors.GREEN);
        context.print(BshTexts.LABEL_NAME.text(), Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print(BshTexts.LABEL_PATH.text(), Colors.CYAN);
        context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
        context.println(BshTexts.TIP_PREFIX.text() + Text.zhEn("使用外部编辑器编辑脚本文件", "edit the script file with an external editor").text(), Colors.GRAY);

        return buildSuccessResult("bscript:edit", Text.zhEn("脚本已准备好编辑: %s", "Script is ready to edit: %s").format(scriptName));
    }

    public BeanShellResult handleScriptDelete(BshScriptDeleteRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        String scriptName = request.getName();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println(CliMessages.ERROR_PREFIX.text() + BshTexts.ERR_SCRIPT_NOT_FOUND.format(scriptName), Colors.RED);
            return buildErrorResult(BshTexts.ERR_SCRIPT_MISSING.text());
        }

        if (IOManager.deleteFile(scriptFile.getAbsolutePath())) {
            context.println(Text.zhEn("BeanShell脚本已删除", "BeanShell script deleted").text(), Colors.GREEN);
            context.print(BshTexts.LABEL_NAME.text(), Colors.CYAN);
            context.println(scriptName, Colors.YELLOW);
            return buildSuccessResult("bscript:delete", Text.zhEn("脚本已删除: %s", "Script deleted: %s").format(scriptName));
        } else {
            context.println(CliMessages.ERROR_PREFIX.text() + Text.zhEn("无法删除脚本 '%s'", "Cannot delete script '%s'").format(scriptName), Colors.RED);
            return buildErrorResult(Text.zhEn("无法删除脚本", "Cannot delete script").text());
        }
    }

    public BeanShellResult handleScriptRun(BshScriptRunRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws IOException {
        String scriptName = request.getName();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println(CliMessages.ERROR_PREFIX.text() + BshTexts.ERR_SCRIPT_NOT_FOUND.format(scriptName), Colors.RED);
            return buildErrorResult(BshTexts.ERR_SCRIPT_MISSING.text());
        }

        String content = IOManager.readFile(scriptFile.getAbsolutePath());

        context.println(Text.zhEn("===== 执行BeanShell脚本: %s =====", "===== Running BeanShell script: %s =====").format(scriptName), Colors.CYAN);
        context.println("", Colors.WHITE);

        try {
            String execResult = getBeanShellExecutor(context.classLoader()).execute(content, beanShellExecutionContext);
            context.println(Text.zhEn("执行结果:", "Result:").text(), Colors.GREEN);
            context.println(execResult, Colors.GRAY);

            BeanShellResult result = buildVariableResult("bscript:run", context.classLoader());
            result.setOutput(execResult);
            return result;
        } catch (Exception e) {
            CommandExceptionHandler.handleException("bsh", e, context, BshTexts.ERR_BSH_EXEC_FAILED.text());
            return buildErrorResult(BshTexts.ERR_BSH_EXEC_FAILED.text() + ": " + e.getMessage());
        }
    }

    public BeanShellResult handleScriptImport(BshScriptImportRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws IOException {
        String filePath = request.getFilePath();
        File sourceFile = new File(filePath);

        if (!sourceFile.exists()) {
            context.println(CliMessages.ERROR_PREFIX.text() + Text.zhEn("文件 '%s' 不存在", "File '%s' does not exist").format(filePath), Colors.RED);
            return buildErrorResult(Text.zhEn("文件不存在", "File does not exist").text());
        }

        String content = IOManager.readFile(sourceFile.getAbsolutePath());
        String scriptName = extractScriptName(sourceFile.getName());
        File destFile = getBeanShellScriptFile(scriptName);

        if (destFile.exists()) {
            context.println(CliMessages.ERROR_PREFIX.text() + BshTexts.ERR_SCRIPT_EXISTS.format(scriptName), Colors.RED);
            context.println(BshTexts.TIP_PREFIX.text() + Text.zhEn("使用 'bscript delete %s' 删除旧脚本", "use 'bscript delete %s' to delete the old script").format(scriptName), Colors.GRAY);
            return buildErrorResult(BshTexts.ERR_SCRIPT_ALREADY_EXISTS.text());
        }

        IOManager.createDirectory(Objects.requireNonNull(destFile.getParentFile()).getAbsolutePath());
        IOManager.writeFile(destFile.getAbsolutePath(), content);

        context.println(Text.zhEn("BeanShell脚本导入成功", "BeanShell script imported").text(), Colors.GREEN);
        context.print(Text.zhEn("源文件: ", "Source: ").text(), Colors.CYAN);
        context.println(sourceFile.getAbsolutePath(), Colors.GRAY);
        context.print(Text.zhEn("脚本名称: ", "Script name: ").text(), Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print(Text.zhEn("目标路径: ", "Destination: ").text(), Colors.CYAN);
        context.println(destFile.getAbsolutePath(), Colors.GRAY);

        return buildSuccessResult("bscript:import", Text.zhEn("导入成功: %s", "Imported: %s").format(scriptName));
    }

    public BeanShellResult handleScriptExport(BshScriptExportRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws IOException {
        String scriptName = request.getName();
        String exportPath = request.getExportPath();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println(CliMessages.ERROR_PREFIX.text() + BshTexts.ERR_SCRIPT_NOT_FOUND.format(scriptName), Colors.RED);
            return buildErrorResult(BshTexts.ERR_SCRIPT_MISSING.text());
        }

        File exportFile = new File(exportPath);
        String content = IOManager.readFile(scriptFile.getAbsolutePath());

        IOManager.writeFile(exportFile.getAbsolutePath(), content);

        context.println(Text.zhEn("BeanShell脚本导出成功", "BeanShell script exported").text(), Colors.GREEN);
        context.print(Text.zhEn("脚本: ", "Script: ").text(), Colors.CYAN);
        context.println(scriptName, Colors.YELLOW);
        context.print(Text.zhEn("导出路径: ", "Export path: ").text(), Colors.CYAN);
        context.println(exportFile.getAbsolutePath(), Colors.GRAY);

        return buildSuccessResult("bscript:export", Text.zhEn("导出成功: %s", "Exported: %s").format(scriptName));
    }
}
