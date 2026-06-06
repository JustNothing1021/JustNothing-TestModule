package com.justnothing.testmodule.command.functions.network.impl;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.functions.network.NetworkManager;
import com.justnothing.testmodule.command.functions.network.NetworkResult;
import com.justnothing.testmodule.command.functions.network.request.*;
import com.justnothing.testmodule.command.output.Colors;

public class NetworkManageCommand extends AbstractNetworkCommand<CommandRequest, NetworkResult> {

    public NetworkResult handleIntercept(NetworkInterceptRequest request) {
        boolean enable = request.getEnable() == null || request.getEnable();
        
        manager.setInterceptEnabled(enable);
        
        if (enable) {
            outln("网络拦截已开启", Colors.GREEN);
        } else {
            outln("网络拦截已关闭", Colors.YELLOW);
        }
        
        NetworkResult r = okResult("intercept");
        r.setMessage(enable ? "拦截已开启" : "拦截已关闭");
        return r;
    }

    public NetworkResult handleRecord(NetworkRecordRequest request) {
        boolean enable = request.getEnable() == null || request.getEnable();
        
        manager.setRecordEnabled(enable);
        
        if (enable) {
            outln("请求记录已开启", Colors.GREEN);
        } else {
            outln("请求记录已关闭", Colors.YELLOW);
        }
        
        NetworkResult r = okResult("record");
        r.setMessage(enable ? "记录已开启" : "记录已关闭");
        return r;
    }

    public NetworkResult handleFilter(NetworkFilterRequest request) {
        String hostPattern = request.getHostPattern();
        
        if (hostPattern == null || hostPattern.isEmpty()) {
            outln("用法: network filter <host_pattern>", Colors.CYAN);
            return createErrorResult("参数不足: hostPattern");
        }
        
        logger.info("设置过滤模式: %s", hostPattern);
        
        var filtered = manager.getRequestsByHost(hostPattern);
        
        outln("过滤结果 (" + hostPattern + "):", Colors.CYAN);
        outln("找到 " + filtered.size() + " 个匹配请求", Colors.YELLOW);
        
        NetworkResult r = okResult("filter");
        r.setMessage("过滤: " + hostPattern + " (" + filtered.size() + " 个结果)");
        return r;
    }

    public NetworkResult handleMock(NetworkMockRequest request) {
        String subCmd = request.getSubCommand();
        
        if (subCmd == null || subCmd.isEmpty()) {
            outln("Mock 子命令: add/header/remove/list/clear", Colors.CYAN);
            return okResult("mock");
        }
        
        return switch (subCmd) {
            case "add" -> handleMockAdd(request);
            case "header" -> handleMockHeader(request);
            case "remove" -> handleMockRemove(request);
            case "list" -> handleMockList();
            case "clear" -> handleMockClear();
            default -> {
                outln("未知mock子命令: " + subCmd, Colors.RED);
                yield createErrorResult("未知子命令: " + subCmd);
            }
        };
    }

    private NetworkResult handleMockAdd(NetworkMockRequest request) {
        String pattern = request.getPattern();
        String response = request.getResponse();
        Integer statusCode = request.getStatusCode() != null ? request.getStatusCode() : 200;
        
        if (pattern == null || response == null) {
            return createErrorResult("参数不足: pattern, response");
        }
        
        manager.addMockRule(pattern, response, statusCode);
        outln("Mock规则已添加: " + pattern + " → " + statusCode, Colors.GREEN);
        
        NetworkResult r = okResult("mock:add");
        r.setMessage("添加规则: " + pattern);
        return r;
    }

    private NetworkResult handleMockHeader(NetworkMockRequest request) {
        String pattern = request.getPattern();
        String name = request.getHeaderName();
        String value = request.getHeaderValue();
        
        if (pattern == null || name == null) {
            return createErrorResult("参数不足: pattern, headerName");
        }
        
        var rule = manager.findMockRule(pattern);
        if (rule != null) {
            rule.addHeader(name, value != null ? value : "");
            outln("Mock头部已添加: " + name + "=" + value + " (" + pattern + ")", Colors.GREEN);
        } else {
            outln("未找到规则: " + pattern, Colors.RED);
            return createErrorResult("未找到规则: " + pattern);
        }
        
        NetworkResult r = okResult("mock:header");
        r.setMessage("添加头部: " + name + " (" + pattern + ")");
        return r;
    }

    private NetworkResult handleMockRemove(NetworkMockRequest request) {
        String pattern = request.getPattern();
        
        if (pattern == null) {
            return createErrorResult("参数不足: pattern");
        }
        
        manager.removeMockRule(pattern);
        outln("Mock规则已移除: " + pattern, Colors.GREEN);
        
        NetworkResult r = okResult("mock:remove");
        r.setMessage("移除规则: " + pattern);
        return r;
    }

    private NetworkResult handleMockList() {
        var rules = manager.getAllMockRules();
        
        outln("Mock规则列表 (" + rules.size() + "):", Colors.CYAN);
        for (var rule : rules) {
            out("  - " + rule.pattern + " → " + rule.statusCode, Colors.GREEN);
            if (!rule.headers.isEmpty()) {
                out(" [" + rule.headers.size() + " headers]", Colors.GRAY);
            }
            outln("", Colors.WHITE);
        }
        
        NetworkResult r = okResult("mock:list");
        r.setMessage("共 " + rules.size() + " 条规则");
        return r;
    }

    private NetworkResult handleMockClear() {
        manager.clearMockRules();
        outln("所有Mock规则已清除", Colors.GREEN);
        
        NetworkResult r = okResult("mock:clear");
        r.setMessage("规则已清除");
        return r;
    }

    public NetworkResult handleHook(NetworkHookRequest request) {
        String hookCmd = request.getSubCommand();
        
        if (hookCmd == null || hookCmd.isEmpty()) {
            outln("用法: network hook <add|remove|list|clear>", Colors.CYAN);
            return okResult("hook");
        }
        
        outln("Hook管理: " + hookCmd, Colors.CYAN);
        
        NetworkResult r = okResult("hook:" + hookCmd);
        r.setMessage("Hook " + hookCmd);
        return r;
    }

    public NetworkResult handleClear(NetworkClearRequest request) {
        int count = manager.getAllRequests().size();
        manager.clearRequests();
        
        out("已清除 ", Colors.LIGHT_GREEN);
        out(count + " ", Colors.YELLOW);
        outln(" 条请求记录", Colors.LIGHT_GREEN);
        
        NetworkResult r = okResult("clear");
        r.setMessage("已清除 " + count + " 条记录");
        return r;
    }

    public NetworkResult handleShutdown(NetworkShutdownRequest request) {
        outln("正在关闭网络监控...", Colors.YELLOW);
        manager.shutdown();
        outln("网络监控已关闭", Colors.GREEN);
        
        NetworkResult r = okResult("shutdown");
        r.setMessage("监控已关闭");
        return r;
    }

    @Override
    protected NetworkResult executeInternal(CommandRequest request) throws Exception {
        if (request instanceof NetworkInterceptRequest r) return handleIntercept(r);
        if (request instanceof NetworkRecordRequest r) return handleRecord(r);
        if (request instanceof NetworkFilterRequest r) return handleFilter(r);
        if (request instanceof NetworkMockRequest r) return handleMock(r);
        if (request instanceof NetworkHookRequest r) return handleHook(r);
        if (request instanceof NetworkClearRequest r) return handleClear(r);
        if (request instanceof NetworkShutdownRequest r) return handleShutdown(r);
        
        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }
}
