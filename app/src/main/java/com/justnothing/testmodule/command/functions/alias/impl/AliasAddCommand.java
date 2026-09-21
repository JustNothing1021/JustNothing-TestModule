package com.justnothing.testmodule.command.functions.alias.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.CliTexts;
import com.justnothing.testmodule.command.framework.i18n.Text;
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
                context.println(CliMessages.ERROR_PREFIX.text() + AliasTexts.ERR_NAME_REQUIRED.text(), Colors.RED);
                context.println(CliMessages.HELP_USAGE_INLINE.text()
                        + CliTexts.resolve(AliasTexts.SUB_ALIAS_ADD_USAGE), Colors.YELLOW);
            }
            return buildErrorResult(AliasTexts.ERR_NAME_REQUIRED.text());
        }

        if (command == null || command.isEmpty()) {
            if (context.isCli()) {
                context.println(CliMessages.ERROR_PREFIX.text() + AliasTexts.ERR_COMMAND_REQUIRED.text(), Colors.RED);
                context.println(CliMessages.HELP_USAGE_INLINE.text()
                        + CliTexts.resolve(AliasTexts.SUB_ALIAS_ADD_USAGE), Colors.YELLOW);
            }
            return buildErrorResult(AliasTexts.ERR_COMMAND_REQUIRED.text());
        }

        boolean success = getAliasManager().addAlias(name, command);

        AliasResult result = new AliasResult();
        result.setSuccess(success);

        if (success) {
            if (context.isCli()) {
                context.println(Text.zhEn("别名已添加:", "Alias added:").text(), Colors.GREEN);
                context.print("  " + name, Colors.CYAN);
                context.println(" -> " + command, Colors.WHITE);
            }
            logger.info("添加别名: " + name + " -> " + command);
        } else {
            if (context.isCli()) {
                context.println(CliMessages.ERROR_PREFIX.text() + Text.zhEn(
                        "别名 '%s' 已存在，请先删除或使用 alias clear 清空所有别名",
                        "alias '%s' already exists; remove it or run 'alias clear' to wipe them all")
                        .format(name), Colors.RED);
            }
            result.setError(new CommandResult.ErrorInfo("ALIAS_EXISTS", AliasTexts.ERR_ALIAS_EXISTS.text(), (Throwable) null));
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
