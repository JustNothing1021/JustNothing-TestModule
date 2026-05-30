package com.justnothing.testmodule.command.functions.script.impl;

import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.functions.script.ScriptResult;
import com.justnothing.testmodule.command.functions.script.request.*;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;

@SubCommandInfo(
    description = "脚本列表与交互式管理器",
    examples = {
        "script list                       列出所有脚本",
        "script vars                       列出脚本执行器变量",
        "script manage                     交互式管理器"
    }
)
public class ScriptManageCommand extends AbstractScriptCommand<ScriptBaseRequest, ScriptResult> {

    @Override
    protected ScriptResult executeInternal(ScriptBaseRequest request) throws Exception {
        if (request instanceof ScriptListRequest) {
            return handleList();
        }
        if (request instanceof ScriptVarsRequest) {
            return handleVars();
        }
        if (request instanceof ScriptManageRequest) {
            handleManage();
            return okResult("manage");
        }
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }

    private ScriptResult okResult(String subCmd) {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand(subCmd);
        return r;
    }

    protected ScriptResult handleList() {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("list");
        File scriptsDir = getScriptsDirectory();

        if (!scriptsDir.exists()) {
            IOManager.createDirectory(scriptsDir);
        }

        File[] scriptFiles = scriptsDir.listFiles();

        if (scriptFiles == null || scriptFiles.length == 0) {
            outln("没有找到脚本", Colors.GRAY);
            r.setSuccess(true);
            r.setScriptList(new java.util.ArrayList<>());
            return r;
        }

        outln("===== 脚本列表 =====", Colors.CYAN);
        outln("", Colors.WHITE);

        java.util.List<String> names = new java.util.ArrayList<>();
        for (File scriptFile : scriptFiles) {
            String name = scriptFile.getName();
            names.add(name);
            long size = scriptFile.length();
            long lastModified = scriptFile.lastModified();

            out("名称: ", Colors.CYAN);
            outln(name, Colors.YELLOW);
            out("  大小: ", Colors.CYAN);
            outln(formatSize(size), Colors.GREEN);
            out("  修改时间: ", Colors.CYAN);
            outln(formatTime(lastModified), Colors.GREEN);
            out("  路径: ", Colors.CYAN);
            outln(scriptFile.getAbsolutePath(), Colors.GRAY);
            outln("", Colors.WHITE);
        }

        out("总计: ", Colors.CYAN);
        out(String.valueOf(scriptFiles.length), Colors.YELLOW);
        outln(" 个脚本", Colors.CYAN);

        r.setSuccess(true);
        r.setScriptList(names);
        return r;
    }

    protected ScriptResult handleVars() {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("vars");
        outln("===== 脚本执行器变量 =====", Colors.CYAN);
        outln("", Colors.WHITE);

        java.util.Map<String, Object> scriptVars = systemScriptRunner.getAllVariablesAsObject();
        if (scriptVars == null || scriptVars.isEmpty()) {
            outln("  (空)", Colors.GRAY);
            r.setSuccess(true);
            r.setVariables(new java.util.ArrayList<>());
            return r;
        }

        java.util.List<ScriptResult.VariableInfo> varList = new java.util.ArrayList<>();
        for (java.util.Map.Entry<String, Object> entry : scriptVars.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();
            Class<?> clazz = value != null ? value.getClass() : null;

            varList.add(new ScriptResult.VariableInfo(name, value));

            out(name, Colors.CYAN);
            out(" (Class: ", Colors.GRAY);
            out(clazz != null ? clazz.getName() : "null", Colors.YELLOW);
            out(", Hash = ", Colors.GRAY);
            outln(value != null ? Integer.toHexString(System.identityHashCode(value)) : "0", Colors.ORANGE);

            out("  ", Colors.DARK_GRAY);
            outln(value != null ? String.valueOf(value) : "null", Colors.GREEN);
            outln("", Colors.WHITE);
        }

        out("总计: ", Colors.CYAN);
        out(String.valueOf(scriptVars.size()), Colors.YELLOW);
        outln(" 个变量", Colors.CYAN);

        r.setSuccess(true);
        r.setVariables(varList);
        return r;
    }

    protected void handleManage() {
        outln("===== 交互式脚本管理器 =====", Colors.CYAN);
        outln("输入 'help' 查看可用命令, 'exit' 或 'quit' 退出", Colors.GRAY);
        outln("", Colors.WHITE);

        label: while (true) {
            String input = this.context.readLine("manage> ");

            if (input == null) {
                break;
            }

            input = input.trim();

            switch (input) {
                case "":
                    continue;
                case "exit":
                case "quit":
                case "0":
                    outln("退出脚本管理器", Colors.GREEN);
                    break label;
                case "help":
                case "?":
                    showManageHelp();
                    continue;
            }

            handleManageCommand(input);
        }

        outln("脚本管理器已退出", Colors.GREEN);
    }

    protected void showManageHelp() {
        outln("", Colors.WHITE);
        outln("可用命令:", Colors.CYAN);
        out("  create <name>        ", Colors.YELLOW);
        outln("- 创建新脚本", Colors.GRAY);
        out("  list                 ", Colors.YELLOW);
        outln("- 列出所有脚本和codebase文件", Colors.GRAY);
        out("  vars                 ", Colors.YELLOW);
        outln("- 列出脚本执行器变量", Colors.GRAY);
        out("  show <name>          ", Colors.YELLOW);
        outln("- 显示文件内容", Colors.GRAY);
        out("  edit <name>          ", Colors.YELLOW);
        outln("- 编辑脚本内容", Colors.GRAY);
        out("  delete <name>        ", Colors.YELLOW);
        outln("- 删除文件", Colors.GRAY);
        out("  run <name>           ", Colors.YELLOW);
        outln("- 执行脚本或codebase文件", Colors.GRAY);
        out("  import <path>        ", Colors.YELLOW);
        outln("- 导入文件", Colors.GRAY);
        out("  export <name> <path> ", Colors.YELLOW);
        outln("- 导出文件", Colors.GRAY);
        out("  codebase             ", Colors.YELLOW);
        outln("- 列出codebase文件", Colors.GRAY);
        out("  help                 ", Colors.YELLOW);
        outln("- 显示此帮助", Colors.GRAY);
        out("  exit / quit          ", Colors.YELLOW);
        outln("- 退出管理器", Colors.GRAY);
        outln("", Colors.WHITE);
    }

    @SuppressWarnings("fallthrough")
    protected void handleManageCommand(String input) {
        String[] parts = input.split("\\s+", 3);
        String cmd = parts[0].toLowerCase();

        try {
            switch (cmd) {
                case "1", "create" -> {
                    if (parts.length < 2) {
                        outln("用法: create <name>", Colors.GRAY);
                        break;
                    }
                    ScriptCrudCommand crud = new ScriptCrudCommand();
                    crud.context = this.context;
                    crud.handleCreate(parts[1]);
                }
                case "2", "list" -> handleList();
                case "vars" -> handleVars();
                case "3", "show" -> {
                    if (parts.length < 2) {
                        outln("用法: show <name>", Colors.GRAY);
                        break;
                    }
                    ScriptCrudCommand crud = new ScriptCrudCommand();
                    crud.context = this.context;
                    crud.handleShow(parts[1]);
                }
                case "edit" -> {
                    if (parts.length < 2) {
                        outln("用法: edit <name>", Colors.GRAY);
                        break;
                    }
                    handleEdit(parts[1]);
                }
                case "4", "delete" -> {
                    if (parts.length < 2) {
                        outln("用法: delete <name>", Colors.GRAY);
                        break;
                    }
                    ScriptCrudCommand crud = new ScriptCrudCommand();
                    crud.context = this.context;
                    crud.handleDelete(parts[1]);
                }
                case "5", "run" -> {
                    if (parts.length < 2) {
                        outln("用法: run <name>", Colors.GRAY);
                        break;
                    }
                    ScriptRunRequest runReq = new ScriptRunRequest();
                    runReq.setName(parts[1]);
                    ScriptExecCommand exec = new ScriptExecCommand();
                    exec.context = this.context;
                    exec.handleRun(runReq);
                }
                case "6", "import" -> {
                    if (parts.length < 2) {
                        outln("用法: import <path>", Colors.GRAY);
                        break;
                    }
                    ScriptImportRequest importReq = new ScriptImportRequest();
                    importReq.setFilePath(parts[1]);
                    ScriptExecCommand exec = new ScriptExecCommand();
                    exec.context = this.context;
                    exec.handleImport(importReq);
                }
                case "7", "export" -> {
                    if (parts.length < 3) {
                        outln("用法: export <name> <path>", Colors.GRAY);
                        break;
                    }
                    ScriptExportRequest exportReq = new ScriptExportRequest();
                    exportReq.setName(parts[1]);
                    exportReq.setFilePath(parts[2]);
                    ScriptExecCommand exec = new ScriptExecCommand();
                    exec.context = this.context;
                    exec.handleExport(exportReq);
                }
                case "codebase" -> handleCodebaseList();
                default -> {
                    out("未知命令: ", Colors.RED);
                    outln(cmd, Colors.YELLOW);
                    outln("输入 'help' 查看帮助", Colors.GRAY);
                }
            }
        } catch (Exception e) {
            out("错误: ", Colors.RED);
            outln(e.getMessage() != null ? e.getMessage() : "没有详细信息", Colors.ORANGE);
        }
    }

    protected void handleEdit(String name) throws IOException {
        File scriptFile = DataBridge.getScriptFile(name);

        if (!scriptFile.exists()) {
            out("错误: 脚本 '", Colors.RED);
            out(name, Colors.YELLOW);
            outln("' 不存在", Colors.RED);
            return;
        }

        String existingContent = IOManager.readFile(scriptFile.getAbsolutePath());
        out("编辑脚本: ", Colors.CYAN);
        outln(name, Colors.YELLOW);
        outln("当前内容 (输入空行结束编辑):", Colors.GRAY);
        outln("", Colors.WHITE);
        outln(existingContent, Colors.WHITE);
        outln("", Colors.WHITE);
        outln("--- 开始编辑 (输入空行保存并退出) ---", Colors.CYAN);
        outln("", Colors.WHITE);

        StringBuilder newContent = new StringBuilder();

        while (true) {
            String line = this.context.readLine("");
            if (line == null || line.isEmpty()) {
                break;
            }
            newContent.append(line).append("\n");
        }

        if (newContent.length() > 0) {
            IOManager.writeFile(scriptFile.getAbsolutePath(), newContent.toString());
            outln("脚本已保存", Colors.GREEN);
        } else {
            outln("编辑已取消 (未做更改)", Colors.GRAY);
        }
    }

    protected void handleCodebaseList() {
        File codebaseDir = getScriptsDirectory();

        if (!codebaseDir.exists()) {
            outln("Codebase目录不存在", Colors.GRAY);
            return;
        }

        File[] files = codebaseDir.listFiles();
        if (files == null || files.length == 0) {
            outln("Codebase目录为空", Colors.GRAY);
            return;
        }

        outln("===== Codebase文件列表 =====", Colors.CYAN);
        outln("", Colors.WHITE);

        for (File file : files) {
            out("  ", Colors.GRAY);
            out(file.getName(), Colors.GREEN);
            if (file.isDirectory()) {
                outln("/", Colors.CYAN);
            } else {
                outln("", Colors.WHITE);
            }
        }

        outln("", Colors.WHITE);
        out("总计: ", Colors.CYAN);
        out(String.valueOf(files.length), Colors.YELLOW);
        outln(" 个文件", Colors.CYAN);
    }
}
