package com.justnothing.testmodule.command.functions.alias.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.alias.request.AliasListRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.output.Colors;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SubCommandInfo(
    description = "列出所有已定义的命令别名",
    usage = "alias list",
    examples = {"alias list"},
    optionsDesc = """
            显示所有已保存的别名，包括名称和对应的完整命令。
            
            示例:
              alias list                    # 列出所有别名
              alias                        # 默认就是 list
            """
)
public class AliasListCommand extends AbstractCommand<AliasListRequest, AliasResult> {

    private static final Logger logger = Logger.getLoggerForName("AliasListCmd");

    public AliasListCommand() {
        super("alias list", AliasListRequest.class, AliasResult.class);
    }

    @Override
    protected AliasResult executeInternal(CommandExecutor.CmdExecContext<AliasListRequest> context) throws Exception {
        AliasManager aliasManager = getAliasManager();
        Map<String, String> aliases = aliasManager.getAllAliases();

        List<AliasInfo> aliasInfos = new ArrayList<>();
        for (Map.Entry<String, String> entry : aliases.entrySet()) {
            aliasInfos.add(new AliasInfo(entry.getKey(), entry.getValue()));
        }

        AliasResult result = new AliasResult();
        result.setAliases(aliasInfos);

        if (context.isCli()) {
            context.println("当前已定义的别名:", Colors.CYAN);
            for (AliasInfo info : aliasInfos) {
                context.print("  " + info.getName(), Colors.GREEN);
                context.println(" -> " + info.getCommand(), Colors.WHITE);
            }
            if (aliasInfos.isEmpty()) {
                context.println("  (无别名)", Colors.GRAY);
            }
        }

        logger.info("列出别名完成，共 " + aliasInfos.size() + " 个");
        return result;
    }

    private static AliasManager getAliasManager() {
        String dataDir = DataDirectoryManager.getMethodsCmdlineDataDirectory();
        return AliasManager.getInstance(new File(dataDir));
    }

}
