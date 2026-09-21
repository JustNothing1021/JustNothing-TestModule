package com.justnothing.testmodule.command.functions.hook.impl;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.hook.HookTexts;
import com.justnothing.testmodule.command.functions.hook.response.HookListResult;
import com.justnothing.testmodule.command.functions.hook.util.HookManager;
import com.justnothing.testmodule.command.functions.hook.request.*;
import com.justnothing.testmodule.command.framework.output.Colors;

public class HookManageCommand extends AbstractHookCommand<CommandRequest<?>, HookListResult> {

    @SuppressWarnings("unchecked")
    public HookManageCommand() {
        super("hook manage", (Class) CommandRequest.class, HookListResult.class);
    }

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
            outln(Text.zhEn("添加Hook成功", "Hook added").text(), Colors.GREEN);
            out("Hook ID: ", Colors.CYAN);
            outln(result.hookId(), Colors.YELLOW);
            
            HookListResult r = okListResult("add");
            r.setMessage(Text.zhEn("Hook已添加: %s", "Hook added: %s").format(result.hookId()));
            return r;
        } else {
            throw new RuntimeException(result.errorMessage());
        }
    }

    public HookListResult handleRemove(HookRemoveRequest request) throws Exception {
        logger.info("移除Hook: %s", request.getHookId());

        // 找不到就是失败，不能再无条件地报「移除成功」
        if (!HookManager.removeHook(request.getHookId(), context)) {
            return createErrorResult(HookTexts.ERR_HOOK_ID_NOT_FOUND.format(request.getHookId()));
        }

        HookListResult r = okListResult("remove");
        r.setMessage(Text.zhEn("Hook已移除: %s", "Hook removed: %s").format(request.getHookId()));
        return r;
    }

    public HookListResult handleEnable(HookEnableRequest request) throws Exception {
        logger.info("启用Hook: %s", request.getHookId());

        if (!HookManager.enableHook(request.getHookId(), context)) {
            return createErrorResult(HookTexts.ERR_HOOK_ID_NOT_FOUND.format(request.getHookId()));
        }

        HookListResult r = okListResult("enable");
        r.setMessage(Text.zhEn("Hook已启用: %s", "Hook enabled: %s").format(request.getHookId()));
        return r;
    }

    public HookListResult handleDisable(HookDisableRequest request) throws Exception {
        logger.info("禁用Hook: %s", request.getHookId());

        if (!HookManager.disableHook(request.getHookId(), context)) {
            return createErrorResult(HookTexts.ERR_HOOK_ID_NOT_FOUND.format(request.getHookId()));
        }

        HookListResult r = okListResult("disable");
        r.setMessage(Text.zhEn("Hook已禁用: %s", "Hook disabled: %s").format(request.getHookId()));
        return r;
    }

    public HookListResult handleClear(HookClearRequest request) throws Exception {
        int count = HookManager.getHookCount();
        logger.warn("清除所有Hook (%d个)", count);
        
        HookManager.clearAllHooks();
        
        out(Text.zhEn("已清除 ", "Cleared ").text(), Colors.LIGHT_GREEN);
        out(count + " ", Colors.YELLOW);
        outln(Text.zhEn("个Hook", "hooks").text(), Colors.LIGHT_GREEN);
        
        HookListResult r = okListResult("clear");
        r.setTotalHookCount(count);
        r.setMessage(Text.zhEn("已清除 %d 个Hook", "Cleared %d hooks").format(count));
        return r;
    }

    @Override
    protected HookListResult executeRequest(CommandRequest<?> request) throws Exception {
        if (request instanceof HookAddRequest r) return handleAdd(r);
        if (request instanceof HookRemoveRequest r) return handleRemove(r);
        if (request instanceof HookEnableRequest r) return handleEnable(r);
        if (request instanceof HookDisableRequest r) return handleDisable(r);
        if (request instanceof HookClearRequest r) return handleClear(r);
        
        throw new IllegalArgumentException(
                HookTexts.ERR_UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }
}
