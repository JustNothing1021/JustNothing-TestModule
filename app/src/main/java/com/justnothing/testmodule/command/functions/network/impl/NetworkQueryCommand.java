package com.justnothing.testmodule.command.functions.network.impl;

import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.i18n.CliMessages;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.functions.network.NetworkTexts;
import com.justnothing.testmodule.command.functions.network.model.NetworkRequestInfo;
import com.justnothing.testmodule.command.functions.network.request.*;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.functions.network.response.NetworkResult;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class NetworkQueryCommand extends AbstractNetworkCommand<CommandRequest<?>, CommandResult> {

    @SuppressWarnings("unchecked")
    public NetworkQueryCommand() {
        super("network query", (Class) CommandRequest.class, CommandResult.class);
    }

    public NetworkResult handleStatus(NetworkStatusRequest request) {
        outln(Text.zhEn("=== 网络监控状态 ===", "=== Network monitoring status ===").text(), Colors.CYAN);
        outln("", Colors.WHITE);

        out(Text.zhEn("拦截状态: ", "Intercept: ").text(), Colors.CYAN);
        outln((manager.isInterceptEnabled() ? NetworkTexts.STATUS_ENABLED : NetworkTexts.STATUS_DISABLED).text(),
                manager.isInterceptEnabled() ? Colors.GREEN : Colors.RED);

        out(Text.zhEn("记录状态: ", "Recording: ").text(), Colors.CYAN);
        outln((manager.isRecordEnabled() ? NetworkTexts.STATUS_ENABLED : NetworkTexts.STATUS_DISABLED).text(),
                manager.isRecordEnabled() ? Colors.GREEN : Colors.RED);

        int totalRequests = manager.getAllRequests().size();
        out(Text.zhEn("已记录请求: ", "Recorded requests: ").text(), Colors.CYAN);
        outln(NetworkTexts.COUNT_ITEMS.format(totalRequests), Colors.YELLOW);

        int mockRules = manager.getAllMockRules().size();
        out(Text.zhEn("Mock规则: ", "Mock rules: ").text(), Colors.CYAN);
        outln(NetworkTexts.COUNT_ITEMS.format(mockRules), Colors.YELLOW);

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
            outln(Text.zhEn("没有记录的请求", "No recorded requests").text(), Colors.GRAY);
            NetworkResult r = new NetworkResult();
            r.setSubCommand("list");
            r.setSuccess(true);
            r.setMessage(Text.zhEn("无记录", "No records").text());
            return r;
        }
        
        outln(Text.zhEn("=== 请求列表 (%d) ===", "=== Request list (%d) ===").format(allRequests.size()), Colors.CYAN);
        outln("", Colors.WHITE);
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        
        for (NetworkRequestInfo info : allRequests) {
            out("[" + info.getId() + "] ", Colors.YELLOW);
            out(info.getMethod() + " ", Colors.GREEN);
            outln(info.getUrl(), Colors.WHITE);
            out(Text.zhEn("  状态: ", "  Status: ").text(), Colors.GRAY);
            out(info.getResponseCode() + "", info.getResponseCode() == 200 ? Colors.GREEN : Colors.RED);
            out(Text.zhEn(" | 时间: ", " | Time: ").text(), Colors.GRAY);
            outln(sdf.format(new Date(info.getRequestTime())), Colors.GRAY);
            outln("", Colors.WHITE);
        }
        
        NetworkResult r = new NetworkResult();
        r.setSubCommand("list");
        r.setSuccess(true);
        r.setMessage(Text.zhEn("共 %d 条记录", "%d records").format(allRequests.size()));
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
            err.setMessage(Text.zhEn("未找到请求 (ID: %d)", "Request not found (ID: %d)").format(requestId));
            return err;
        }

        outln(Text.zhEn("=== 请求详情 #%d ===", "=== Request details #%d ===").format(requestId), Colors.CYAN);
        outln("", Colors.WHITE);

        out("URL: ", Colors.CYAN); outln(targetInfo.getUrl(), Colors.WHITE);
        out(CliMessages.LABEL_METHOD.text(), Colors.CYAN); outln(targetInfo.getMethod(), Colors.GREEN);
        out(Text.zhEn("状态码: ", "Status code: ").text(), Colors.CYAN); outln(targetInfo.getResponseCode() + "", Colors.YELLOW);
        out("Host: ", Colors.CYAN); outln(targetInfo.getHost(), Colors.WHITE);
        out(Text.zhEn("客户端: ", "Client: ").text(), Colors.CYAN); outln(targetInfo.getClientType(), Colors.GRAY);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        out(Text.zhEn("时间: ", "Time: ").text(), Colors.CYAN); outln(sdf.format(new Date(targetInfo.getRequestTime())), Colors.GRAY);

        if (targetInfo.getHeaders() != null && !targetInfo.getHeaders().isEmpty()) {
            outln(Text.zhEn("\n请求头:", "\nRequest headers:").text(), Colors.CYAN);
            targetInfo.getHeaders().forEach((k, v) -> {
                out("  " + k + ": ", Colors.GRAY);
                outln(v, Colors.WHITE);
            });
        }

        if (targetInfo.getResponseBody() != null) {
            outln(Text.zhEn("\n响应体:", "\nResponse body:").text(), Colors.CYAN);
            String body = targetInfo.getResponseBody();
            if (body.length() > 500) {
                outln(body.substring(0, 500) + Text.zhEn("... (截断)", "... (truncated)").text(), Colors.GRAY);
            } else {
                outln(body, Colors.GRAY);
            }
        }

        NetworkResult r = new NetworkResult();
        r.setSubCommand("info");
        r.setSuccess(true);
        r.setMessage(Text.zhEn("请求 #%d 详情", "Request #%d details").format(requestId));
        return r;
    }

    public NetworkResult handleWatch(NetworkWatchRequest request) {
        outln(Text.zhEn("实时监控模式 (按 Ctrl+C 停止)...", "Watch mode (press Ctrl+C to stop)...").text(), Colors.CYAN);
        outln("", Colors.WHITE);
        outln(Text.zhEn("提示: 监控模式需要持续运行，建议在后台使用", "Tip: watch mode runs continuously; use it in the background").text(),
                Colors.GRAY);

        NetworkResult r = new NetworkResult();
        r.setSubCommand("watch");
        r.setSuccess(true);
        r.setMessage(Text.zhEn("监控模式启动", "Watch mode started").text());
        return r;
    }

    public NetworkResult handleExport(NetworkExportRequest request) {
        String filePath = request.getFilePath();

        List<NetworkRequestInfo> allRequests = manager.getAllRequests();

        outln(Text.zhEn("导出 %d 条请求记录到 %s", "Exporting %d request records to %s").format(allRequests.size(), filePath), Colors.CYAN);

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
            r.setMessage(Text.zhEn("导出 %d 条记录 (JSON) → %s", "Exported %d records (JSON) → %s")
                    .format(allRequests.size(), filePath));
            return r;
        } catch (Exception e) {
            NetworkResult err = new NetworkResult();
            err.setSubCommand("export");
            err.setSuccess(false);
            err.setMessage(Text.zhEn("导出失败: %s", "Export failed: %s").format(e.getMessage()));
            return err;
        }
    }

    @Override
    protected CommandResult executeRequest(CommandRequest<?> request) throws Exception {
        if (request instanceof NetworkListRequest r) return handleList(r);
        if (request instanceof NetworkInfoRequest r) return handleInfo(r);
        if (request instanceof NetworkExportRequest r) return handleExport(r);
        if (request instanceof NetworkStatusRequest r) return handleStatus(r);
        if (request instanceof NetworkWatchRequest r) return handleWatch(r);

        throw new IllegalArgumentException(NetworkTexts.UNSUPPORTED_REQUEST_TYPE.format(request.getClass().getSimpleName()));
    }
}
