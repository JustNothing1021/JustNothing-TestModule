package com.justnothing.testmodule.command.functions.alias.impl;

import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.model.AbstractCommand;
import com.justnothing.testmodule.command.framework.annotation.SubCommandInfo;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.alias.request.AliasClearRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.alias.AliasTexts;

import java.io.File;

@SubCommandInfo(
    description = AliasTexts.SUB_ALIAS_CLEAR_DESC,
    usage = "alias clear",
    examples = {"alias clear"},
    optionsDesc = AliasTexts.SUB_ALIAS_CLEAR_OPTIONS
)
public class AliasClearCommand extends AbstractCommand<AliasClearRequest, AliasResult> {

    private static final Logger logger = Logger.getLoggerForName("AliasClearCmd");

    public AliasClearCommand() {
        super("alias clear", AliasClearRequest.class, AliasResult.class);
    }

    @Override
    protected AliasResult executeInternal(CommandExecutor.CmdExecContext<AliasClearRequest> context) throws Exception {
        int count = getAliasManager().getAllAliases().size();
        getAliasManager().clearAliases();

        AliasResult result = new AliasResult();
        result.setSuccess(true);

        if (context.isCli()) {
            context.println(Text.zhEn("已清空所有别名 (共删除 %s 个)", "All aliases cleared (%s removed)")
                    .format(count), Colors.GREEN);
        }

        logger.info("清空所有别名，共删除 " + count + " 个");
        return result;
    }

    private static AliasManager getAliasManager() {
        String dataDir = DataDirectoryManager.getMethodsCmdlineDataDirectory();
        return AliasManager.getInstance(new File(dataDir));
    }

}
