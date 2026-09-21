package com.justnothing.testmodule.command.functions.script.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.script.ScriptTexts;
import com.justnothing.testmodule.command.functions.script.response.ScriptResult;
import com.justnothing.testmodule.command.functions.script.request.*;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.File;
import java.io.IOException;

@SubCommandInfo(
    description = ScriptTexts.SUB_SCRIPT_MANAGE_DESC,
    examples = {
        "script list                       列出所有脚本",
        "script vars                       列出脚本执行器变量",
        "script manage                     交互式管理器"
    }
)
public class ScriptManageCommand extends AbstractScriptCommand<ScriptBaseRequest<?>, ScriptResult> {

    @SuppressWarnings("unchecked")
    public ScriptManageCommand() {
        super("script manage", (Class) ScriptBaseRequest.class, ScriptResult.class);
    }

    @Override
    protected ScriptResult executeRequest(ScriptBaseRequest<?> request) throws Exception {
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
        throw new IllegalArgumentException(
                ScriptTexts.ERR_UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
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
            outln(Text.zhEn("没有找到脚本", "No scripts found").text(), Colors.GRAY);
            r.setSuccess(true);
            r.setScriptList(new java.util.ArrayList<>());
            return r;
        }

        outln(Text.zhEn("===== 脚本列表 =====", "===== Script list =====").text(), Colors.CYAN);
        outln("", Colors.WHITE);

        java.util.List<String> names = new java.util.ArrayList<>();
        for (File scriptFile : scriptFiles) {
            String name = scriptFile.getName();
            names.add(name);
            long size = scriptFile.length();
            long lastModified = scriptFile.lastModified();

            out(Text.zhEn("名称: ", "Name: ").text(), Colors.CYAN);
            outln(name, Colors.YELLOW);
            out(Text.zhEn("  大小: ", "  Size: ").text(), Colors.CYAN);
            outln(formatSize(size), Colors.GREEN);
            out(Text.zhEn("  修改时间: ", "  Modified: ").text(), Colors.CYAN);
            outln(formatTime(lastModified), Colors.GREEN);
            out("  " + ScriptTexts.LABEL_PATH.text(), Colors.CYAN);
            outln(scriptFile.getAbsolutePath(), Colors.GRAY);
            outln("", Colors.WHITE);
        }

        out(ScriptTexts.LABEL_TOTAL.text(), Colors.CYAN);
        out(String.valueOf(scriptFiles.length), Colors.YELLOW);
        outln(Text.zhEn(" 个脚本", " scripts").text(), Colors.CYAN);

        r.setSuccess(true);
        r.setScriptList(names);
        return r;
    }

    protected ScriptResult handleVars() {
        ScriptResult r = new ScriptResult(java.util.UUID.randomUUID().toString());
        r.setSubCommand("vars");
        outln(Text.zhEn("===== 脚本执行器变量 =====", "===== Interpreter variables =====").text(), Colors.CYAN);
        outln("", Colors.WHITE);

        java.util.Map<String, Object> scriptVars = systemScriptRunner.getAllVariablesAsObject();
        if (scriptVars == null || scriptVars.isEmpty()) {
            outln(Text.zhEn("  (空)", "  (empty)").text(), Colors.GRAY);
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

        out(ScriptTexts.LABEL_TOTAL.text(), Colors.CYAN);
        out(String.valueOf(scriptVars.size()), Colors.YELLOW);
        outln(Text.zhEn(" 个变量", " variables").text(), Colors.CYAN);

        r.setSuccess(true);
        r.setVariables(varList);
        return r;
    }

    protected void handleManage() {
        outln(Text.zhEn("===== 交互式脚本管理器 =====", "===== Interactive script manager =====").text(), Colors.CYAN);
        outln(Text.zhEn("输入 'help' 查看可用命令, 'exit' 或 'quit' 退出",
                "Type 'help' for available commands, 'exit' or 'quit' to leave").text(), Colors.GRAY);
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
                    outln(Text.zhEn("退出脚本管理器", "Leaving the script manager").text(), Colors.GREEN);
                    break label;
                case "help":
                case "?":
                    showManageHelp();
                    continue;
            }

            handleManageCommand(input);
        }

        outln(Text.zhEn("脚本管理器已退出", "Script manager exited").text(), Colors.GREEN);
    }

    protected void showManageHelp() {
        outln("", Colors.WHITE);
        outln(Text.zhEn("可用命令:", "Available commands:").text(), Colors.CYAN);
        out("  create <name>        ", Colors.YELLOW);
        outln(Text.zhEn("- 创建新脚本", "- Create a new script").text(), Colors.GRAY);
        out("  list                 ", Colors.YELLOW);
        outln(Text.zhEn("- 列出所有脚本和codebase文件", "- List all scripts and codebase files").text(), Colors.GRAY);
        out("  vars                 ", Colors.YELLOW);
        outln(Text.zhEn("- 列出脚本执行器变量", "- List interpreter variables").text(), Colors.GRAY);
        out("  show <name>          ", Colors.YELLOW);
        outln(Text.zhEn("- 显示文件内容", "- Show file contents").text(), Colors.GRAY);
        out("  edit <name>          ", Colors.YELLOW);
        outln(Text.zhEn("- 编辑脚本内容", "- Edit script contents").text(), Colors.GRAY);
        out("  delete <name>        ", Colors.YELLOW);
        outln(Text.zhEn("- 删除文件", "- Delete a file").text(), Colors.GRAY);
        out("  run <name>           ", Colors.YELLOW);
        outln(Text.zhEn("- 执行脚本或codebase文件", "- Run a script or codebase file").text(), Colors.GRAY);
        out("  import <path>        ", Colors.YELLOW);
        outln(Text.zhEn("- 导入文件", "- Import a file").text(), Colors.GRAY);
        out("  export <name> <path> ", Colors.YELLOW);
        outln(Text.zhEn("- 导出文件", "- Export a file").text(), Colors.GRAY);
        out("  codebase             ", Colors.YELLOW);
        outln(Text.zhEn("- 列出codebase文件", "- List codebase files").text(), Colors.GRAY);
        out("  help                 ", Colors.YELLOW);
        outln(Text.zhEn("- 显示此帮助", "- Show this help").text(), Colors.GRAY);
        out("  exit / quit          ", Colors.YELLOW);
        outln(Text.zhEn("- 退出管理器", "- Exit the manager").text(), Colors.GRAY);
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
                        outln(CliMessages.HELP_USAGE_INLINE.text() + "create <name>", Colors.GRAY);
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
                        outln(CliMessages.HELP_USAGE_INLINE.text() + "show <name>", Colors.GRAY);
                        break;
                    }
                    ScriptCrudCommand crud = new ScriptCrudCommand();
                    crud.context = this.context;
                    crud.handleShow(parts[1]);
                }
                case "edit" -> {
                    if (parts.length < 2) {
                        outln(CliMessages.HELP_USAGE_INLINE.text() + "edit <name>", Colors.GRAY);
                        break;
                    }
                    handleEdit(parts[1]);
                }
                case "4", "delete" -> {
                    if (parts.length < 2) {
                        outln(CliMessages.HELP_USAGE_INLINE.text() + "delete <name>", Colors.GRAY);
                        break;
                    }
                    ScriptCrudCommand crud = new ScriptCrudCommand();
                    crud.context = this.context;
                    crud.handleDelete(parts[1]);
                }
                case "5", "run" -> {
                    if (parts.length < 2) {
                        outln(CliMessages.HELP_USAGE_INLINE.text() + "run <name>", Colors.GRAY);
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
                        outln(CliMessages.HELP_USAGE_INLINE.text() + "import <path>", Colors.GRAY);
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
                        outln(CliMessages.HELP_USAGE_INLINE.text() + "export <name> <path>", Colors.GRAY);
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
                    out(Text.zhEn("未知命令: ", "Unknown command: ").text(), Colors.RED);
                    outln(cmd, Colors.YELLOW);
                    outln(Text.zhEn("输入 'help' 查看帮助", "Type 'help' for help").text(), Colors.GRAY);
                }
            }
        } catch (Exception e) {
            out(CliMessages.ERROR_PREFIX.text(), Colors.RED);
            outln(e.getMessage() != null ? e.getMessage() : ScriptTexts.NO_DETAILS.text(), Colors.ORANGE);
        }
    }

    protected void handleEdit(String name) throws IOException {
        File scriptFile = DataBridge.resolveScriptFile(name);

        if (!scriptFile.exists()) {
            out(CliMessages.ERROR_PREFIX.text() + ScriptTexts.PREFIX_SCRIPT_QUOTED.text(), Colors.RED);
            out(name, Colors.YELLOW);
            outln(ScriptTexts.SUFFIX_NOT_EXIST.text(), Colors.RED);
            return;
        }

        String existingContent = IOManager.readFile(scriptFile.getAbsolutePath());
        out(Text.zhEn("编辑脚本: ", "Editing script: ").text(), Colors.CYAN);
        outln(name, Colors.YELLOW);
        outln(Text.zhEn("当前内容 (输入空行结束编辑):", "Current contents (an empty line ends editing):").text(), Colors.GRAY);
        outln("", Colors.WHITE);
        outln(existingContent, Colors.WHITE);
        outln("", Colors.WHITE);
        outln(Text.zhEn("--- 开始编辑 (输入空行保存并退出) ---",
                "--- Start editing (an empty line saves and exits) ---").text(), Colors.CYAN);
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
            outln(Text.zhEn("脚本已保存", "Script saved").text(), Colors.GREEN);
        } else {
            outln(Text.zhEn("编辑已取消 (未做更改)", "Editing cancelled (no changes were made)").text(), Colors.GRAY);
        }
    }

    protected void handleCodebaseList() {
        File codebaseDir = getScriptsDirectory();

        if (!codebaseDir.exists()) {
            outln(Text.zhEn("Codebase目录不存在", "Codebase directory does not exist").text(), Colors.GRAY);
            return;
        }

        File[] files = codebaseDir.listFiles();
        if (files == null || files.length == 0) {
            outln(Text.zhEn("Codebase目录为空", "Codebase directory is empty").text(), Colors.GRAY);
            return;
        }

        outln(Text.zhEn("===== Codebase文件列表 =====", "===== Codebase file list =====").text(), Colors.CYAN);
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
        out(ScriptTexts.LABEL_TOTAL.text(), Colors.CYAN);
        out(String.valueOf(files.length), Colors.YELLOW);
        outln(Text.zhEn(" 个文件", " files").text(), Colors.CYAN);
    }
}
