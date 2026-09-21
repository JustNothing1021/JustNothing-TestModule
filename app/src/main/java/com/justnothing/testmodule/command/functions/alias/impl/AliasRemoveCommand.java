package com.justnothing.testmodule.command.functions.alias.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.CliTexts;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.alias.request.AliasRemoveRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.alias.AliasTexts;

import java.io.File;

@SubCommandInfo(
    description = AliasTexts.SUB_ALIAS_REMOVE_DESC,
    usage = AliasTexts.SUB_ALIAS_REMOVE_USAGE,
    examples = {
        "alias remove bi",
        "alias rm bi"
    },
    optionsDesc = AliasTexts.SUB_ALIAS_REMOVE_OPTIONS
)
public class AliasRemoveCommand extends AbstractCommand<AliasRemoveRequest, AliasResult> {

    private static final Logger logger = Logger.getLoggerForName("AliasRemoveCmd");

    public AliasRemoveCommand() {
        super("alias remove", AliasRemoveRequest.class, AliasResult.class);
    }

    @Override
    protected AliasResult executeInternal(CommandExecutor.CmdExecContext<AliasRemoveRequest> context) throws Exception {
        AliasRemoveRequest request = context.getRequest();
        String name = request.getName();

        if (name == null || name.isEmpty()) {
            if (context.isCli()) {
                context.println(CliMessages.ERROR_PREFIX.text() + AliasTexts.ERR_NAME_REQUIRED.text(), Colors.RED);
                context.println(CliMessages.HELP_USAGE_INLINE.text()
                        + CliTexts.resolve(AliasTexts.SUB_ALIAS_REMOVE_USAGE), Colors.YELLOW);
            }
            return buildErrorResult(AliasTexts.ERR_NAME_REQUIRED.text());
        }

        boolean removed = getAliasManager().removeAlias(name);

        AliasResult result = new AliasResult();
        result.setSuccess(removed);

        if (removed) {
            if (context.isCli()) {
                context.println(Text.zhEn("别名已删除: %s", "Alias removed: %s").format(name), Colors.GREEN);
            }
            logger.info("删除别名: " + name);
        } else {
            if (context.isCli()) {
                context.println(CliMessages.ERROR_PREFIX.text()
                        + Text.zhEn("别名 '%s' 不存在", "alias '%s' not found").format(name), Colors.RED);
            }
            result.setError(new CommandResult.ErrorInfo("ALIAS_NOT_FOUND", AliasTexts.ERR_ALIAS_NOT_FOUND.text(), (Throwable) null));
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
