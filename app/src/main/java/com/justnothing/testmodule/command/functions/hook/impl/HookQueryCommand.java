package com.justnothing.testmodule.command.functions.hook.impl;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.hook.*;
import com.justnothing.testmodule.command.functions.hook.request.*;
import com.justnothing.testmodule.command.output.Colors;

import java.util.List;
import java.util.Map;

public class HookQueryCommand extends AbstractHookCommand<CommandRequest, CommandResult> {

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
            throw new IllegalArgumentException("未找到Hook (ID: " + request.getHookId() + ")");
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
        r.setMessage("Hook信息查询成功");
        return r;
    }

    public HookAddResult handleOutput(HookOutputRequest request) throws Exception {
        logger.info("获取Hook输出: %s count=%d", request.getHookId(), request.getOutputCount());

        List<HookAddResult.HookDetailInfo> output = HookManager.getHookOutputDetail(request.getHookId(), request.getOutputCount());
        if (output == null) {
            throw new IllegalArgumentException("未找到Hook (ID: " + request.getHookId() + ")");
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
        r.setMessage("获取 " + output.size() + " 条输出记录");
        return r;
    }

    @Override
    protected CommandResult executeInternal(CommandRequest request) throws Exception {
        if (request instanceof HookListRequest r) return handleList(r);
        if (request instanceof HookInfoRequest r) return handleInfo(r);
        if (request instanceof HookOutputRequest r) return handleOutput(r);
        
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }
}
