package com.justnothing.testmodule.command.functions.alias.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.functions.alias.request.AliasAddRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.alias.AliasTexts;

import java.io.File;

@SubCommandInfo(
    description = AliasTexts.SUB_ALIAS_ADD_DESC,
    usage = AliasTexts.SUB_ALIAS_ADD_USAGE,
    examples = {
        "alias add bi class info",
        "alias add ls class list -v"
    },
    optionsDesc = AliasTexts.SUB_ALIAS_ADD_OPTIONS
)
public class AliasAddCommand extends AbstractCommand<AliasAddRequest, AliasResult> {

    private static final Logger logger = Logger.getLoggerForName("AliasAddCmd");

    public AliasAddCommand() {
        super("alias add", AliasAddRequest.class, AliasResult.class);
    }

    @Override
    protected AliasResult executeInternal(CommandExecutor.CmdExecContext<AliasAddRequest> context) throws Exception {
        AliasAddRequest request = context.getRequest();
        String name = request.getName();
        String command = request.getCommand();

        if (name == null || name.isEmpty()) {
            if (context.isCli()) {
                context.println("错误: 别名名称不能为空", Colors.RED);
                context.println("用法: alias add <别名> <命令>", Colors.YELLOW);
            }
            return buildErrorResult("别名名称不能为空");
        }

        if (command == null || command.isEmpty()) {
            if (context.isCli()) {
                context.println("错误: 别名命令不能为空", Colors.RED);
                context.println("用法: alias add <别名> <命令>", Colors.YELLOW);
            }
            return buildErrorResult("别名命令不能为空");
        }

        boolean success = getAliasManager().addAlias(name, command);

        AliasResult result = new AliasResult();
        result.setSuccess(success);

        if (success) {
            if (context.isCli()) {
                context.println("别名已添加:", Colors.GREEN);
                context.print("  " + name, Colors.CYAN);
                context.println(" -> " + command, Colors.WHITE);
            }
            logger.info("添加别名: " + name + " -> " + command);
        } else {
            if (context.isCli()) {
                context.println("错误: 别名 '" + name + "' 已存在，请先删除或使用 alias clear 清空所有别名", Colors.RED);
            }
            result.setError(new CommandResult.ErrorInfo("ALIAS_EXISTS", "别名已存在", (Throwable) null));
        }

        return result;
    }

    private static AliasManager getAliasManager() {
        String dataDir = DataDirectoryManager.getMethodsCmdlineDataDirectory();
        return AliasManager.getInstance(new File(dataDir));
    }

    private AliasResult buildErrorResult(String message) {
        AliasResult result = new AliasResult();
        result.setSuccess(false);
        result.setMessage(message);
        return result;
    }

}
