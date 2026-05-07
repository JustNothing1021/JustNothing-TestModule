package com.justnothing.testmodule.command.functions.alias.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.alias.request.AliasClearRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.output.Colors;

import java.io.File;

@SubCommandInfo(
    description = "清空所有已定义的命令别名",
    usage = "alias clear",
    examples = {"alias clear"},
    optionsDesc = """
            清除所有已保存的别名，恢复到初始状态。
            
            ⚠️ 警告: 此操作不可撤销！
            
            示例:
              alias clear                   # 清空所有别名
            """
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
            context.println("已清空所有别名 (共删除 " + count + " 个)", Colors.GREEN);
        }

        logger.info("清空所有别名，共删除 " + count + " 个");
        return result;
    }

    private static AliasManager getAliasManager() {
        String dataDir = DataDirectoryManager.getMethodsCmdlineDataDirectory();
        return AliasManager.getInstance(new File(dataDir));
    }

}
