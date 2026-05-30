package com.justnothing.testmodule.command.functions.network;

import static com.justnothing.testmodule.constants.CommandServer.CMD_NETWORK_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.base.protocol.CommandRequest;
import com.justnothing.testmodule.command.base.command.Cmd;
import com.justnothing.testmodule.command.base.command.CmdRoutes;
import com.justnothing.testmodule.command.base.command.CommandRouter;
import com.justnothing.testmodule.command.base.IllegalCommandLineArgumentException;
import com.justnothing.testmodule.command.base.command.CmdParamProcessor;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.logging.Logger;

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

import com.justnothing.testmodule.command.functions.network.request.NetworkListRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkInfoRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkFilterRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkMockAddRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkMockHeaderRequest;
import com.justnothing.testmodule.command.functions.network.request.NetworkExportRequest;

@Cmd(
    name = "network",
    description = "网络请求监控和调试工具",
    defaultResultType = NetworkResult.class
)
@CmdRoutes({
    @CmdRoutes.Route(path = "intercept", request = CommandRequest.class, handler = NetworkMain.class, description = "开启/关闭网络拦截"),
    @CmdRoutes.Route(path = "record", request = CommandRequest.class, handler = NetworkMain.class, description = "开启/关闭请求记录"),
    @CmdRoutes.Route(path = "status", request = CommandRequest.class, handler = NetworkMain.class, description = "显示当前状态"),
    @CmdRoutes.Route(path = "list", request = NetworkListRequest.class, handler = NetworkMain.class, description = "列出请求记录"),
    @CmdRoutes.Route(path = "info", request = NetworkInfoRequest.class, handler = NetworkMain.class, description = "查看请求详情"),
    @CmdRoutes.Route(path = "filter", request = NetworkFilterRequest.class, handler = NetworkMain.class, description = "过滤特定主机的请求"),
    @CmdRoutes.Route(path = "mock", request = CommandRequest.class, handler = NetworkMain.class, description = "Mock 规则管理"),
    @CmdRoutes.Route(path = "hook", request = CommandRequest.class, handler = NetworkMain.class, description = "Hook 管理"),
    @CmdRoutes.Route(path = "watch", request = CommandRequest.class, handler = NetworkMain.class, description = "实时监控"),
    @CmdRoutes.Route(path = "export", request = NetworkExportRequest.class, handler = NetworkMain.class, description = "导出请求记录"),
    @CmdRoutes.Route(path = "clear", request = CommandRequest.class, handler = NetworkMain.class, description = "清除请求记录"),
    @CmdRoutes.Route(path = "shutdown", request = CommandRequest.class, handler = NetworkMain.class, description = "关闭网络监控")
})
public class NetworkMain extends MainCommand<NetworkResult> {

    private static final Logger logger = Logger.getLoggerForName("NetworkMain");

    public NetworkMain() {
        super("network", NetworkResult.class);
    }

    @Override
    public String getHelpText() {
        return CommandRouter.getInstance().generateHelpForCommand("network");
    }

    @Override
    public NetworkResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String[] args = context.args();

        if (args.length < 1) {
            context.println(getHelpText(), Colors.WHITE);
            return null;
        }

        String subCommand = args[0];
        String[] subArgs = new String[args.length - 1];
        System.arraycopy(args, 1, subArgs, 0, subArgs.length);

        try {
            switch (subCommand) {
                case "intercept" -> handleIntercept(subArgs, context);
                case "record" -> handleRecord(subArgs, context);
                case "status" -> handleStatus(context);
                case "list" -> handleList(subArgs, context);
                case "info" -> handleInfo(subArgs, context);
                case "filter" -> handleFilter(subArgs, context);
                case "mock" -> handleMock(subArgs, context);
                case "hook" -> handleHook(subArgs, context);
                case "watch" -> handleWatch(subArgs, context);
                case "export" -> handleExport(subArgs, context);
                case "clear" -> handleClear(context);
                case "shutdown" -> handleShutdown(context);
                default -> {
                    context.print("未知子命令: ", Colors.RED);
                    context.println(subCommand, Colors.YELLOW);
                }
            }
        } catch (IllegalCommandLineArgumentException e) {
            throw e;
        } catch (Exception e) {
            CommandExceptionHandler.handleException("network " + subCommand, e, context, "执行 network 命令失败");
            return createErrorResult("执行network命令失败: " + e.getMessage());
        }

        NetworkResult result = new NetworkResult(java.util.UUID.randomUUID().toString());
        return result;
    }

    private void handleIntercept(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException("需要指定 on 或 off");
        }

        String action = args[0].toLowerCase(Locale.getDefault());
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

                logger.info("网络拦截已启用 (OkHttp=" + okHttp + ", HttpURL=" + httpUrl + ", Retrofit=" + retrofit + ")");
            }
            case "off" -> {
                manager.setInterceptEnabled(false);
                NetworkInterceptor.unhookAll();
                ctx.println("网络拦截已禁用", Colors.YELLOW);
                logger.info("网络拦截已禁用");
            }
            default -> throw new IllegalCommandLineArgumentException("无效参数: " + action + ", 请使用 on 或 off");
        }
    }

    private void handleRecord(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException("需要指定 on 或 off");
        }

        String action = args[0].toLowerCase(Locale.getDefault());
        NetworkManager manager = NetworkManager.getInstance();

        switch (action) {
            case "on" -> {
                manager.setRecordEnabled(true);
                ctx.println("请求记录已启用", Colors.LIGHT_GREEN);
                logger.debug("请求记录已启用");
            }
            case "off" -> {
                manager.setRecordEnabled(false);
                ctx.println("请求记录已禁用", Colors.YELLOW);
                logger.debug("请求记录已禁用");
            }
            default -> throw new IllegalCommandLineArgumentException("无效参数: " + action + ", 请使用 on 或 off");
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

    private void handleList(String[] args, CommandExecutor.CmdExecContext ctx) throws Exception {
        NetworkListRequest request = new NetworkListRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        NetworkManager manager = NetworkManager.getInstance();
        List<NetworkRequestInfo> requests;

        if (request.getMethod() != null) {
            requests = manager.getRequestsByMethod(request.getMethod().toUpperCase(Locale.getDefault()));
        } else if (request.getStatusFilter() != null && request.getStatusFilter().endsWith("xx")) {
            int prefix = Integer.parseInt(request.getStatusFilter().substring(0, 1));
            requests = manager.getRequestsByStatus(prefix * 100, (prefix + 1) * 100 - 1);
        } else {
            requests = manager.getAllRequests();
        }

        if (request.getHost() != null) {
            String finalHost = request.getHost();
            requests = requests.stream()
                    .filter(r -> r.getHost().contains(finalHost))
                    .collect(Collectors.toList());
        }

        if (request.getStatusFilter() != null && !request.getStatusFilter().endsWith("xx")) {
            try {
                int finalStatusCode = Integer.parseInt(request.getStatusFilter());
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

        displayRequestList(ctx, requests, request.getLimit());
    }

    private void displayRequestList(CommandExecutor.CmdExecContext ctx, List<NetworkRequestInfo> requests, int limit) {
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

    private void handleInfo(String[] args, CommandExecutor.CmdExecContext ctx) throws Exception {
        NetworkInfoRequest request = new NetworkInfoRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        NetworkRequestInfo reqInfo = NetworkManager.getInstance().getRequest(request.getTargetRequestId());
        if (reqInfo == null) {
            throw new IllegalCommandLineArgumentException("未找到请求记录 (ID: " + request.getTargetRequestId() + ")");
        }

        ctx.println(reqInfo.toString(), Colors.WHITE);
    }

    private void handleFilter(String[] args, CommandExecutor.CmdExecContext ctx) throws Exception {
        NetworkFilterRequest request = new NetworkFilterRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        List<NetworkRequestInfo> requests = NetworkManager.getInstance().getRequestsByHost(request.getHost());

        if (requests.isEmpty()) {
            ctx.println("没有找到匹配的请求", Colors.GRAY);
            return;
        }

        ctx.println("=== 过滤结果: " + request.getHost() + " ===", Colors.CYAN);
        ctx.println("");

        for (NetworkRequestInfo req : requests) {
            ctx.print(String.format(Locale.getDefault(), "[%d] ", req.getId()), Colors.GRAY);
            ctx.print(req.getMethod(), getMethodColor(req.getMethod()));
            ctx.print(" ", Colors.WHITE);
            ctx.print(req.getPath(), Colors.GREEN);
            ctx.print(" -> ", Colors.GRAY);
            ctx.println(String.valueOf(req.getResponseCode()), getStatusColor(req.getResponseCode()));
        }

        ctx.println("");
        ctx.print("共 ", Colors.GRAY);
        ctx.print(String.valueOf(requests.size()), Colors.YELLOW);
        ctx.println(" 条记录", Colors.GRAY);
    }

    private void handleMock(String[] args, CommandExecutor.CmdExecContext ctx) throws Exception {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException("需要指定子命令 (add|header|remove|list|clear)");
        }

        String subCmd = args[0];
        String[] mockArgs = new String[args.length - 1];
        System.arraycopy(args, 1, mockArgs, 0, mockArgs.length);

        switch (subCmd) {
            case "add" -> handleMockAdd(mockArgs, ctx);
            case "header" -> handleMockHeader(mockArgs, ctx);
            case "remove" -> handleMockRemove(mockArgs, ctx);
            case "list" -> handleMockList(ctx);
            case "clear" -> handleMockClear(ctx);
            default -> throw new IllegalCommandLineArgumentException("未知 mock 子命令: " + subCmd);
        }
    }

    private void handleMockAdd(String[] args, CommandExecutor.CmdExecContext ctx) throws Exception {
        NetworkMockAddRequest request = new NetworkMockAddRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        NetworkManager.getInstance().addMockRule(request.getPattern(), request.getResponse(), request.getStatusCode());

        ctx.println("Mock 规则已添加", Colors.LIGHT_GREEN);
        ctx.print("匹配: ", Colors.CYAN);
        ctx.println(request.getPattern(), Colors.YELLOW);
        ctx.print("状态码: ", Colors.CYAN);
        ctx.println(String.valueOf(request.getStatusCode()), Colors.GREEN);
        ctx.print("响应: ", Colors.CYAN);
        ctx.println(truncate(request.getResponse(), 100), Colors.GRAY);

        logger.debug("添加 Mock 规则: pattern=" + request.getPattern() + ", status=" + request.getStatusCode());
    }

    private void handleMockHeader(String[] args, CommandExecutor.CmdExecContext ctx) throws Exception {
        NetworkMockHeaderRequest request = new NetworkMockHeaderRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        List<NetworkManager.MockRule> rules = NetworkManager.getInstance().getAllMockRules();
        NetworkManager.MockRule targetRule = null;

        for (NetworkManager.MockRule rule : rules) {
            if (rule.pattern.equals(request.getPattern())) {
                targetRule = rule;
                break;
            }
        }

        if (targetRule == null) {
            throw new IllegalCommandLineArgumentException("未找到 Mock 规则: " + request.getPattern());
        }

        targetRule.addHeader(request.getHeaderName(), request.getHeaderValue());
        ctx.println("响应头已添加", Colors.LIGHT_GREEN);
        ctx.print(request.getPattern(), Colors.YELLOW);
        ctx.print(" -> ", Colors.WHITE);
        ctx.print(request.getHeaderName(), Colors.CYAN);
        ctx.print(": ", Colors.GRAY);
        ctx.println(request.getHeaderValue(), Colors.GREEN);
    }

    private void handleMockRemove(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException("需要指定 pattern");
        }

        String pattern = args[0];
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
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException("需要指定子命令 (remove|list)");
        }

        String subCmd = args[0];

        switch (subCmd) {
            case "remove" -> {
                if (args.length < 2) {
                    throw new IllegalCommandLineArgumentException("需要指定 Hook key");
                }
                String key = args[1];
                NetworkManager.getInstance().removeHook(key);
                ctx.println("Hook 已移除: " + key, Colors.LIGHT_GREEN);
            }
            case "list" -> {
                ctx.println("=== 活跃 Hook 列表 ===", Colors.CYAN);
                ctx.println("");
                ctx.print("Hook 数量: ", Colors.GRAY);
                ctx.println(String.valueOf(NetworkManager.getInstance().getHookCount()), Colors.YELLOW);
            }
            default -> throw new IllegalCommandLineArgumentException("未知 hook 子命令: " + subCmd);
        }
    }

    private void handleWatch(String[] args, CommandExecutor.CmdExecContext ctx) {
        if (args.length < 1) {
            throw new IllegalCommandLineArgumentException("需要指定 on 或 off");
        }

        String action = args[0].toLowerCase(Locale.getDefault());
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
            default -> throw new IllegalCommandLineArgumentException("无效参数: " + action + ", 请使用 on 或 off");
        }
    }

    private void handleExport(String[] args, CommandExecutor.CmdExecContext ctx) throws Exception {
        NetworkExportRequest request = new NetworkExportRequest();
        CmdParamProcessor.parseCommandLineArgs(request, args);

        List<NetworkRequestInfo> requests = NetworkManager.getInstance().getAllRequests();

        if (requests.isEmpty()) {
            ctx.println("没有请求记录可导出", Colors.GRAY);
            return;
        }

        JSONArray array = new JSONArray();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        for (NetworkRequestInfo req : requests) {
            JSONObject obj = new JSONObject();
            obj.put("id", req.getId());
            obj.put("url", req.getUrl());
            obj.put("method", req.getMethod());
            obj.put("clientType", req.getClientType());
            obj.put("requestTime", sdf.format(new Date(req.getRequestTime())));
            obj.put("responseCode", req.getResponseCode());
            obj.put("responseMessage", req.getResponseMessage());
            obj.put("duration", req.getDuration());
            obj.put("completed", req.isCompleted());

            if (!req.getHeaders().isEmpty()) {
                JSONObject headers = new JSONObject();
                for (Map.Entry<String, String> entry : req.getHeaders().entrySet()) {
                    headers.put(entry.getKey(), entry.getValue());
                }
                obj.put("requestHeaders", headers);
            }

            if (req.getRequestBody() != null) {
                obj.put("requestBody", req.getRequestBody());
            }

            if (req.getResponseHeaders() != null && !req.getResponseHeaders().isEmpty()) {
                JSONObject headers = new JSONObject();
                for (Map.Entry<String, String> entry : req.getResponseHeaders().entrySet()) {
                    headers.put(entry.getKey(), entry.getValue());
                }
                obj.put("responseHeaders", headers);
            }

            if (req.getResponseBody() != null) {
                obj.put("responseBody", req.getResponseBody());
            }

            if (req.getError() != null) {
                obj.put("error", req.getError().getMessage());
            }

            array.put(obj);
        }

        JSONObject root = new JSONObject();
        root.put("exportTime", sdf.format(new Date()));
        root.put("totalRequests", requests.size());
        root.put("requests", array);

        try {
            IOManager.writeFile(request.getFilePath(), root.toString(2));
            ctx.println("请求记录已导出", Colors.LIGHT_GREEN);
            ctx.print("文件: ", Colors.CYAN);
            ctx.println(request.getFilePath(), Colors.GREEN);
            ctx.print("记录数: ", Colors.CYAN);
            ctx.println(String.valueOf(requests.size()), Colors.YELLOW);

            logger.info("导出 %d 条请求记录到 %s", requests.size(), request.getFilePath());
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
