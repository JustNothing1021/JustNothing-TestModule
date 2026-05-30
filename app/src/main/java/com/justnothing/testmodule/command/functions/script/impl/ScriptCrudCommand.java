package com.justnothing.testmodule.command.functions.script.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.script.ScriptResult;
import com.justnothing.testmodule.command.functions.script.request.ScriptBaseRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptCreateRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptDeleteRequest;
import com.justnothing.testmodule.command.functions.script.request.ScriptShowRequest;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.text.SimpleDateFormat;
import java.util.Objects;
import java.util.UUID;

@SubCommandInfo(
    description = "脚本 CRUD 操作 - 创建/显示/删除",
    examples = {
        "script create <name>              创建新脚本",
        "script show <name>                显示脚本内容",
        "script delete <name>              删除脚本"
    }
)
public class ScriptCrudCommand extends AbstractScriptCommand<ScriptBaseRequest, ScriptResult> {

    @Override
    protected ScriptResult executeInternal(ScriptBaseRequest request) throws Exception {
        if (request instanceof ScriptCreateRequest r) {
            return handleCreate(r.getName());
        }
        if (request instanceof ScriptShowRequest r) {
            return handleShow(r.getName());
        }
        if (request instanceof ScriptDeleteRequest r) {
            return handleDelete(r.getName());
        }
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    protected ScriptResult handleCreate(String scriptName) throws IOException {
        ScriptResult r = new ScriptResult(UUID.randomUUID().toString());
        r.setSubCommand("create");

        if (scriptName == null || scriptName.isEmpty()) {
            context.println("错误: 需要指定脚本名称", Colors.RED);
            context.println("用法: script create <name>", Colors.GRAY);
            r.setSuccess(false);
            r.setOutput("需要指定脚本名称");
            return r;
        }

        if (!isValidScriptName(scriptName)) {
            context.println("错误: 脚本名称只能包含字母、数字和下划线", Colors.RED);
            r.setSuccess(false);
            r.setOutput("脚本名称无效: " + scriptName);
            return r;
        }

        File scriptFile = DataBridge.getScriptFile(scriptName);
        if (scriptFile.exists()) {
            context.print("错误: 脚本 '", Colors.RED);
            context.print(scriptName, Colors.YELLOW);
            context.println("' 已存在", Colors.RED);
            r.setSuccess(false);
            r.setOutput("脚本已存在: " + scriptName);
            return r;
        }

        IOManager.createDirectory(Objects.requireNonNull(scriptFile.getParentFile()).getAbsolutePath());

        String content = "// Script: " + scriptName + "\n" +
                "// Created by: " +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date())
                +
                "\n" +
                "// 在这里编写你的脚本代码...\n";

        IOManager.writeFile(scriptFile.getAbsolutePath(), content);

        context.print("脚本 '", Colors.GREEN);
        context.print(scriptName, Colors.YELLOW);
        context.println("' 创建成功", Colors.GREEN);
        context.print("路径: ", Colors.CYAN);
        context.println(scriptFile.getAbsolutePath(), Colors.GREEN);

        r.setSuccess(true);
        r.setScriptName(scriptName);
        return r;
    }

    protected ScriptResult handleShow(String fileName) throws IOException {
        ScriptResult r = new ScriptResult(UUID.randomUUID().toString());
        r.setSubCommand("show");

        if (fileName == null || fileName.isEmpty()) {
            context.println("错误: 需要指定文件名称", Colors.RED);
            context.println("用法: script show <name>", Colors.GRAY);
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
        context.print("===== 文件内容: ", Colors.CYAN);
        context.print(fileName, Colors.YELLOW);
        context.println(" =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        context.println(content, Colors.WHITE);

        r.setSuccess(true);
        r.setScriptName(fileName);
        r.setCode(content);
        return r;
    }

    protected ScriptResult handleDelete(String fileName) {
        ScriptResult r = new ScriptResult(UUID.randomUUID().toString());
        r.setSubCommand("delete");

        if (fileName == null || fileName.isEmpty()) {
            context.println("错误: 需要指定文件名称", Colors.RED);
            context.println("用法: script delete <name>", Colors.GRAY);
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

        if (IOManager.deleteFile(targetFile.getAbsolutePath())) {
            context.print("文件 '", Colors.GREEN);
            context.print(fileName, Colors.YELLOW);
            context.println("' 已删除", Colors.GREEN);
            r.setSuccess(true);
        } else {
            context.print("错误: 无法删除文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("'", Colors.RED);
            r.setSuccess(false);
            r.setOutput("无法删除文件: " + fileName);
        }

        r.setDeletedName(fileName);
        return r;
    }
}
