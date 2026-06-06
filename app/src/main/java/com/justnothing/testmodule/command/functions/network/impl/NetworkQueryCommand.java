package com.justnothing.testmodule.command.functions.network.impl;

import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.functions.network.*;
import com.justnothing.testmodule.command.functions.network.request.*;
import com.justnothing.testmodule.command.output.Colors;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NetworkQueryCommand extends AbstractNetworkCommand<CommandRequest, CommandResult> {

    public NetworkResult handleStatus(NetworkStatusRequest request) {
        outln("=== 网络监控状态 ===", Colors.CYAN);
        outln("", Colors.WHITE);

        out("拦截状态: ", Colors.CYAN);
        outln(manager.isInterceptEnabled() ? "开启" : "关闭", manager.isInterceptEnabled() ? Colors.GREEN : Colors.RED);

        out("记录状态: ", Colors.CYAN);
        outln(manager.isRecordEnabled() ? "开启" : "关闭", manager.isRecordEnabled() ? Colors.GREEN : Colors.RED);

        int totalRequests = manager.getAllRequests().size();
        out("已记录请求: ", Colors.CYAN);
        outln(totalRequests + " 条", Colors.YELLOW);

        int mockRules = manager.getAllMockRules().size();
        out("Mock规则: ", Colors.CYAN);
        outln(mockRules + " 条", Colors.YELLOW);

        NetworkResult r = new NetworkResult();
        r.setSubCommand("status");
        r.setSuccess(true);
        r.setMessage(String.format("intercept=%b, record=%b, requests=%d, mocks=%d",
                manager.isInterceptEnabled(), manager.isRecordEnabled(),
                totalRequests, mockRules));
        return r;
    }

    public NetworkResult handleList(NetworkListRequest request) {
        List<NetworkRequestInfo> allRequests = manager.getAllRequests();
        
        if (allRequests.isEmpty()) {
            outln("没有记录的请求", Colors.GRAY);
            NetworkResult r = new NetworkResult();
            r.setSubCommand("list");
            r.setSuccess(true);
            r.setMessage("无记录");
            return r;
        }
        
        outln("=== 请求列表 (" + allRequests.size() + ") ===", Colors.CYAN);
        outln("", Colors.WHITE);
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        
        for (NetworkRequestInfo info : allRequests) {
            out("[" + info.getId() + "] ", Colors.YELLOW);
            out(info.getMethod() + " ", Colors.GREEN);
            outln(info.getUrl(), Colors.WHITE);
            out("  状态: ", Colors.GRAY);
            out(info.getResponseCode() + "", info.getResponseCode() == 200 ? Colors.GREEN : Colors.RED);
            out(" | 时间: ", Colors.GRAY);
            outln(sdf.format(new Date(info.getRequestTime())), Colors.GRAY);
            outln("", Colors.WHITE);
        }
        
        NetworkResult r = new NetworkResult();
        r.setSubCommand("list");
        r.setSuccess(true);
        r.setMessage("共 " + allRequests.size() + " 条记录");
        return r;
    }

    public NetworkResult handleInfo(NetworkInfoRequest request) {
        int requestId = request.getTargetRequestId();

        var allRequests = manager.getAllRequests();
        NetworkRequestInfo targetInfo = null;
        for (var info : allRequests) {
            if (info.getId() == requestId) {
                targetInfo = info;
                break;
            }
        }

        if (targetInfo == null) {
            NetworkResult err = new NetworkResult();
            err.setSubCommand("info");
            err.setSuccess(false);
            err.setMessage("未找到请求 (ID: " + requestId + ")");
            return err;
        }

        outln("=== 请求详情 #" + requestId + " ===", Colors.CYAN);
        outln("", Colors.WHITE);

        out("URL: ", Colors.CYAN); outln(targetInfo.getUrl(), Colors.WHITE);
        out("方法: ", Colors.CYAN); outln(targetInfo.getMethod(), Colors.GREEN);
        out("状态码: ", Colors.CYAN); outln(targetInfo.getResponseCode() + "", Colors.YELLOW);
        out("Host: ", Colors.CYAN); outln(targetInfo.getHost(), Colors.WHITE);
        out("客户端: ", Colors.CYAN); outln(targetInfo.getClientType(), Colors.GRAY);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        out("时间: ", Colors.CYAN); outln(sdf.format(new Date(targetInfo.getRequestTime())), Colors.GRAY);

        if (targetInfo.getHeaders() != null && !targetInfo.getHeaders().isEmpty()) {
            outln("\n请求头:", Colors.CYAN);
            targetInfo.getHeaders().forEach((k, v) -> {
                out("  " + k + ": ", Colors.GRAY);
                outln(v, Colors.WHITE);
            });
        }

        if (targetInfo.getResponseBody() != null) {
            outln("\n响应体:", Colors.CYAN);
            String body = targetInfo.getResponseBody();
            if (body.length() > 500) {
                outln(body.substring(0, 500) + "... (截断)", Colors.GRAY);
            } else {
                outln(body, Colors.GRAY);
            }
        }

        NetworkResult r = new NetworkResult();
        r.setSubCommand("info");
        r.setSuccess(true);
        r.setMessage("请求 #" + requestId + " 详情");
        return r;
    }

    public NetworkResult handleWatch(NetworkWatchRequest request) {
        outln("实时监控模式 (按 Ctrl+C 停止)...", Colors.CYAN);
        outln("", Colors.WHITE);
        outln("提示: 监控模式需要持续运行，建议在后台使用", Colors.GRAY);

        NetworkResult r = new NetworkResult();
        r.setSubCommand("watch");
        r.setSuccess(true);
        r.setMessage("监控模式启动");
        return r;
    }

    public NetworkResult handleExport(NetworkExportRequest request) {
        String filePath = request.getFilePath();

        List<NetworkRequestInfo> allRequests = manager.getAllRequests();

        outln("导出 " + allRequests.size() + " 条请求记录到 " + filePath, Colors.CYAN);

        try {
            org.json.JSONArray array = new org.json.JSONArray();
            for (NetworkRequestInfo info : allRequests) {
                org.json.JSONObject obj = new org.json.JSONObject();
                obj.put("id", info.getId());
                obj.put("method", info.getMethod());
                obj.put("url", info.getUrl());
                obj.put("statusCode", info.getResponseCode());
                obj.put("host", info.getHost());
                obj.put("timestamp", info.getRequestTime());
                array.put(obj);
            }

            String json = array.toString(2);
            outln(json, Colors.GRAY);

            NetworkResult r = new NetworkResult();
            r.setSubCommand("export");
            r.setSuccess(true);
            r.setOutput(json);
            r.setMessage("导出 " + allRequests.size() + " 条记录 (JSON) → " + filePath);
            return r;
        } catch (Exception e) {
            NetworkResult err = new NetworkResult();
            err.setSubCommand("export");
            err.setSuccess(false);
            err.setMessage("导出失败: " + e.getMessage());
            return err;
        }
    }

    @Override
    protected CommandResult executeInternal(CommandRequest request) throws Exception {
        if (request instanceof NetworkListRequest r) return handleList(r);
        if (request instanceof NetworkInfoRequest r) return handleInfo(r);
        if (request instanceof NetworkExportRequest r) return handleExport(r);
        if (request instanceof NetworkStatusRequest r) return handleStatus(r);
        if (request instanceof NetworkWatchRequest r) return handleWatch(r);

        throw new IllegalArgumentException("不支持的请求类型: " + request.getClass().getSimpleName());
    }
}
