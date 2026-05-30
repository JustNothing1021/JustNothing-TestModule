package com.justnothing.testmodule.command.handlers.hook;

import com.justnothing.testmodule.command.functions.hook.request.HookListRequest;
import com.justnothing.testmodule.command.functions.hook.HookListResult;
import com.justnothing.testmodule.command.functions.hook.HookManager;

import java.util.List;
import java.util.Map;

public class HookListRequestHandler {

    public HookListResult handle(HookListRequest request) {
        HookListResult result = new HookListResult();
        result.setRequestId(request.getRequestId());
        result.setTimestamp(System.currentTimeMillis());

        List<Map<String, Object>> hooksMap = HookManager.getAllHooksAsMap();
        int activeCount = 0;

        for (Map<String, Object> hookMap : hooksMap) {
            HookListResult.HookItem item = new HookListResult.HookItem();
            item.setId((String) hookMap.get("id"));
            item.setClassName((String) hookMap.get("className"));
            item.setMethodName((String) hookMap.get("methodName"));
            item.setSignature((String) hookMap.get("signature"));

            String beforeCode = (String) hookMap.get("beforeCode");
            String beforeCodebase = (String) hookMap.get("beforeCodebase");
            String afterCode = (String) hookMap.get("afterCode");
            String afterCodebase = (String) hookMap.get("afterCodebase");
            String replaceCode = (String) hookMap.get("replaceCode");
            String replaceCodebase = (String) hookMap.get("replaceCodebase");

            item.setHasBefore(isNotEmpty(beforeCode) || isNotEmpty(beforeCodebase));
            item.setHasAfter(isNotEmpty(afterCode) || isNotEmpty(afterCodebase));
            item.setHasReplace(isNotEmpty(replaceCode) || isNotEmpty(replaceCodebase));

            Number callCount = (Number) hookMap.get("callCount");
            item.setCallCount(callCount != null ? callCount.intValue() : 0);

            Boolean active = (Boolean) hookMap.get("active");
            Boolean enabled = (Boolean) hookMap.get("enabled");
            item.setActive(active != null && active);
            item.setEnabled(enabled == null || enabled);

            Number createTime = (Number) hookMap.get("createTime");
            item.setCreateTime(createTime != null ? createTime.longValue() : 0L);

            item.setBeforeCodePreview(truncate(beforeCode));
            item.setAfterCodePreview(truncate(afterCode));
            item.setReplaceCodePreview(truncate(replaceCode));

            result.addHook(item);

            if (active != null && active && (enabled == null || enabled)) {
                activeCount++;
            }
        }

        result.setTotalHookCount(hooksMap.size());
        result.setActiveCount(activeCount);
        result.setSuccess(true);

        return result;
    }

    private boolean isNotEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private String truncate(String code) {
        if (code == null) return null;
        if (code.length() <= 50) return code;
        return code.substring(0, 47) + "...";
    }
}
