package com.justnothing.testmodule.command.functions.bsh.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptListRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshScriptShowRequest;
import com.justnothing.testmodule.command.functions.bsh.request.BshVarsRequest;
import com.justnothing.testmodule.command.functions.bsh.response.BeanShellResult;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class BshQueryCommand extends AbstractBeanShellCommand<CommandRequest> {

    public BshQueryCommand() {
        super("bsh query", CommandRequest.class);
    }

    @Override
    protected BeanShellResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        CommandRequest request = context.getRequest();

        if (request instanceof BshVarsRequest req) {
            return handleVars(req, context);
        } else if (request instanceof BshScriptListRequest req) {
            return handleScriptList(req, context);
        } else if (request instanceof BshScriptShowRequest req) {
            return handleScriptShow(req, context);
        }

        return buildErrorResult("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    public BeanShellResult handleVars(BshVarsRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();

        context.print("(当前ClassLoader: ", Colors.GRAY);
        context.println((targetPackage == null ? "默认加载器" : targetPackage) + ")", Colors.YELLOW);
        context.println("", Colors.WHITE);
        context.println("BeanShell执行器的变量列表:", Colors.CYAN);

        Map<String, Object> bshVars = getBeanShellExecutor(classLoader).getVariables();
        if (bshVars.isEmpty()) {
            context.println("  (空)", Colors.GRAY);
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

    public BeanShellResult handleScriptList(BshScriptListRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) {
        File scriptsDir = DataBridge.getScriptsDirectory();
        if (!scriptsDir.exists()) {
            context.println("脚本目录不存在: " + scriptsDir.getAbsolutePath(), Colors.RED);
            return buildErrorResult("脚本目录不存在");
        }

        File[] scriptFiles = scriptsDir.listFiles((dir, name) -> name.endsWith(".bsh"));

        if (scriptFiles == null || scriptFiles.length == 0) {
            context.println("没有找到BeanShell脚本", Colors.GRAY);
            return buildSuccessResult("bscript:list", "没有找到脚本");
        }

        context.println("===== BeanShell脚本列表 =====", Colors.CYAN);
        context.println("", Colors.WHITE);

        for (File scriptFile : scriptFiles) {
            String name = scriptFile.getName();
            long size = scriptFile.length();
            long lastModified = scriptFile.lastModified();

            context.print("名称: ", Colors.CYAN);
            context.println(name, Colors.GREEN);
            context.print("  大小: ", Colors.CYAN);
            context.println(formatSize(size), Colors.YELLOW);
            context.print("  修改时间: ", Colors.CYAN);
            context.println(formatTime(lastModified), Colors.GRAY);
            context.print("  路径: ", Colors.CYAN);
            context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
            context.println("", Colors.WHITE);
        }

        context.print("总计: ", Colors.CYAN);
        context.println(scriptFiles.length + " 个脚本", Colors.YELLOW);

        return buildSuccessResult("bscript:list", scriptFiles.length + " 个脚本");
    }

    public BeanShellResult handleScriptShow(BshScriptShowRequest request, CommandExecutor.CmdExecContext<CommandRequest> context) throws IOException {
        String scriptName = request.getName();
        File scriptFile = getBeanShellScriptFile(scriptName);

        if (!scriptFile.exists()) {
            context.println("错误: 脚本 '" + scriptName + "' 不存在", Colors.RED);
            return buildErrorResult("脚本 '" + scriptName + "' 不存在");
        }

        String content = IOManager.readFile(scriptFile.getAbsolutePath());
        context.println("===== BeanShell脚本内容: " + scriptName + " =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        context.println(content, Colors.GRAY);

        return buildSuccessResult("bscript:show", content);
    }
}
