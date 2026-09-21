package com.justnothing.testmodule.command.functions.network.impl;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.network.NetworkTexts;
import com.justnothing.testmodule.command.functions.network.response.NetworkResult;
import com.justnothing.testmodule.command.functions.network.request.*;
import com.justnothing.testmodule.command.framework.output.Colors;

public class NetworkManageCommand extends AbstractNetworkCommand<CommandRequest<?>, NetworkResult> {

    private static final Text INTERCEPT_ON = Text.zhEn("拦截已开启", "Interception enabled");
    private static final Text INTERCEPT_OFF = Text.zhEn("拦截已关闭", "Interception disabled");
    private static final Text RECORD_ON = Text.zhEn("记录已开启", "Recording enabled");
    private static final Text RECORD_OFF = Text.zhEn("记录已关闭", "Recording disabled");

    @SuppressWarnings("unchecked")
    public NetworkManageCommand() {
        super("network manage", (Class) CommandRequest.class, NetworkResult.class);
    }

    public NetworkResult handleIntercept(NetworkInterceptRequest request) {
        boolean enable = request.getEnable() == null || request.getEnable();
        
        manager.setInterceptEnabled(enable);
        
        if (enable) {
            outln(Text.zhEn("网络拦截已开启", "Network interception enabled").text(), Colors.GREEN);
        } else {
            outln(Text.zhEn("网络拦截已关闭", "Network interception disabled").text(), Colors.YELLOW);
        }
        
        NetworkResult r = okResult("intercept");
        r.setMessage((enable ? INTERCEPT_ON : INTERCEPT_OFF).text());
        return r;
    }

    public NetworkResult handleRecord(NetworkRecordRequest request) {
        boolean enable = request.getEnable() == null || request.getEnable();
        
        manager.setRecordEnabled(enable);
        
        if (enable) {
            outln(Text.zhEn("请求记录已开启", "Request recording enabled").text(), Colors.GREEN);
        } else {
            outln(Text.zhEn("请求记录已关闭", "Request recording disabled").text(), Colors.YELLOW);
        }
        
        NetworkResult r = okResult("record");
        r.setMessage((enable ? RECORD_ON : RECORD_OFF).text());
        return r;
    }

    public NetworkResult handleFilter(NetworkFilterRequest request) {
        String hostPattern = request.getHostPattern();
        
        if (hostPattern == null || hostPattern.isEmpty()) {
            outln(CliMessages.HELP_USAGE_INLINE.text() + "network filter <host_pattern>", Colors.CYAN);
            return createErrorResult(CliMessages.ERR_NOT_ENOUGH_ARGS.text() + ": hostPattern");
        }
        
        logger.info("设置过滤模式: %s", hostPattern);
        
        var filtered = manager.getRequestsByHost(hostPattern);
        
        outln(Text.zhEn("过滤结果 (%s):", "Filter results (%s):").format(hostPattern), Colors.CYAN);
        outln(Text.zhEn("找到 %d 个匹配请求", "Found %d matching requests").format(filtered.size()), Colors.YELLOW);
        
        NetworkResult r = okResult("filter");
        r.setMessage(Text.zhEn("过滤: %s (%d 个结果)", "Filtered: %s (%d results)")
                .format(hostPattern, filtered.size()));
        return r;
    }

    public NetworkResult handleMock(NetworkMockRequest request) {
        String subCmd = request.getSubCommand();
        
        if (subCmd == null || subCmd.isEmpty()) {
            outln(Text.zhEn("Mock 子命令: add/header/remove/list/clear", "Mock subcommands: add/header/remove/list/clear").text(),
                    Colors.CYAN);
            return okResult("mock");
        }
        
        return switch (subCmd) {
            case "add" -> handleMockAdd(request);
            case "header" -> handleMockHeader(request);
            case "remove" -> handleMockRemove(request);
            case "list" -> handleMockList();
            case "clear" -> handleMockClear();
            default -> {
                outln(Text.zhEn("未知mock子命令: %s", "Unknown mock subcommand: %s").format(subCmd), Colors.RED);
                yield createErrorResult(Text.zhEn("未知子命令: %s", "Unknown subcommand: %s").format(subCmd));
            }
        };
    }

    private NetworkResult handleMockAdd(NetworkMockRequest request) {
        String pattern = request.getPattern();
        String response = request.getResponse();
        Integer statusCode = request.getStatusCode() != null ? request.getStatusCode() : 200;
        
        if (pattern == null || response == null) {
            return createErrorResult(CliMessages.ERR_NOT_ENOUGH_ARGS.text() + ": pattern, response");
        }
        
        manager.addMockRule(pattern, response, statusCode);
        outln(Text.zhEn("Mock规则已添加: %s → %s", "Mock rule added: %s → %s").format(pattern, statusCode), Colors.GREEN);
        
        NetworkResult r = okResult("mock:add");
        r.setMessage(Text.zhEn("添加规则: %s", "Rule added: %s").format(pattern));
        return r;
    }

    private NetworkResult handleMockHeader(NetworkMockRequest request) {
        String pattern = request.getPattern();
        String name = request.getHeaderName();
        String value = request.getHeaderValue();
        
        if (pattern == null || name == null) {
            return createErrorResult(CliMessages.ERR_NOT_ENOUGH_ARGS.text() + ": pattern, headerName");
        }
        
        var rule = manager.findMockRule(pattern);
        if (rule != null) {
            rule.addHeader(name, value != null ? value : "");
            outln(Text.zhEn("Mock头部已添加: %s=%s (%s)", "Mock header added: %s=%s (%s)").format(name, value, pattern), Colors.GREEN);
        } else {
            outln(NetworkTexts.RULE_NOT_FOUND.format(pattern), Colors.RED);
            return createErrorResult(NetworkTexts.RULE_NOT_FOUND.format(pattern));
        }
        
        NetworkResult r = okResult("mock:header");
        r.setMessage(Text.zhEn("添加头部: %s (%s)", "Header added: %s (%s)").format(name, pattern));
        return r;
    }

    private NetworkResult handleMockRemove(NetworkMockRequest request) {
        String pattern = request.getPattern();
        
        if (pattern == null) {
            return createErrorResult(CliMessages.ERR_NOT_ENOUGH_ARGS.text() + ": pattern");
        }
        
        manager.removeMockRule(pattern);
        outln(Text.zhEn("Mock规则已移除: %s", "Mock rule removed: %s").format(pattern), Colors.GREEN);
        
        NetworkResult r = okResult("mock:remove");
        r.setMessage(Text.zhEn("移除规则: %s", "Rule removed: %s").format(pattern));
        return r;
    }

    private NetworkResult handleMockList() {
        var rules = manager.getAllMockRules();
        
        outln(Text.zhEn("Mock规则列表 (%d):", "Mock rules (%d):").format(rules.size()), Colors.CYAN);
        for (var rule : rules) {
            out("  - " + rule.pattern + " → " + rule.statusCode, Colors.GREEN);
            if (!rule.headers.isEmpty()) {
                out(" [" + rule.headers.size() + " headers]", Colors.GRAY);
            }
            outln("", Colors.WHITE);
        }
        
        NetworkResult r = okResult("mock:list");
        r.setMessage(Text.zhEn("共 %d 条规则", "%d rules").format(rules.size()));
        return r;
    }

    private NetworkResult handleMockClear() {
        manager.clearMockRules();
        outln(Text.zhEn("所有Mock规则已清除", "All mock rules cleared").text(), Colors.GREEN);
        
        NetworkResult r = okResult("mock:clear");
        r.setMessage(Text.zhEn("规则已清除", "Rules cleared").text());
        return r;
    }

    public NetworkResult handleHook(NetworkHookRequest request) {
        String hookCmd = request.getSubCommand();
        
        if (hookCmd == null || hookCmd.isEmpty()) {
            outln(CliMessages.HELP_USAGE_INLINE.text() + "network hook <add|remove|list|clear>", Colors.CYAN);
            return okResult("hook");
        }
        
        outln(Text.zhEn("Hook管理: %s", "Hook management: %s").format(hookCmd), Colors.CYAN);
        
        NetworkResult r = okResult("hook:" + hookCmd);
        r.setMessage("Hook " + hookCmd);
        return r;
    }

    public NetworkResult handleClear(NetworkClearRequest request) {
        int count = manager.getAllRequests().size();
        manager.clearRequests();
        
        out(Text.zhEn("已清除 ", "Cleared ").text(), Colors.LIGHT_GREEN);
        out(count + " ", Colors.YELLOW);
        outln(Text.zhEn(" 条请求记录", " request records").text(), Colors.LIGHT_GREEN);
        
        NetworkResult r = okResult("clear");
        r.setMessage(Text.zhEn("已清除 %d 条记录", "Cleared %d records").format(count));
        return r;
    }

    public NetworkResult handleShutdown(NetworkShutdownRequest request) {
        outln(Text.zhEn("正在关闭网络监控...", "Shutting down network monitoring...").text(), Colors.YELLOW);
        manager.shutdown();
        outln(Text.zhEn("网络监控已关闭", "Network monitoring stopped").text(), Colors.GREEN);
        
        NetworkResult r = okResult("shutdown");
        r.setMessage(Text.zhEn("监控已关闭", "Monitoring stopped").text());
        return r;
    }

    @Override
    protected NetworkResult executeRequest(CommandRequest<?> request) throws Exception {
        if (request instanceof NetworkInterceptRequest r) return handleIntercept(r);
        if (request instanceof NetworkRecordRequest r) return handleRecord(r);
        if (request instanceof NetworkFilterRequest r) return handleFilter(r);
        if (request instanceof NetworkMockRequest r) return handleMock(r);
        if (request instanceof NetworkHookRequest r) return handleHook(r);
        if (request instanceof NetworkClearRequest r) return handleClear(r);
        if (request instanceof NetworkShutdownRequest r) return handleShutdown(r);
        
        throw new IllegalArgumentException(NetworkTexts.UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }
}
