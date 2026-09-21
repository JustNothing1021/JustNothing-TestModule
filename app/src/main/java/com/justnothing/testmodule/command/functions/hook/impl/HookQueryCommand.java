package com.justnothing.testmodule.command.functions.hook.impl;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.hook.request.*;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.hook.HookTexts;
import com.justnothing.testmodule.command.functions.hook.response.HookAddResult;
import com.justnothing.testmodule.command.functions.hook.response.HookListResult;
import com.justnothing.testmodule.command.functions.hook.util.HookManager;

import java.util.List;
import java.util.Map;

public class HookQueryCommand extends AbstractHookCommand<CommandRequest<?>, CommandResult> {

    @SuppressWarnings("unchecked")
    public HookQueryCommand() {
        super("hook query", (Class) CommandRequest.class, CommandResult.class);
    }

    public HookListResult handleList(HookListRequest request) {
        logger.debug("列出所有Hook");
        
        HookManager.listHooks(context);
        
        List<Map<String, Object>> hooksMap = HookManager.getAllHooksAsMap();
        
        HookListResult r = new HookListResult();
        r.setSubCommand("list");
        r.setSuccess(true);
        r.setTotalHookCount(hooksMap.size());
        r.setActiveCount((int) hooksMap.stream().filter(h -> Boolean.TRUE.equals(h.get("enabled"))).count());
        r.setTimestamp(System.currentTimeMillis());
        return r;
    }

    public HookAddResult handleInfo(HookInfoRequest request) throws Exception {
        logger.info("查看Hook信息: %s", request.getHookId());

        List<HookAddResult.HookDetailInfo> detail = HookManager.getHookInfoDetail(request.getHookId());
        if (detail == null) {
            throw new IllegalArgumentException(
                    HookTexts.ERR_HOOK_ID_NOT_FOUND.format(request.getHookId()));
        }
        
        for (HookAddResult.HookDetailInfo info : detail) {
            out(info.getKey() + ": ", Colors.CYAN);
            outln(info.getValue(), Colors.WHITE);
        }
        
        HookAddResult r = new HookAddResult();
        r.setSubCommand("info");
        r.setSuccessAction(true);
        r.setHookId(request.getHookId());
        r.setDetail(detail);
        r.setMessage(Text.zhEn("Hook信息查询成功", "Hook info retrieved").text());
        return r;
    }

    public HookAddResult handleOutput(HookOutputRequest request) throws Exception {
        logger.info("获取Hook输出: %s count=%d", request.getHookId(), request.getOutputCount());

        List<HookAddResult.HookDetailInfo> output = HookManager.getHookOutputDetail(request.getHookId(), request.getOutputCount());
        if (output == null) {
            throw new IllegalArgumentException(
                    HookTexts.ERR_HOOK_ID_NOT_FOUND.format(request.getHookId()));
        }
        
        for (HookAddResult.HookDetailInfo line : output) {
            out(line.getValue(), Colors.WHITE);
            outln("", Colors.WHITE);
        }
        
        HookAddResult r = new HookAddResult();
        r.setSubCommand("output");
        r.setSuccessAction(true);
        r.setHookId(request.getHookId());
        r.setDetail(output);
        r.setMessage(Text.zhEn("获取到了 %d 条输出记录", "Fetched %d output records").format(output.size()));
        return r;
    }

    @Override
    protected CommandResult executeRequest(CommandRequest<?> request) throws Exception {
        if (request instanceof HookListRequest r) return handleList(r);
        if (request instanceof HookInfoRequest r) return handleInfo(r);
        if (request instanceof HookOutputRequest r) return handleOutput(r);
        
        throw new IllegalArgumentException(
                HookTexts.ERR_UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }
}
