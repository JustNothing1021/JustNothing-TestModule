package com.justnothing.testmodule.command.agent;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;

import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.GsonFactory;
import com.justnothing.methodsclient.executor.AsyncChmodExecutor;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InspectionAgent {

    private static final String TAG = "InspectionAgent";
    private static final Logger logger = Logger.getLoggerForName(TAG);

    public static final String DESCRIPTOR_PREFIX = "methods-injector-";
    private static final String AGENT_WORK_DIR = "/data/local/tmp/methods/agent";

    private static volatile InspectionAgent instance;
    private static volatile boolean initialized = false;
    private static final Object initLock = new Object();

    private final Context applicationContext;
    private final String packageName;
    private final String socketName;
    private volatile LocalServerSocket serverSocket;
    private final ExecutorService executor;
    private volatile boolean running = false;

    public static synchronized boolean ensureInitialized(Context ctx) {
        if (initialized && instance != null) return true;
        synchronized (initLock) {
            if (initialized) return true;
            try {
                instance = new InspectionAgent(ctx.getApplicationContext());
                if (instance.startServer()) {
                    initialized = true;
                    registerBuiltinHandlers();
                    logger.info("Agent 已启动: " + instance.packageName + " socket=" + instance.socketName);
                } else {
                    logger.error("Agent 启动失败: " + instance.packageName);
                    instance = null;
                }
            } catch (Exception e) {
                logger.error("Agent 初始化异常", e);
                instance = null;
            }
        }
        return initialized;
    }

    private InspectionAgent(Context ctx) {
        this.applicationContext = ctx.getApplicationContext();
        this.packageName = ctx.getPackageName();
        this.socketName = DESCRIPTOR_PREFIX + packageName;
        this.executor = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "AgentWorker-" + packageName);
            t.setDaemon(true);
            return t;
        });
    }

    private boolean startServer() {
        try {
            // \0 前缀就代表这个二Socket是虚拟路径, 在内核里面用hash表示的, 不在文件系统里, 可以直接IPC
            // 不用chmod, 不用担心SELinux, 还是太好用了
            serverSocket = new LocalServerSocket("\0" + socketName);
            running = true;
            writeInfoFile();
            executor.submit(this::acceptLoop);

            logger.info("Agent 绑定 AbstractLocalSocket: " + socketName + " (" + packageName + ")");
            return true;
        } catch (Exception e) {
            logger.error("启动 LocalServerSocket 失败: " + e.getMessage(), e);
            return false;
        }
    }

    private void writeInfoFile() {
        try {
            File infoFile = new File(AGENT_WORK_DIR, DESCRIPTOR_PREFIX + packageName + ".info");
            FileOutputStream fos = new FileOutputStream(infoFile);
            fos.write((packageName + "\n" + System.currentTimeMillis() + "\n").getBytes(StandardCharsets.UTF_8));
            fos.getFD().sync();
            fos.close();
            AsyncChmodExecutor.chmodFile(infoFile.getAbsolutePath(), "644", false);
        } catch (Exception e) {
            logger.warn("写入 info 文件失败: " + e.getMessage());
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                LocalSocket client = serverSocket.accept();
                logger.debug("新连接来自: " + client);
                executor.submit(() -> handleClient(client));
            } catch (IOException e) {
                if (!running) break;
            }
        }
        logger.info("Agent accept 循环结束: " + packageName);
    }

    private void handleClient(LocalSocket client) {
        try (client) {
            try {
                BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));

                String requestJson = reader.readLine();
                if (requestJson == null || requestJson.isEmpty()) {
                    writer.write(buildError(-1, "EMPTY_REQUEST", "请求为空"));
                    writer.newLine();
                    writer.flush();
                    return;
                }

                JSONObject request = new JSONObject(requestJson);
                String commandType = request.optString("command", "");

                if ("PING".equals(commandType)) {
                    JSONObject resp = new JSONObject();
                    resp.put("returnCode", 0);
                    resp.put("data", buildPingData());
                    writer.write(resp.toString());
                    writer.newLine();
                    writer.flush();
                    return;
                }

                if (commandType.isEmpty()) {
                    writer.write(buildError(-1, "MISSING_COMMAND", "缺少 command 字段"));
                    writer.newLine();
                    writer.flush();
                    return;
                }

                JSONObject params = request.optJSONObject("params");

                AgentCommandHandler handler = AgentCommandRouter.getHandler(commandType);

                // === 内置系统命令（不需要注册到 Router） ===
                if ("_shutdown".equals(commandType)) {
                    // 关闭服务端，清理资源
                    JSONObject resp = new JSONObject();
                    resp.put("returnCode", 0);
                    resp.put("data", "shutdown accepted");
                    writer.write(resp.toString());
                    writer.newLine();
                    writer.flush();
                    // 延迟关闭，确保响应已发送
                    executor.execute(() -> shutdown());
                    return;
                }

                if ("_dispatch".equals(commandType)) {
                    // 代理执行任意主服务命令（支持交互式协议）
                    String cmdStr = params != null ? params.optString("command", "") : "";
                    if (cmdStr.isEmpty()) {
                        writer.write(buildError(-1, "MISSING_PARAM",
                                "_dispatch 缺少 command 参数"));
                        writer.newLine();
                        writer.flush();
                        return;
                    }
                    // 发送切换到交互模式的确认
                    JSONObject ack = new JSONObject();
                    ack.put("returnCode", 0);
                    ack.put("data", "switching to interactive mode");
                    writer.write(ack.toString());
                    writer.newLine();
                    writer.flush();

                    // 使用当前 Socket 的流进行交互式执行
                    executeDispatchedCommandInteractive(cmdStr, client.getInputStream(), client.getOutputStream());
                    return;
                }

                if (handler == null) {
                    writer.write(buildError(-1, "UNKNOWN_COMMAND",
                            "未知命令: " + commandType + ", 可用: " + AgentCommandRouter.getAvailableCommands()));
                    writer.newLine();
                    writer.flush();
                    return;
                }

                CommandResult result = handler.handle(params, applicationContext);

                JSONObject response = new JSONObject();
                response.put("returnCode", result.isSuccess() ? 0 : -1);
                if (!result.isSuccess()) {
                    CommandResult.ErrorInfo err = result.getError();
                    if (err != null) {
                        JSONObject errorJson = new JSONObject();
                        errorJson.put("code", err.getCode());
                        errorJson.put("message", err.getMessage());
                        response.put("error", errorJson);
                    }
                }
                response.put("data", new JSONObject(result.toJsonString()));
                writer.write(response.toString());
                writer.newLine();
                writer.flush();

            } catch (Exception e) {
                logger.error("处理客户端请求异常: " + e.getMessage(), e);
            }
        } catch (Exception ignored) {
        }
    }

    private JSONObject buildPingData() throws JSONException {
        JSONObject data = new JSONObject();
        data.put("packageName", packageName);
        data.put("startTime", System.currentTimeMillis());
        data.put("version", "1.0");
        return data;
    }

    private static void registerBuiltinHandlers() {
        AgentCommandRouter.register(new SpListHandler());
        AgentCommandRouter.register(new SpReadHandler());
        AgentCommandRouter.register(new SpWriteHandler());
        AgentCommandRouter.register(new DbListHandler());
        AgentCommandRouter.register(new DbQueryHandler());
        AgentCommandRouter.register(new DbTablesHandler());
    }

    private String buildError(int code, String errorType, String message) {
        try {
            JSONObject err = new JSONObject();
            err.put("code", errorType != null ? errorType : "UNKNOWN");
            err.put("message", message != null ? message : "unknown");
            JSONObject resp = new JSONObject();
            resp.put("returnCode", code);
            resp.put("error", err);
            return resp.toString();
        } catch (JSONException e) {
            return "{\"returnCode\":" + code + ",\"error\":{\"code\":\"UNKNOWN\",\"message\":\"" + message + "\"}}";
        }
    }

    public static InspectionAgent getInstance() {
        return instance;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSocketName() {
        return socketName;
    }

    public boolean isRunning() {
        return running && serverSocket != null;
    }

    /**
     * 优雅关闭 InspectionAgent
     * <p>
     * 1. 关闭 ServerSocket（acceptLoop 会退出）
     * 2. 清理 .info 文件
     * 3. 重置状态
     */
    public void shutdown() {
        logger.info("InspectionAgent 关闭: " + packageName);
        running = false;

        // 关闭 ServerSocket
        if (serverSocket != null) {
            try {
                serverSocket.close();
                logger.info("ServerSocket 已关闭: " + socketName);
            } catch (IOException e) {
                logger.warn("关闭 ServerSocket 失败: " + e.getMessage());
            }
            serverSocket = null;
        }

        // 清理 .info 文件
        File infoFile = new File(AGENT_WORK_DIR, DESCRIPTOR_PREFIX + packageName + ".info");
        if (infoFile.exists()) {
            try {
                infoFile.delete();
                logger.info("已清理 .info 文件: " + infoFile.getName());
            } catch (Exception e) {
                logger.warn("删除 .info 文件失败: " + e.getMessage());
            }
        }

        // 重置单例状态（允许重新初始化）
        instance = null;
        initialized = false;
    }

    /**
     * 在目标应用进程中通过 Socket 交互式执行代理命令
     * <p>
     * 复用当前 IPC Socket 连接，使用 InteractiveOutputHandler 进行双向通信。
     * 输入线程使用 InteractiveProtocol 二进制帧协议读取客户端响应，
     * 与主服务 SocketClientHandler.runInteractiveProtocolServer() 保持一致。
     *
     * @param commandStr 完整命令字符串
     * @param socketIn   Socket 输入流（用于读取客户端输入响应）
     * @param socketOut  Socket 输出流（用于发送输出和输入请求）
     */
    private void executeDispatchedCommandInteractive(String commandStr,
                                                      java.io.InputStream socketIn,
                                                      java.io.OutputStream socketOut) {
        try {
            com.justnothing.testmodule.command.CommandExecutor executor =
                    new com.justnothing.testmodule.command.CommandExecutor();

            // 使用 InteractiveOutputHandler — 通过 Socket 双向通信
            com.justnothing.testmodule.command.output.InteractiveOutputHandler outputHandler =
                    new com.justnothing.testmodule.command.output.InteractiveOutputHandler(socketOut);
            outputHandler.setSupportsInput(true);
            outputHandler.setCommand(commandStr);

            // 启动输入读取线程：使用 InteractiveProtocol 二进制帧协议读取客户端输入响应
            // 与 SocketClientHandler.runInteractiveProtocolServer() 保持一致的帧解析逻辑
            java.util.concurrent.atomic.AtomicBoolean inputRunning = new java.util.concurrent.atomic.AtomicBoolean(true);
            java.util.concurrent.ExecutorService inputExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
            inputExecutor.submit(() -> {
                try {
                    while (inputRunning.get() && !Thread.currentThread().isInterrupted()) {
                        Object[] packet = com.justnothing.testmodule.command.protocol.InteractiveProtocol
                                .readMessage(socketIn);
                        if (packet == null) {
                            logger.debug("_dispatch 客户端关闭连接");
                            break;
                        }
                        byte packetType = (byte) packet[0];
                        byte[] packetData = (byte[]) packet[1];

                        switch (packetType) {
                            case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_INPUT_RESPONSE:
                                if (packetData != null) {
                                    String response = new String(packetData, StandardCharsets.UTF_8);
                                    String[] parts = response.split(":", 2);
                                    if (parts.length == 2) {
                                        logger.debug("_dispatch 收到输入响应: " + parts[0]);
                                        outputHandler.handleInputResponse(parts[0], parts[1]);
                                    }
                                }
                                break;

                            case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_CLIENT_PONG:
                                logger.debug("_dispatch 收到 CLIENT_PONG");
                                break;

                            case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_INPUT_PONG:
                                com.justnothing.testmodule.command.output.InteractiveOutputHandler.lastResponseTime
                                        .getAndSet(System.currentTimeMillis());
                                logger.debug("_dispatch 收到 INPUT_PONG");
                                break;

                            case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_SET_HIGHLIGHT_MODE:
                                // 客户端发来的模式切换请求：转发给命令执行上下文
                                // 当前通过日志记录，未来可接入上下文感知的模式管理
                                if (packetData != null) {
                                    String mode = new String(packetData, StandardCharsets.UTF_8);
                                    logger.debug("_dispatch 收到客户端模式切换请求: " + mode);
                                }
                                break;

                            case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_TUI_WIDGET_CREATE:
                            case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_TUI_WIDGET_UPDATE:
                            case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_TUI_WIDGET_DESTROY:
                            case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_TUI_CLEAR_ALL:
                                // TUI Widget 控制消息：客户端→服务端方向暂不处理（当前为服务端驱动）
                                // 未来可支持双向通信（如客户端上报组件状态）
                                logger.debug("_dispatch 收到 TUI Widget 消息: %s",
                                        com.justnothing.testmodule.command.protocol.InteractiveProtocol
                                                .getMessageTypeName(packetType));
                                break;

                            default:
                                logger.debug("_dispatch 收到未知帧类型: " +
                                        com.justnothing.testmodule.command.protocol.InteractiveProtocol
                                                .getMessageTypeName(packetType));
                                break;
                        }
                    }
                } catch (Exception e) {
                    if (inputRunning.get()) {
                        logger.debug("_dispatch 输入读取结束: " + e.getMessage());
                    }
                }
            });

            try {
                // 执行命令（可能阻塞很久，比如 sinteractive）
                com.justnothing.testmodule.command.output.ClientRequirements requirements =
                        new com.justnothing.testmodule.command.output.ClientRequirements();
                requirements.setSupportsInput(true);
                executor.execute(commandStr, outputHandler, requirements);
            } finally {
                inputRunning.set(false);
                outputHandler.close();
                inputExecutor.shutdown();
            }

        } catch (Exception e) {
            logger.error("_dispatch 交互执行失败: " + commandStr + " - " + e.getMessage(), e);
            try {
                org.json.JSONObject errResp = new org.json.JSONObject();
                errResp.put("returnCode", -1);
                org.json.JSONObject errObj = new org.json.JSONObject();
                errObj.put("code", "DISPATCH_ERROR");
                errObj.put("message", "命令执行失败: " + e.getMessage());
                errResp.put("error", errObj);
                socketOut.write((errResp.toString() + "\n").getBytes(StandardCharsets.UTF_8));
                socketOut.flush();
            } catch (Exception ignored) {}
        }
    }
}
