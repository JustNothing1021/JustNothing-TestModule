package com.justnothing.testmodule.command.functions.alias.impl;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.AbstractCommand;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.command.SubCommandInfo;
import com.justnothing.testmodule.command.functions.alias.request.AliasRemoveRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.output.Colors;

import java.io.File;

@SubCommandInfo(
    description = "删除指定的命令别名",
    usage = "alias remove <别名>",
    examples = {
        "alias remove bi",
        "alias rm bi"
    },
    optionsDesc = """
            参数:
              <别名>   要删除的别名名称
            
            示例:
              alias remove bi     # 删除 bi 别名
              alias rm ls        # 删除 ls 别名
            """
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
                context.println("错误: 别名名称不能为空", Colors.RED);
                context.println("用法: alias remove <别名>", Colors.YELLOW);
            }
            return buildErrorResult("别名名称不能为空");
        }

        boolean removed = getAliasManager().removeAlias(name);

        AliasResult result = new AliasResult();
        result.setSuccess(removed);

        if (removed) {
            if (context.isCli()) {
                context.println("别名已删除: " + name, Colors.GREEN);
            }
            logger.info("删除别名: " + name);
        } else {
            if (context.isCli()) {
                context.println("错误: 别名 '" + name + "' 不存在", Colors.RED);
            }
            result.setError(new CommandResult.ErrorInfo("ALIAS_NOT_FOUND", "别名不存在", (Throwable) null));
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
