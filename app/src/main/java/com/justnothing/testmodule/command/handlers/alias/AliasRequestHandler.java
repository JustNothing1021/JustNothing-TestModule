package com.justnothing.testmodule.command.handlers.alias;

import static com.justnothing.testmodule.constants.CommandServer.CMD_ALIAS_VER;

import com.justnothing.testmodule.command.functions.alias.util.AliasManager;
import com.justnothing.testmodule.command.proxy.RequestHandler;
import com.justnothing.testmodule.command.functions.alias.model.AliasInfo;
import com.justnothing.testmodule.command.functions.alias.request.AliasRequest;
import com.justnothing.testmodule.command.functions.alias.response.AliasResult;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AliasRequestHandler implements RequestHandler<AliasRequest, AliasResult> {

    private static final Logger logger = Logger.getLoggerForName("AliasRequestHandler");
    private static AliasManager aliasManager;

    @Override
    public String getCommandType() {
        return "Alias";
    }

    @Override
    public AliasRequest parseRequest(JSONObject obj) {
        return new AliasRequest().fromJson(obj);
    }

    @Override
    public AliasResult createResult(String requestId) {
        return new AliasResult(requestId);
    }

    @Override
    public String getHelpText() {
        return String.format(Locale.getDefault(),
                """
                语法: alias <子命令> [参数]
                
                管理命令别名，简化命令输入。
                
                子命令:
                    add <名称> <命令>    - 添加别名
                    remove <名称>        - 删除别名
                    list                 - 列出所有别名
                    clear                - 清除所有自定义别名
                    show <名称>          - 显示别名对应的命令
                
                示例:
                    alias add pm performance       - 添加别名: pm -> performance
                    alias add hi "help interactive" - 添加带参数的别名
                    alias add sr "script run"      - 添加子命令别名
                    alias remove pm                - 删除别名 pm
                    alias list                     - 列出所有别名
                    alias show pm                  - 显示 pm 对应的命令
                    alias clear                    - 清除所有自定义别名
                
                使用别名:
                    pm sample start        - 等同于 performance sample start
                    sc run myscript        - 等同于 script run myscript
                    hi                     - 等同于 help interactive
                
                默认别名:
                    h, ?  -> help
                    pm    -> performance
                    sc    -> script
                    tr    -> trace
                    wt    -> watch
                    bp    -> breakpoint
                    cls   -> clear
                
                注意:
                    - 别名名称不能包含空格
                    - 别名会自动保存，重启后保留
                    - 最多支持100个别名
                    - 别名可以包含子命令和参数
                
                (Submodule alias %s)
                """, CMD_ALIAS_VER);
    }

    @Override
    public AliasResult handle(AliasRequest request) {
        logger.debug("处理别名请求: action=" + request.getAction());

        AliasResult result = new AliasResult(request.getRequestId());

        try {
            ensureAliasManager();
            String action = request.getAction();

            switch (action) {
                case AliasRequest.ACTION_LIST -> handleList(result);
                case AliasRequest.ACTION_ADD -> handleAdd(request, result);
                case AliasRequest.ACTION_REMOVE -> handleRemove(request, result);
                case AliasRequest.ACTION_CLEAR -> handleClear(result);
                default -> result.setError(new CommandResult.ErrorInfo("INVALID_ACTION", "不支持的别名操作: " + action));
            }

        } catch (Exception e) {
            logger.error("处理别名请求失败", e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "处理请求失败: " + e.getMessage()));
        }

        return result;
    }



    private void ensureAliasManager() {
        if (aliasManager == null) {
            String dataDir = DataDirectoryManager.getMethodsCmdlineDataDirectory();
            if (dataDir != null) {
                aliasManager = AliasManager.getInstance(new File(dataDir));
            } else {
                throw new RuntimeException("无法获取数据目录");
            }
        }
    }

    private void handleList(AliasResult result) {
        Map<String, String> allAliases = aliasManager.getAllAliases();
        List<AliasInfo> aliases = new ArrayList<>();

        for (Map.Entry<String, String> entry : allAliases.entrySet()) {
            aliases.add(new AliasInfo(entry.getKey(), entry.getValue()));
        }

        result.setAliases(aliases);
        result.setSuccess(true);
        logger.info("返回 " + aliases.size() + " 个别名");
    }

    private void handleAdd(AliasRequest request, AliasResult result) {
        String name = request.getName();
        String command = request.getCommand();

        if (name == null || name.trim().isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_PARAM", "别名名称不能为空"));
            return;
        }

        if (command == null || command.trim().isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_PARAM", "命令不能为空"));
            return;
        }

        if (aliasManager.addAlias(name, command)) {
            result.setSuccess(true);
            logger.info("添加别名: " + name + " -> " + command);
        } else {
            result.setError(new CommandResult.ErrorInfo("ADD_FAILED", "添加别名失败，可能是名称无效或已达上限"));
        }
    }

    private void handleRemove(AliasRequest request, AliasResult result) {
        String name = request.getName();

        if (name == null || name.trim().isEmpty()) {
            result.setError(new CommandResult.ErrorInfo("INVALID_PARAM", "别名名称不能为空"));
            return;
        }

        if (aliasManager.removeAlias(name)) {
            result.setSuccess(true);
            logger.info("删除别名: " + name);
        } else {
            result.setError(new CommandResult.ErrorInfo("REMOVE_FAILED", "删除失败，别名不存在"));
        }
    }

    private void handleClear(AliasResult result) {
        aliasManager.clearAliases();
        result.setSuccess(true);
        logger.info("清除所有别名");
    }
}