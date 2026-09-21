package com.justnothing.testmodule.command.functions.bsh.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.functions.bsh.BshTexts;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptListRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptShowRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshVarsRequest;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class BshQueryCommand extends AbstractBeanShellCommand<CommandRequest<?>> {

    @SuppressWarnings("unchecked")
    public BshQueryCommand() {
        super("bsh query", (Class) CommandRequest.class);
    }

    @Override
    protected BeanShellResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
        CommandRequest<?> request = context.getRequest();

        if (request instanceof BshVarsRequest req) {
            return handleVars(req, context);
        } else if (request instanceof BshScriptListRequest req) {
            return handleScriptList(req, context);
        } else if (request instanceof BshScriptShowRequest req) {
            return handleScriptShow(req, context);
        }

        return buildErrorResult(BshTexts.ERR_UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }

    public BeanShellResult handleVars(BshVarsRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();

        context.print(Text.zhEn("(当前ClassLoader: ", "(Current ClassLoader: ").text(), Colors.GRAY);
        context.println((targetPackage == null ? Text.zhEn("默认加载器", "default loader").text() : targetPackage) + ")", Colors.YELLOW);
        context.println("", Colors.WHITE);
        context.println(Text.zhEn("BeanShell执行器的变量列表:", "BeanShell executor variables:").text(), Colors.CYAN);

        Map<String, Object> bshVars = getBeanShellExecutor(classLoader).getVariables();
        if (bshVars.isEmpty()) {
            context.println(Text.zhEn("  (空)", "  (empty)").text(), Colors.GRAY);
        } else {
            for (Map.Entry<String, Object> entry : bshVars.entrySet()) {
                Object value = entry.getValue();
                context.print("  " + entry.getKey() + " = ", Colors.CYAN);
                context.print(String.valueOf(value), Colors.GREEN);
                context.println(" (" + (value != null ? value.getClass().getSimpleName() : "null") + ")", Colors.GRAY);
            }
        }

        return buildVariableResult("bvars", classLoader);
    }

    public BeanShellResult handleScriptList(BshScriptListRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) {
        File scriptsDir = DataBridge.getScriptsDirectory();
        if (!scriptsDir.exists()) {
            context.println(BshTexts.ERR_SCRIPT_DIR_NOT_FOUND.text() + ": " + scriptsDir.getAbsolutePath(), Colors.RED);
            return buildErrorResult(BshTexts.ERR_SCRIPT_DIR_NOT_FOUND.text());
        }

        File[] scriptFiles = scriptsDir.listFiles((dir, name) -> name.endsWith(".bsh"));

        if (scriptFiles == null || scriptFiles.length == 0) {
            context.println(Text.zhEn("没有找到BeanShell脚本", "No BeanShell scripts found").text(), Colors.GRAY);
            return buildSuccessResult("bscript:list", Text.zhEn("没有找到脚本", "No scripts found").text());
        }

        context.println(Text.zhEn("===== BeanShell脚本列表 =====", "===== BeanShell scripts =====").text(), Colors.CYAN);
        context.println("", Colors.WHITE);

        for (File scriptFile : scriptFiles) {
            String name = scriptFile.getName();
            long size = scriptFile.length();
            long lastModified = scriptFile.lastModified();

            context.print(BshTexts.LABEL_NAME.text(), Colors.CYAN);
            context.println(name, Colors.GREEN);
            context.print(Text.zhEn("  大小: ", "  Size: ").text(), Colors.CYAN);
            context.println(formatSize(size), Colors.YELLOW);
            context.print(Text.zhEn("  修改时间: ", "  Last modified: ").text(), Colors.CYAN);
            context.println(formatTime(lastModified), Colors.GRAY);
            context.print(Text.zhEn("  路径: ", "  Path: ").text(), Colors.CYAN);
            context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
            context.println("", Colors.WHITE);
        }

        context.print(Text.zhEn("总计: ", "Total: ").text(), Colors.CYAN);
        context.println(BshTexts.SCRIPT_COUNT.format(scriptFiles.length), Colors.YELLOW);

        return buildSuccessResult("bscript:list", BshTexts.SCRIPT_COUNT.format(scriptFiles.length));
    }

    public BeanShellResult handleScriptShow(BshScriptShowRequest request, CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws IOException {
        String scriptName = request.getName();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println(CliMessages.ERROR_PREFIX.text() + BshTexts.ERR_SCRIPT_NOT_FOUND.format(scriptName), Colors.RED);
            return buildErrorResult(BshTexts.ERR_SCRIPT_NOT_FOUND.format(scriptName));
        }

        String content = IOManager.readFile(scriptFile.getAbsolutePath());
        context.println(Text.zhEn("===== BeanShell脚本内容: %s =====", "===== BeanShell script: %s =====").format(scriptName), Colors.CYAN);
        context.println("", Colors.WHITE);
        context.println(content, Colors.GRAY);

        return buildSuccessResult("bscript:show", content);
    }
}
