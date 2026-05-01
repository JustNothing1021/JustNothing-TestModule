package com.justnothing.testmodule.command.functions.network;

import static com.justnothing.testmodule.constants.CommandServer.CMD_NETWORK_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.io.IOManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.justnothing.testmodule.command.base.RegisterCommand;

@RegisterCommand("network")
public class NetworkMain extends MainCommand<NetworkRequest, NetworkResult> {

    public NetworkMain() {
        super("Network", NetworkResult.class);
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: network <subcmd> [args...]
                
                网络请求监控和调试工具.
                
                子命令:
                    intercept on|off           - 开启/关闭网络拦截
                    record on|off              - 开启/关闭请求记录
                    status                     - 显示当前状态
                    list [options]             - 列出请求记录
                    info <id>                  - 查看请求详情
                    filter <host>              - 过滤特定主机的请求
                    mock add <pattern> <response> [status] [headers]
                                               - 添加 Mock 规则
                    mock header <pattern> <name> <value>
                                               - 为 Mock 规则添加响应头
                    mock remove <pattern>      - 移除 Mock 规则
                    mock list                  - 列出所有 Mock 规则
                    mock clear                 - 清除所有 Mock 规则
                    hook remove <key>          - 移除指定 Hook
                    watch on|off               - 开启/关闭实时监控
                    export [file]              - 导出请求记录
                    clear                      - 清除请求记录
                    shutdown                   - 关闭网络监控
                
                list 选项:
                    --method <method>          - 按请求方法过滤
                    --status <code>            - 按状态码过滤 (如 200, 404, 5xx)
                    --host <host>              - 按主机过滤
                    --limit <n>                - 限制显示数量
                
                示例:
                    network intercept on
                    network status
                    network list
                    network list --method POST
                    network list --status 404
                    network list --host api.example.com --limit 10
                    network info 1
                    network filter google.com
                    network mock add "api.test.com" '{"code":0}' 200
                    network mock header "api.test.com" "Content-Type" "application/json"
                    network mock remove "api.test.com"
                    network mock list
                    network hook remove okhttp_new_call
                    network watch on
                    network export /sdcard/network_log.json
                    network clear
                    network shutdown
                
                注意:
                    - intercept 模式会拦截所有 OkHttp 请求
                    - mock 规则支持 URL 匹配 (包含匹配或正则)
                    - 请求记录最多保留 100 条
                    - watch 模式会实时输出网络请求
                
                (Submodule network %s)
                """, CMD_NETWORK_VER);
    }

    @Override
    public NetworkResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        String subCommand = args[0];

        try {
            switch (subCommand) {
                case "intercept" -> handleIntercept(args, context);
                case "record" -> handleRecord(args, context);
                case "status" -> handleStatus(context);
                case "list" -> handleList(args, context);
                case "info" -> handleInfo(args, context);
                case "filter" -> handleFilter(args, context);
                case "mock" -> handleMock(args, context);
                case "hook" -> handleHook(args, context);
                case "watch" -> handleWatch(args, context);
                case "export" -> handleExport(args, context);
                case "clear" -> handleClear(context);
                case "shutdown" -> handleShutdown(context);
                default -> {
                    context.print("未知子命令: ", Colors.RED);
                    context.println(subCommand, Colors.YELLOW);
                }
            }
        } catch (Exception e) {
            CommandExceptionHandler.handleException("network", e, context, "执行 network 命令失败");

            if (shouldReturnStructuredData(context)) {
                return createErrorResult("执行network命令失败: " + e.getMessage());
            }
        }

        if (shouldReturnStructuredData(context)) {
            NetworkResult result = new NetworkResult(java.util.UUID.randomUUID().toString());
            return result;
        }
        return null;
    }

    private void handleIntercept(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 2) {
            ctx.println("错误: 需要指定 on 或 off", Colors.RED);
            ctx.println("用法: network intercept on|off", Colors.GRAY);
            return;
        }

        String action = args[1].toLowerCase(Locale.getDefault());
        NetworkManager manager = NetworkManager.getInstance();

        switch (action) {
            case "on" -> {
                ClassLoader classLoader = ctx.classLoader();
                boolean okHttp = NetworkInterceptor.hookOkHttp(classLoader);
                boolean httpUrl = NetworkInterceptor.hookHttpURLConnection(classLoader);
                boolean retrofit = NetworkInterceptor.hookRetrofit(classLoader);

                manager.setInterceptEnabled(true);

                ctx.println("网络拦截已启用", Colors.LIGHT_GREEN);
                ctx.print("OkHttp: ", Colors.CYAN);
                ctx.println(okHttp ? "已 Hook" : "未找到", okHttp ? Colors.GREEN : Colors.YELLOW);
                ctx.print("HttpURLConnection: ", Colors.CYAN);
                ctx.println(httpUrl ? "已 Hook" : "未找到", httpUrl ? Colors.GREEN : Colors.YELLOW);
                ctx.print("Retrofit: ", Colors.CYAN);
                ctx.println(retrofit ? "已 Hook" : "未找到", retrofit ? Colors.GREEN : Colors.YELLOW);
            }
            case "off" -> {
                manager.setInterceptEnabled(false);
                NetworkInterceptor.unhookAll();
                ctx.println("网络拦截已禁用", Colors.YELLOW);
            }
            default -> {
                ctx.print("错误: 无效参数 '", Colors.RED);
                ctx.print(action, Colors.YELLOW);
                ctx.println("', 请使用 on 或 off", Colors.RED);
            }
        }
    }

    private void handleRecord(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 2) {
            ctx.println("错误: 需要指定 on 或 off", Colors.RED);
            ctx.println("用法: network record on|off", Colors.GRAY);
            return;
        }

        String action = args[1].toLowerCase(Locale.getDefault());
        NetworkManager manager = NetworkManager.getInstance();

        switch (action) {
            case "on" -> {
                manager.setRecordEnabled(true);
                ctx.println("请求记录已启用", Colors.LIGHT_GREEN);
            }
            case "off" -> {
                manager.setRecordEnabled(false);
                ctx.println("请求记录已禁用", Colors.YELLOW);
            }
            default -> {
                ctx.print("错误: 无效参数 '", Colors.RED);
                ctx.print(action, Colors.YELLOW);
                ctx.println("', 请使用 on 或 off", Colors.RED);
            }
        }
    }

    private void handleStatus(CommandExecutor.CmdExecContext ctx) {
        NetworkManager manager = NetworkManager.getInstance();

        ctx.println("=== 网络监控状态 ===", Colors.CYAN);
        ctx.println("");

        ctx.print("拦截模式: ", Colors.GRAY);
        ctx.println(manager.isInterceptEnabled() ? "已启用" : "已禁用",
                manager.isInterceptEnabled() ? Colors.LIGHT_GREEN : Colors.YELLOW);

        ctx.print("记录模式: ", Colors.GRAY);
        ctx.println(manager.isRecordEnabled() ? "已启用" : "已禁用",
                manager.isRecordEnabled() ? Colors.LIGHT_GREEN : Colors.YELLOW);

        ctx.print("OkHttp Hook: ", Colors.GRAY);
        ctx.println(NetworkInterceptor.isOkHttpHooked() ? "已安装" : "未安装",
                NetworkInterceptor.isOkHttpHooked() ? Colors.GREEN : Colors.GRAY);

        ctx.print("HttpURLConnection Hook: ", Colors.GRAY);
        ctx.println(NetworkInterceptor.isHttpUrlHooked() ? "已安装" : "未安装",
                NetworkInterceptor.isHttpUrlHooked() ? Colors.GREEN : Colors.GRAY);

        ctx.print("Retrofit Hook: ", Colors.GRAY);
        ctx.println(NetworkInterceptor.isRetrofitHooked() ? "已安装" : "未安装",
                NetworkInterceptor.isRetrofitHooked() ? Colors.GREEN : Colors.GRAY);

        ctx.println("");
        ctx.print("请求记录: ", Colors.GRAY);
        ctx.print(String.valueOf(manager.getRequestCount()), Colors.YELLOW);
        ctx.println(" 条", Colors.GRAY);

        ctx.print("Mock 规则: ", Colors.GRAY);
        ctx.print(String.valueOf(manager.getMockRuleCount()), Colors.YELLOW);
        ctx.println(" 条", Colors.GRAY);

        ctx.print("活跃 Hook: ", Colors.GRAY);
        ctx.print(String.valueOf(manager.getHookCount()), Colors.YELLOW);
        ctx.println(" 个", Colors.GRAY);
    }

    private void handleList(String[] args, CommandExecutor.CmdExecContext ctx) {
        String method = null;
        String host = null;
        String statusFilter = null;
        int limit = 20;

        for (int i = 1; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--method" -> {
                    if (i + 1 < args.length) {
                        method = args[++i].toUpperCase(Locale.getDefault());
                    }
                }
                case "--host" -> {
                    if (i + 1 < args.length) {
                        host = args[++i];
                    }
                }
                case "--status" -> {
                    if (i + 1 < args.length) {
                        statusFilter = args[++i];
                    }
                }
                case "--limit" -> {
                    if (i + 1 < args.length) {
                        try {
                            limit = Integer.parseInt(args[++i]);
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }
        }

        NetworkManager manager = NetworkManager.getInstance();
        List<NetworkRequestInfo> requests;

        if (method != null) {
            requests = manager.getRequestsByMethod(method);
        } else if (statusFilter != null && statusFilter.endsWith("xx")) {
            int prefix = Integer.parseInt(statusFilter.substring(0, 1));
            requests = manager.getRequestsByStatus(prefix * 100, (prefix + 1) * 100 - 1);
        } else {
            requests = manager.getAllRequests();
        }

        if (host != null) {
            String finalHost = host;
            requests = requests.stream()
                    .filter(r -> r.getHost().contains(finalHost))
                    .collect(Collectors.toList());
        }

        if (statusFilter != null && !statusFilter.endsWith("xx")) {
            try {
                int finalStatusCode = Integer.parseInt(statusFilter);
                requests = requests.stream()
                        .filter(r -> r.getResponseCode() == finalStatusCode)
                        .collect(Collectors.toList());
            } catch (NumberFormatException ignored) {
            }
        }

        if (requests.isEmpty()) {
            ctx.println("没有找到请求记录", Colors.GRAY);
            return;
        }

        ctx.println("=== 网络请求列表 ===", Colors.CYAN);
        ctx.println("");

        int count = 0;
        for (NetworkRequestInfo request : requests) {
            if (count >= limit) break;

            byte statusColor = getStatusColor(request.getResponseCode());
            byte methodColor = getMethodColor(request.getMethod());

            ctx.print(String.format(Locale.getDefault(), "[%d] ", request.getId()), Colors.GRAY);
            ctx.print(request.getMethod(), methodColor);
            ctx.print(" ", Colors.WHITE);
            ctx.print(truncate(request.getHost(), 40), Colors.GREEN);
            ctx.print(" ", Colors.WHITE);

            if (request.isCompleted()) {
                if (request.getError() != null) {
                    ctx.print("ERROR", Colors.RED);
                } else {
                    ctx.print(String.valueOf(request.getResponseCode()), statusColor);
                }
            } else {
                ctx.print("PENDING", Colors.YELLOW);
            }

            ctx.print(" (", Colors.GRAY);
            ctx.print(String.valueOf(request.getDuration()), Colors.YELLOW);
            ctx.println("ms)", Colors.GRAY);

            count++;
        }

        ctx.println("");
        ctx.print("显示 ", Colors.GRAY);
        ctx.print(String.valueOf(Math.min(count, limit)), Colors.YELLOW);
        ctx.print(" / ", Colors.GRAY);
        ctx.print(String.valueOf(requests.size()), Colors.YELLOW);
        ctx.println(" 条记录", Colors.GRAY);
    }

    private void handleInfo(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 2) {
            ctx.println("错误: 需要指定请求 ID", Colors.RED);
            ctx.println("用法: network info <id>", Colors.GRAY);
            return;
        }

        int id;
        try {
            id = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            ctx.println("错误: 无效的 ID", Colors.RED);
            return;
        }

        NetworkRequestInfo request = NetworkManager.getInstance().getRequest(id);
        if (request == null) {
            ctx.println("错误: 未找到请求记录 (ID: " + id + ")", Colors.RED);
            return;
        }

        ctx.println(request.toString(), Colors.WHITE);
    }

    private void handleFilter(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 2) {
            ctx.println("错误: 需要指定主机名", Colors.RED);
            ctx.println("用法: network filter <host>", Colors.GRAY);
            return;
        }

        String host = args[1];
        List<NetworkRequestInfo> requests = NetworkManager.getInstance().getRequestsByHost(host);

        if (requests.isEmpty()) {
            ctx.println("没有找到匹配的请求", Colors.GRAY);
            return;
        }

        ctx.println("=== 过滤结果: " + host + " ===", Colors.CYAN);
        ctx.println("");

        for (NetworkRequestInfo request : requests) {
            ctx.print(String.format(Locale.getDefault(), "[%d] ", request.getId()), Colors.GRAY);
            ctx.print(request.getMethod(), getMethodColor(request.getMethod()));
            ctx.print(" ", Colors.WHITE);
            ctx.print(request.getPath(), Colors.GREEN);
            ctx.print(" -> ", Colors.GRAY);
            ctx.println(String.valueOf(request.getResponseCode()), getStatusColor(request.getResponseCode()));
        }

        ctx.println("");
        ctx.print("共 ", Colors.GRAY);
        ctx.print(String.valueOf(requests.size()), Colors.YELLOW);
        ctx.println(" 条记录", Colors.GRAY);
    }

    private void handleMock(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 2) {
            ctx.println("错误: 需要指定子命令", Colors.RED);
            ctx.println("用法: network mock <add|header|remove|list|clear>", Colors.GRAY);
            return;
        }

        String subCmd = args[1];

        switch (subCmd) {
            case "add" -> handleMockAdd(args, ctx);
            case "header" -> handleMockHeader(args, ctx);
            case "remove" -> handleMockRemove(args, ctx);
            case "list" -> handleMockList(ctx);
            case "clear" -> handleMockClear(ctx);
            default -> {
                ctx.print("未知子命令: ", Colors.RED);
                ctx.println(subCmd, Colors.YELLOW);
            }
        }
    }

    private void handleMockAdd(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 4) {
            ctx.println("错误: 参数不足", Colors.RED);
            ctx.println("用法: network mock add <pattern> <response> [status]", Colors.GRAY);
            return;
        }

        String pattern = args[2];
        String response = args[3];
        int status = 200;

        if (args.length >= 5) {
            try {
                status = Integer.parseInt(args[4]);
            } catch (NumberFormatException e) {
                ctx.println("警告: 无效的状态码，使用默认值 200", Colors.YELLOW);
            }
        }

        NetworkManager.getInstance().addMockRule(pattern, response, status);

        ctx.println("Mock 规则已添加", Colors.LIGHT_GREEN);
        ctx.print("匹配: ", Colors.CYAN);
        ctx.println(pattern, Colors.YELLOW);
        ctx.print("状态码: ", Colors.CYAN);
        ctx.println(String.valueOf(status), Colors.GREEN);
        ctx.print("响应: ", Colors.CYAN);
        ctx.println(truncate(response, 100), Colors.GRAY);
    }

    private void handleMockHeader(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 5) {
            ctx.println("错误: 参数不足", Colors.RED);
            ctx.println("用法: network mock header <pattern> <name> <value>", Colors.GRAY);
            return;
        }

        String pattern = args[2];
        String name = args[3];
        String value = args[4];

        List<NetworkManager.MockRule> rules = NetworkManager.getInstance().getAllMockRules();
        NetworkManager.MockRule targetRule = null;

        for (NetworkManager.MockRule rule : rules) {
            if (rule.pattern.equals(pattern)) {
                targetRule = rule;
                break;
            }
        }

        if (targetRule == null) {
            ctx.println("错误: 未找到 Mock 规则: " + pattern, Colors.RED);
            return;
        }

        targetRule.addHeader(name, value);
        ctx.println("响应头已添加", Colors.LIGHT_GREEN);
        ctx.print(pattern, Colors.YELLOW);
        ctx.print(" -> ", Colors.WHITE);
        ctx.print(name, Colors.CYAN);
        ctx.print(": ", Colors.GRAY);
        ctx.println(value, Colors.GREEN);
    }

    private void handleMockRemove(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 3) {
            ctx.println("错误: 需要指定 pattern", Colors.RED);
            ctx.println("用法: network mock remove <pattern>", Colors.GRAY);
            return;
        }

        String pattern = args[2];
        NetworkManager.getInstance().removeMockRule(pattern);
        ctx.println("Mock 规则已移除: " + pattern, Colors.LIGHT_GREEN);
    }

    private void handleMockList(CommandExecutor.CmdExecContext ctx) {
        List<NetworkManager.MockRule> rules = NetworkManager.getInstance().getAllMockRules();

        if (rules.isEmpty()) {
            ctx.println("没有 Mock 规则", Colors.GRAY);
            return;
        }

        ctx.println("=== Mock 规则列表 ===", Colors.CYAN);
        ctx.println("");

        for (int i = 0; i < rules.size(); i++) {
            NetworkManager.MockRule rule = rules.get(i);
            ctx.print(String.format(Locale.getDefault(), "%d. ", i + 1), Colors.GRAY);
            ctx.print(rule.pattern, Colors.YELLOW);
            ctx.print(" -> ", Colors.WHITE);
            ctx.print(String.valueOf(rule.statusCode), getStatusColor(rule.statusCode));
            ctx.println("", Colors.DEFAULT);
            ctx.print("   响应: ", Colors.GRAY);
            ctx.println(truncate(rule.response, 80), Colors.GRAY);

            if (!rule.headers.isEmpty()) {
                ctx.print("   响应头: ", Colors.GRAY);
                StringBuilder headerStr = new StringBuilder();
                for (Map.Entry<String, String> entry : rule.headers.entrySet()) {
                    if (headerStr.length() > 0) headerStr.append(", ");
                    headerStr.append(entry.getKey()).append("=").append(entry.getValue());
                }
                ctx.println(headerStr.toString(), Colors.CYAN);
            }
        }
    }

    private void handleMockClear(CommandExecutor.CmdExecContext ctx) {
        NetworkManager.getInstance().clearMockRules();
        ctx.println("所有 Mock 规则已清除", Colors.LIGHT_GREEN);
    }

    private void handleHook(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 2) {
            ctx.println("错误: 需要指定子命令", Colors.RED);
            ctx.println("用法: network hook <remove|list>", Colors.GRAY);
            return;
        }

        String subCmd = args[1];

        switch (subCmd) {
            case "remove" -> {
                if (args.length < 3) {
                    ctx.println("错误: 需要指定 Hook key", Colors.RED);
                    ctx.println("用法: network hook remove <key>", Colors.GRAY);
                    ctx.println("可用的 Hook: okhttp_new_call", Colors.GRAY);
                    return;
                }
                String key = args[2];
                NetworkManager.getInstance().removeHook(key);
                ctx.println("Hook 已移除: " + key, Colors.LIGHT_GREEN);
            }
            case "list" -> {
                ctx.println("=== 活跃 Hook 列表 ===", Colors.CYAN);
                ctx.println("");
                ctx.print("Hook 数量: ", Colors.GRAY);
                ctx.println(String.valueOf(NetworkManager.getInstance().getHookCount()), Colors.YELLOW);
            }
            default -> {
                ctx.print("未知子命令: ", Colors.RED);
                ctx.println(subCmd, Colors.YELLOW);
            }
        }
    }

    private void handleWatch(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 2) {
            ctx.println("错误: 需要指定 on 或 off", Colors.RED);
            ctx.println("用法: network watch on|off", Colors.GRAY);
            return;
        }

        String action = args[1].toLowerCase(Locale.getDefault());
        NetworkManager manager = NetworkManager.getInstance();

        switch (action) {
            case "on" -> {
                manager.addListener(new NetworkManager.NetworkListener() {
                    @Override
                    public void onRequestCreated(NetworkRequestInfo request) {
                        ctx.print("[Network] ", Colors.CYAN);
                        ctx.print(request.getMethod(), getMethodColor(request.getMethod()));
                        ctx.print(" ", Colors.WHITE);
                        ctx.println(request.getUrl(), Colors.GREEN);
                    }

                    @Override
                    public void onRequestCompleted(NetworkRequestInfo request) {
                        ctx.print("[Network] ", Colors.CYAN);
                        ctx.print("#" + request.getId() + " ", Colors.GRAY);
                        ctx.print("完成 ", Colors.LIGHT_GREEN);
                        ctx.print(String.valueOf(request.getResponseCode()), getStatusColor(request.getResponseCode()));
                        ctx.print(" (", Colors.GRAY);
                        ctx.print(String.valueOf(request.getDuration()), Colors.YELLOW);
                        ctx.println("ms)", Colors.GRAY);
                    }

                    @Override
                    public void onRequestFailed(NetworkRequestInfo request, Throwable error) {
                        ctx.print("[Network] ", Colors.CYAN);
                        ctx.print("#" + request.getId() + " ", Colors.GRAY);
                        ctx.print("失败: ", Colors.RED);
                        ctx.println(Objects.requireNonNullElse(error.getMessage(), "未知错误"), Colors.YELLOW);
                    }
                });
                ctx.println("实时监控已启用", Colors.LIGHT_GREEN);
            }
            case "off" -> {
                ctx.println("实时监控已禁用", Colors.YELLOW);
                ctx.println("提示: 重启命令会话以完全移除监听器", Colors.GRAY);
            }
            default -> {
                ctx.print("错误: 无效参数 '", Colors.RED);
                ctx.print(action, Colors.YELLOW);
                ctx.println("', 请使用 on 或 off", Colors.RED);
            }
        }
    }

    private void handleExport(String[] args, CommandExecutor.CmdExecContext ctx) throws JSONException {
        String filePath = args.length >= 2 ? args[1] : "/sdcard/network_log.json";

        List<NetworkRequestInfo> requests = NetworkManager.getInstance().getAllRequests();

        if (requests.isEmpty()) {
            ctx.println("没有请求记录可导出", Colors.GRAY);
            return;
        }

        JSONArray array = new JSONArray();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (NetworkRequestInfo request : requests) {
            JSONObject obj = new JSONObject();
            obj.put("id", request.getId());
            obj.put("url", request.getUrl());
            obj.put("method", request.getMethod());
            obj.put("clientType", request.getClientType());
            obj.put("requestTime", sdf.format(new Date(request.getRequestTime())));
            obj.put("responseCode", request.getResponseCode());
            obj.put("responseMessage", request.getResponseMessage());
            obj.put("duration", request.getDuration());
            obj.put("completed", request.isCompleted());

            if (!request.getHeaders().isEmpty()) {
                JSONObject headers = new JSONObject();
                for (Map.Entry<String, String> entry : request.getHeaders().entrySet()) {
                    headers.put(entry.getKey(), entry.getValue());
                }
                obj.put("requestHeaders", headers);
            }

            if (request.getRequestBody() != null) {
                obj.put("requestBody", request.getRequestBody());
            }

            if (request.getResponseHeaders() != null && !request.getResponseHeaders().isEmpty()) {
                JSONObject headers = new JSONObject();
                for (Map.Entry<String, String> entry : request.getResponseHeaders().entrySet()) {
                    headers.put(entry.getKey(), entry.getValue());
                }
                obj.put("responseHeaders", headers);
            }

            if (request.getResponseBody() != null) {
                obj.put("responseBody", request.getResponseBody());
            }

            if (request.getError() != null) {
                obj.put("error", request.getError().getMessage());
            }

            array.put(obj);
        }

        JSONObject root = new JSONObject();
        root.put("exportTime", sdf.format(new Date()));
        root.put("totalRequests", requests.size());
        root.put("requests", array);

        try {
            IOManager.writeFile(filePath, root.toString(2));
            ctx.println("请求记录已导出", Colors.LIGHT_GREEN);
            ctx.print("文件: ", Colors.CYAN);
            ctx.println(filePath, Colors.GREEN);
            ctx.print("记录数: ", Colors.CYAN);
            ctx.println(String.valueOf(requests.size()), Colors.YELLOW);
        } catch (Exception e) {
            ctx.println("导出失败: " + e.getMessage(), Colors.RED);
        }
    }

    private void handleClear(CommandExecutor.CmdExecContext ctx) {
        int count = NetworkManager.getInstance().getRequestCount();
        NetworkManager.getInstance().clearRequests();
        ctx.print("已清除 ", Colors.LIGHT_GREEN);
        ctx.print(String.valueOf(count), Colors.YELLOW);
        ctx.println(" 条请求记录", Colors.LIGHT_GREEN);
    }

    private void handleShutdown(CommandExecutor.CmdExecContext ctx) {
        NetworkManager.getInstance().shutdown();
        ctx.println("网络监控已关闭", Colors.YELLOW);
        ctx.println("所有 Hook、Mock 规则和请求记录已清除", Colors.GRAY);
    }

    private byte getStatusColor(int statusCode) {
        if (statusCode >= 200 && statusCode < 300) return Colors.LIGHT_GREEN;
        if (statusCode >= 300 && statusCode < 400) return Colors.YELLOW;
        if (statusCode >= 400 && statusCode < 500) return Colors.MAGENTA;
        if (statusCode >= 500) return Colors.RED;
        return Colors.GRAY;
    }

    private byte getMethodColor(String method) {
        return switch (method.toUpperCase(Locale.getDefault())) {
            case "GET" -> Colors.LIGHT_GREEN;
            case "POST" -> Colors.CYAN;
            case "PUT" -> Colors.YELLOW;
            case "DELETE" -> Colors.RED;
            case "PATCH" -> Colors.MAGENTA;
            default -> Colors.WHITE;
        };
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return "";
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength) + "...";
    }
}
