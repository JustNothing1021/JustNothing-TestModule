package com.justnothing.testmodule.command.handlers.hook;

import com.justnothing.testmodule.command.functions.hook.HookManager;
import com.justnothing.testmodule.command.proxy.RequestHandler;
import com.justnothing.testmodule.command.functions.hook.HookListRequest;
import com.justnothing.testmodule.command.base.CommandResult;
import com.justnothing.testmodule.command.functions.hook.HookListResult;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class HookListRequestHandler implements RequestHandler<HookListRequest, HookListResult> {

    private static final Logger logger = Logger.getLoggerForName("HookListReqHandler");

    @Override
    public String getCommandType() {
        return "HookList";
    }

    @Override
    public HookListRequest parseRequest(JSONObject obj) {
        return new HookListRequest().fromJson(obj);
    }

    @Override
    public HookListResult createResult(String requestId) {
        return new HookListResult(requestId);
    }

    @Override
    public HookListResult handle(HookListRequest request) {
        logger.debug("处理Hook列表请求");

        HookListResult result = new HookListResult(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        try {
            List<Map<String, Object>> hookMaps = HookManager.getAllHooksAsMap();
            int activeCount = 0;

            for (Map<String, Object> map : hookMaps) {
                HookListResult.HookItem item = new HookListResult.HookItem();
                item.setId((String) map.get("id"));
                item.setClassName((String) map.get("className"));
                item.setMethodName((String) map.get("methodName"));
                item.setSignature((String) map.get("signature"));
                item.setHasBefore(Boolean.TRUE.equals(map.get("hasBefore")));
                item.setHasAfter(Boolean.TRUE.equals(map.get("hasAfter")));
                item.setHasReplace(Boolean.TRUE.equals(map.get("hasReplace")));
                item.setCallCount(((Number) map.getOrDefault("callCount", 0)).intValue());
                item.setActive(Boolean.TRUE.equals(map.get("active")));
                item.setEnabled(Boolean.TRUE.equals(map.get("enabled")));
                item.setCreateTime(((Number) map.getOrDefault("createTime", 0L)).longValue());

                String beforeCode = (String) map.get("beforeCode");
                if (beforeCode != null && !beforeCode.isEmpty()) {
                    item.setBeforeCodePreview(truncateCode(beforeCode));
                }
                String afterCode = (String) map.get("afterCode");
                if (afterCode != null && !afterCode.isEmpty()) {
                    item.setAfterCodePreview(truncateCode(afterCode));
                }
                String replaceCode = (String) map.get("replaceCode");
                if (replaceCode != null && !replaceCode.isEmpty()) {
                    item.setReplaceCodePreview(truncateCode(replaceCode));
                }

                result.addHook(item);

                if (item.isActive() && item.isEnabled()) {
                    activeCount++;
                }
            }

            result.setTotalHookCount(hookMaps.size());
            result.setActiveCount(activeCount);

            logger.info("Hook列表查询成功, 总数: " + hookMaps.size() + ", 活跃: " + activeCount);

        } catch (Exception e) {
            logger.error("获取Hook列表失败", e);
            result.setError(new CommandResult.ErrorInfo("INTERNAL_ERROR", "获取Hook列表失败: " + e.getMessage()));
        }

        return result;
    }

    private String truncateCode(String code) {
        if (code == null) return "";
        if (code.length() <= 50) return code;
        return code.substring(0, 47) + "...";
    }
}
