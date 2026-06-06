package com.justnothing.testmodule.command.functions.hook.impl;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.hook.HookListResult;
import com.justnothing.testmodule.command.functions.hook.HookManager;
import com.justnothing.testmodule.command.functions.hook.request.*;
import com.justnothing.testmodule.command.output.Colors;

public class HookManageCommand extends AbstractHookCommand<CommandRequest, HookListResult> {

    public HookListResult handleAdd(HookAddRequest request) throws Exception {
        logger.info("添加Hook: %s.%s%s",
                   request.getClassName(), request.getMethodName(),
                   request.getSignature() != null ? "(" + request.getSignature() + ")" : "");

        HookManager.AddHookResult result = HookManager.addHook(
                request.getClassName(), request.getMethodName(), request.getSignature(),
                request.getBeforeCode(), request.getAfterCode(), request.getReplaceCode(),
                request.getBeforeCodebase(), request.getAfterCodebase(), request.getReplaceCodebase(),
                context);

        if (result.success()) {
            outln("添加Hook成功", Colors.GREEN);
            out("Hook ID: ", Colors.CYAN);
            outln(result.hookId(), Colors.YELLOW);
            
            HookListResult r = okListResult("add");
            r.setMessage("Hook已添加: " + result.hookId());
            return r;
        } else {
            throw new RuntimeException(result.errorMessage());
        }
    }

    public HookListResult handleRemove(HookRemoveRequest request) throws Exception {
        logger.info("移除Hook: %s", request.getHookId());

        try {
            HookManager.removeHook(request.getHookId(), context);
            outln("移除Hook成功", Colors.GREEN);
            
            HookListResult r = okListResult("remove");
            r.setMessage("Hook已移除: " + request.getHookId());
            return r;
        } catch (Exception e) {
            throw new IllegalArgumentException("未找到Hook (ID: " + request.getHookId() + ")");
        }
    }

    public HookListResult handleEnable(HookEnableRequest request) throws Exception {
        logger.info("启用Hook: %s", request.getHookId());

        HookManager.enableHook(request.getHookId(), context);
        outln("启用Hook成功", Colors.GREEN);
        
        HookListResult r = okListResult("enable");
        r.setMessage("Hook已启用: " + request.getHookId());
        return r;
    }

    public HookListResult handleDisable(HookDisableRequest request) throws Exception {
        logger.info("禁用Hook: %s", request.getHookId());

        HookManager.disableHook(request.getHookId(), context);
        outln("禁用Hook成功", Colors.GREEN);
        
        HookListResult r = okListResult("disable");
        r.setMessage("Hook已禁用: " + request.getHookId());
        return r;
    }

    public HookListResult handleClear(HookClearRequest request) throws Exception {
        int count = HookManager.getHookCount();
        logger.warn("清除所有Hook (%d个)", count);
        
        HookManager.clearAllHooks();
        
        out("已清除 ", Colors.LIGHT_GREEN);
        out(count + " ", Colors.YELLOW);
        outln("个Hook", Colors.LIGHT_GREEN);
        
        HookListResult r = okListResult("clear");
        r.setTotalHookCount(count);
        r.setMessage("已清除 " + count + " 个Hook");
        return r;
    }

    @Override
    protected HookListResult executeInternal(CommandRequest request) throws Exception {
        if (request instanceof HookAddRequest r) return handleAdd(r);
        if (request instanceof HookRemoveRequest r) return handleRemove(r);
        if (request instanceof HookEnableRequest r) return handleEnable(r);
        if (request instanceof HookDisableRequest r) return handleDisable(r);
        if (request instanceof HookClearRequest r) return handleClear(r);
        
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }
}
