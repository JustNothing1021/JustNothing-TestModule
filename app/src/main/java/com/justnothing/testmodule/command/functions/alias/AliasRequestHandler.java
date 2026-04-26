package com.justnothing.testmodule.command.functions.alias;

import com.justnothing.testmodule.protocol.json.handler.RequestHandler;
import com.justnothing.testmodule.protocol.json.model.AliasInfo;
import com.justnothing.testmodule.protocol.json.request.AliasRequest;
import com.justnothing.testmodule.protocol.json.response.AliasResult;
import com.justnothing.testmodule.protocol.json.response.CommandResult;
import com.justnothing.testmodule.utils.data.DataDirectoryManager;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
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