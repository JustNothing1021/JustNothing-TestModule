package com.justnothing.testmodule.command.functions.script.impl;

import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.script.ScriptTexts;
import com.justnothing.testmodule.command.functions.script.response.ScriptResult;
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
    description = ScriptTexts.SUB_SCRIPT_CRUD_DESC,
    examples = {
        "script create <name>              创建新脚本",
        "script show <name>                显示脚本内容",
        "script delete <name>              删除脚本"
    }
)
public class ScriptCrudCommand extends AbstractScriptCommand<ScriptBaseRequest<?>, ScriptResult> {

    @SuppressWarnings("unchecked")
    public ScriptCrudCommand() {
        super("script crud", (Class) ScriptBaseRequest.class, ScriptResult.class);
    }

    @Override
    protected ScriptResult executeRequest(ScriptBaseRequest<?> request) throws Exception {
        if (request instanceof ScriptCreateRequest r) {
            return handleCreate(r.getName());
        }
        if (request instanceof ScriptShowRequest r) {
            return handleShow(r.getName());
        }
        if (request instanceof ScriptDeleteRequest r) {
            return handleDelete(r.getName());
        }
        throw new IllegalArgumentException(
                ScriptTexts.ERR_UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }

    protected ScriptResult handleCreate(String scriptName) throws IOException {
        ScriptResult r = new ScriptResult(UUID.randomUUID().toString());
        r.setSubCommand("create");

        if (scriptName == null || scriptName.isEmpty()) {
            context.println(CliMessages.ERROR_PREFIX.text() + ScriptTexts.ERR_NEED_SCRIPT_NAME.text(), Colors.RED);
            context.println(CliMessages.HELP_USAGE_INLINE.text() + "script create <name>", Colors.GRAY);
            r.setSuccess(false);
            r.setOutput(ScriptTexts.ERR_NEED_SCRIPT_NAME.text());
            return r;
        }

        if (!isValidScriptName(scriptName)) {
            context.println(CliMessages.ERROR_PREFIX.text()
                    + Text.zhEn("脚本名称只能包含字母、数字和下划线",
                            "Script name may contain only letters, digits and underscores").text(), Colors.RED);
            r.setSuccess(false);
            r.setOutput(Text.zhEn("脚本名称无效: %s", "Invalid script name: %s").format(scriptName));
            return r;
        }

        File scriptFile = DataBridge.getScriptFile(scriptName);
        if (scriptFile.exists()) {
            context.print(CliMessages.ERROR_PREFIX.text() + ScriptTexts.PREFIX_SCRIPT_QUOTED.text(), Colors.RED);
            context.print(scriptName, Colors.YELLOW);
            context.println(ScriptTexts.SUFFIX_ALREADY_EXISTS.text(), Colors.RED);
            r.setSuccess(false);
            r.setOutput(Text.zhEn("脚本已存在: %s", "Script already exists: %s").format(scriptName));
            return r;
        }

        IOManager.createDirectory(Objects.requireNonNull(scriptFile.getParentFile()).getAbsolutePath());

        String content = "// Script: " + scriptName + "\n" +
                "// Created by: " +
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        .format(new Date())
                +
                "\n" +
                Text.zhEn("// 在这里编写你的脚本代码...\n", "// Write your script code here...\n").text();

        IOManager.writeFile(scriptFile.getAbsolutePath(), content);

        context.print(ScriptTexts.PREFIX_SCRIPT_QUOTED.text(), Colors.GREEN);
        context.print(scriptName, Colors.YELLOW);
        context.println(Text.zhEn("' 创建成功", "' created").text(), Colors.GREEN);
        context.print(ScriptTexts.LABEL_PATH.text(), Colors.CYAN);
        context.println(scriptFile.getAbsolutePath(), Colors.GREEN);

        r.setSuccess(true);
        r.setScriptName(scriptName);
        return r;
    }

    protected ScriptResult handleShow(String fileName) throws IOException {
        ScriptResult r = new ScriptResult(UUID.randomUUID().toString());
        r.setSubCommand("show");

        if (fileName == null || fileName.isEmpty()) {
            context.println(CliMessages.ERROR_PREFIX.text() + ScriptTexts.ERR_NEED_FILE_NAME.text(), Colors.RED);
            context.println(CliMessages.HELP_USAGE_INLINE.text() + "script show <name>", Colors.GRAY);
            r.setSuccess(false);
            r.setOutput(ScriptTexts.ERR_NEED_FILE_NAME.text());
            return r;
        }

        File targetFile = DataBridge.resolveScriptFile(fileName);

        if (!targetFile.exists()) {
            context.print(CliMessages.ERROR_PREFIX.text() + ScriptTexts.PREFIX_FILE_QUOTED.text(), Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println(ScriptTexts.SUFFIX_NOT_EXIST.text(), Colors.RED);
            r.setSuccess(false);
            r.setOutput(ScriptTexts.ERR_FILE_NOT_FOUND.format(fileName));
            return r;
        }

        String content = IOManager.readFile(targetFile.getAbsolutePath());
        context.print(Text.zhEn("===== 文件内容: ", "===== File contents: ").text(), Colors.CYAN);
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
            context.println(CliMessages.ERROR_PREFIX.text() + ScriptTexts.ERR_NEED_FILE_NAME.text(), Colors.RED);
            context.println(CliMessages.HELP_USAGE_INLINE.text() + "script delete <name>", Colors.GRAY);
            r.setSuccess(false);
            r.setOutput(ScriptTexts.ERR_NEED_FILE_NAME.text());
            return r;
        }

        File targetFile = DataBridge.resolveScriptFile(fileName);

        if (!targetFile.exists()) {
            context.print(CliMessages.ERROR_PREFIX.text() + ScriptTexts.PREFIX_FILE_QUOTED.text(), Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println(ScriptTexts.SUFFIX_NOT_EXIST.text(), Colors.RED);
            r.setSuccess(false);
            r.setOutput(ScriptTexts.ERR_FILE_NOT_FOUND.format(fileName));
            return r;
        }

        if (IOManager.deleteFile(targetFile.getAbsolutePath())) {
            context.print(ScriptTexts.PREFIX_FILE_QUOTED.text(), Colors.GREEN);
            context.print(fileName, Colors.YELLOW);
            context.println(Text.zhEn("' 已删除", "' deleted").text(), Colors.GREEN);
            r.setSuccess(true);
        } else {
            context.print(CliMessages.ERROR_PREFIX.text()
                    + Text.zhEn("无法删除文件 '", "Cannot delete file '").text(), Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("'", Colors.RED);
            r.setSuccess(false);
            r.setOutput(Text.zhEn("无法删除文件: %s", "Cannot delete file: %s").format(fileName));
        }

        r.setDeletedName(fileName);
        return r;
    }
}
