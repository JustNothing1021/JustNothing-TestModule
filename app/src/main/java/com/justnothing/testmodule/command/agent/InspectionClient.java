package com.justnothing.testmodule.command.agent;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.justnothing.testmodule.command.base.protocol.CommandResult;
import com.justnothing.testmodule.command.base.protocol.GsonFactory;
import com.justnothing.testmodule.utils.io.ShellExecutorProvider;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InspectionClient {

    private static final Logger logger = Logger.getLoggerForName("InspectionClient");

    private static final String AGENT_WORK_DIR = "/data/local/tmp/methods/agent";

    public static AgentInfo ping(String packageName) throws AgentNotFoundException, AgentDeadException {
        if (!isAgentInfoExists(packageName)) throw new AgentNotFoundException(packageName);

        JSONObject request;
        try {
            request = new JSONObject();
            request.put("command", "PING");
        } catch (JSONException e) {
            throw new AgentDeadException(packageName);
        }

        try {
            JSONObject response = sendRequest(packageName, request);
            int returnCode = response.optInt("returnCode", -1);
            if (returnCode != 0) {
                throw new AgentDeadException(packageName);
            }
            JSONObject data = response.optJSONObject("data");
            if (data == null) {
                throw new AgentDeadException(packageName);
            }
            return new AgentInfo(
                    data.optString("packageName", packageName),
                    data.optLong("startTime", 0),
                    data.optString("version", "unknown"));
        } catch (AgentNotFoundException | AgentDeadException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentDeadException(packageName);
        }
    }

    public static boolean isAlive(String packageName) {
        try {
            ping(packageName);
            return true;
        } catch (AgentNotFoundException | AgentDeadException e) {
            return false;
        }
    }

    public static JSONObject execute(String packageName, String command,
                                     JSONObject params) throws Exception {
        if (!isAgentInfoExists(packageName)) throw new AgentNotFoundException(packageName);

        JSONObject request = new JSONObject();
        request.put("command", command);
        if (params != null) request.put("params", params);

        JSONObject response = sendRequest(packageName, request);

        int returnCode = response.optInt("returnCode", 0);
        if (returnCode < 0) {
            JSONObject error = response.optJSONObject("error");
            String code = error != null ? error.optString("code", "UNKNOWN") : "ERROR_" + returnCode;
            String msg = error != null ? error.optString("message", "未知错误") : "Agent 命令执行失败";
            throw new AgentCommandFailedException(code, msg);
        }

        return response;
    }

    public static Map<String, Object> executeAndGetData(String packageName, String command,
                                                        JSONObject params) throws Exception {
        JSONObject resp = execute(packageName, command, params);
        Object data = resp.opt("data");
        if (data instanceof JSONObject) {
            return jsonObjectToMap((JSONObject) data);
        }
        throw new AgentCommandFailedException("INVALID_RESPONSE",
                "期望 data 为 JSON Object, 实际: " + (data != null ? data.getClass().getSimpleName() : "null"));
    }

    public static List<Map<String, Object>> executeAndGetList(String packageName, String command,
                                                               JSONObject params) throws Exception {
        JSONObject resp = execute(packageName, command, params);
        Object data = resp.opt("data");
        if (data instanceof JSONArray arr) {
            List<Map<String, Object>> result = new ArrayList<>(arr.length());
            for (int i = 0; i < arr.length(); i++) {
                Object item = arr.opt(i);
                if (item instanceof JSONObject) result.add(jsonObjectToMap((JSONObject) item));
            }
            return result;
        }
        throw new AgentCommandFailedException("INVALID_RESPONSE",
                "期望 data 为 JSON Array, 实际: " + (data != null ? data.getClass().getSimpleName() : "null"));
    }

    public static Set<String> getAvailableCommands(String packageName) throws Exception {
        JSONObject resp = execute(packageName, "_list_commands", null);
        Object data = resp.opt("data");
        if (data instanceof JSONArray arr) {
            Set<String> cmds = new HashSet<>();
            for (int i = 0; i < arr.length(); i++) {
                String cmd = arr.optString(i, null);
                if (cmd != null) cmds.add(cmd);
            }
            return cmds;
        }
        return Collections.emptySet();
    }

    // ==================== Agent 生命周期管理 ====================

    /**
     * 列出所有在线 Agent（带 ping 校验 + 死文件清理）
     * <p>
     * 扫描 AGENT_WORK_DIR 下所有 .info 文件，对每个尝试 ping。
     * - ping 成功 → 在线
     * - ping 失败 → 清理遗留的 .info 文件（目标应用已崩溃/下线）
     *
     * @return 在线 Agent 列表（已清理死文件）
     */
    public static List<AgentStatus> listAllAgents() {
        List<AgentStatus> result = new ArrayList<>();
        File workDir = new File(AGENT_WORK_DIR);
        if (!workDir.exists() || !workDir.isDirectory()) {
            return result;
        }

        File[] infoFiles = workDir.listFiles((dir, name) ->
                name.startsWith(InspectionAgent.DESCRIPTOR_PREFIX) && name.endsWith(".info"));
        if (infoFiles == null || infoFiles.length == 0) {
            return result;
        }

        for (File infoFile : infoFiles) {
            // 从文件名提取 packageName: methods-injector-{pkg}.info → {pkg}
            String fileName = infoFile.getName();
            String pkg = fileName.substring(
                    InspectionAgent.DESCRIPTOR_PREFIX.length(),
                    fileName.length() - ".info".length());

            try {
                AgentInfo info = ping(pkg);
                result.add(new AgentStatus(pkg, info.startTime(), info.version(), true, null));
            } catch (AgentNotFoundException e) {
                // .info 文件存在但内容异常，清理
                logger.warn("Agent .info 文件损坏: " + pkg + ", 删除");
                safeDelete(infoFile);
            } catch (AgentDeadException e) {
                // 目标应用已下线但 .info 文件残留，清理
                logger.info("Agent 已离线 (清理遗留文件): " + pkg);
                safeDelete(infoFile);
                result.add(new AgentStatus(pkg, 0, "unknown", false, "应用已下线"));
            }
        }

        return result;
    }

    /**
     * 请求启动目标应用的 InspectionAgent
     * <p>
     * 通过写入 sentinel 激活文件来触发 InspectionAgentHook 的哨兵线程，
     * 哨兵线程检测到文件后会初始化 InspectionAgent。
     *
     * @param packageName 目标应用包名
     * @return true 如果之前已经激活或成功写入标记文件
     */
    public static boolean requestStart(String packageName) {
        if (InspectionAgentHook.isActive(packageName)) {
            logger.debug(packageName + " 已经激活");
            return true;
        }
        return InspectionAgentHook.requestActivation(packageName);
    }

    /**
     * 请求停止目标应用的 InspectionAgent
     * <p>
     * 两步操作：
     * 1. 通过 IPC 发送 _shutdown 命令让 Agent 关闭 ServerSocket
     * 2. 删除 sentinel 激活文件防止重启后自动激活
     *
     * @param packageName 目标应用包名
     * @return true 如果停止成功（或 Agent 本身就不在线）
     */
    public static boolean requestStop(String packageName) {
        // Step 1: 尝试通过 IPC 发送 shutdown 命令
        try {
            execute(packageName, "_shutdown", null);
            logger.info("已发送 shutdown 命令: " + packageName);
        } catch (AgentNotFoundException e) {
            logger.debug(packageName + " Agent 不在线, 无需 shutdown");
        } catch (Exception e) {
            logger.warn("发送 shutdown 失败 (可能已离线): " + packageName + " - " + e.getMessage());
        }

        // Step 2: 清理 sentinel 激活文件
        InspectionAgentHook.deactivate(packageName);

        // Step 3: 清理残留的 .info 文件
        File infoFile = new File(AGENT_WORK_DIR, InspectionAgent.DESCRIPTOR_PREFIX + packageName + ".info");
        if (infoFile.exists()) {
            safeDelete(infoFile);
        }

        return true;
    }

    private static void safeDelete(File file) {
        try {
            if (file.exists()) {
                ShellExecutorProvider.get().execute("rm -f " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            logger.warn("删除文件失败: " + file.getAbsolutePath() + " - " + e.getMessage());
        }
    }

    // ==================== 交互式命令执行 ====================

    /**
     * 以交互式协议在目标应用上执行命令
     * <p>
     * 与普通 execute() 不同，此方法不会在收到首个 JSON 响应后关闭连接，
     * 而是切换到 InteractiveProtocol 二进制帧模式进行双向通信。
     * 帧处理逻辑完全对齐 {@link com.justnothing.methodsclient.executor.SocketStreamReader}。
     * <p>
     * 流程：
     * 1. 连接目标 Agent 的 LocalSocket
     * 2. 发送 _dispatch JSON 请求
     * 3. 读取 ACK 响应（确认切换到交互模式）
     * 4. 进入二进制帧循环（与 SocketStreamReader.runInteractiveMainLoop 一致的逻辑）
     *
     * @param packageName 目标应用包名
     * @param command     要执行的命令字符串
     * @param callback    交互回调（用于输出显示 + 输入读取）
     */
    public static void executeInteractive(String packageName, String command,
                                          InteractiveDispatchCallback callback) throws Exception {
        if (!isAgentInfoExists(packageName)) throw new AgentNotFoundException(packageName);

        try (LocalSocket socket = new LocalSocket()) {
            socket.connect(new LocalSocketAddress(
                    "\0" + getSocketName(packageName),
                    LocalSocketAddress.Namespace.ABSTRACT));

            java.io.OutputStream socketOut = socket.getOutputStream();
            java.io.InputStream socketIn = socket.getInputStream();

            // Step 1: 发送 _dispatch JSON 请求
            JSONObject request = new JSONObject();
            request.put("command", "_dispatch");
            JSONObject params = new JSONObject();
            params.put("command", command);
            request.put("params", params);

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socketOut, StandardCharsets.UTF_8));
            writer.write(request.toString());
            writer.newLine();
            writer.flush();

            // Step 2: 读取 ACK 响应
            BufferedReader reader = new BufferedReader(new InputStreamReader(socketIn, StandardCharsets.UTF_8));
            String ackLine = reader.readLine();
            if (ackLine == null || ackLine.isEmpty()) {
                throw new AgentDeadException(packageName);
            }

            JSONObject ack = new JSONObject(ackLine);
            int returnCode = ack.optInt("returnCode", -1);
            if (returnCode != 0) {
                JSONObject error = ack.optJSONObject("error");
                String errMsg = error != null ? error.optString("message", "dispatch 被拒绝") : "未知错误";
                callback.onError(errMsg);
                return;
            }

            // Step 3: 进入交互式二进制帧循环（对齐 SocketStreamReader）
            callback.onSessionStart(command);

            java.util.concurrent.atomic.AtomicBoolean running = new java.util.concurrent.atomic.AtomicBoolean(true);
            java.util.concurrent.atomic.AtomicLong lastResponseTime =
                    new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
            Object writeLock = new Object();

            // 启动 CLIENT_PING 保活线程（与 SocketStreamReader.startPingThread 一致）
            java.util.concurrent.ScheduledFuture<?> pingFuture = null;
            try {
                pingFuture = com.justnothing.testmodule.utils.concurrent.ThreadPoolManager.scheduleWithFixedDelayUntil(
                        () -> {
                            try {
                                synchronized (writeLock) {
                                    com.justnothing.testmodule.command.protocol.InteractiveProtocol.writeMessage(
                                            socketOut,
                                            com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_CLIENT_PING,
                                            null);
                                }
                            } catch (IOException ignored) {}
                        },
                        0, 5000, java.util.concurrent.TimeUnit.MILLISECONDS,
                        () -> !running.get()
                                || System.currentTimeMillis() - lastResponseTime.get() > 30000
                );
            } catch (Exception e) {
                logger.warn("启动 PING 线程失败（非致命）: " + e.getMessage());
            }

            try {
                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    Object[] packet = com.justnothing.testmodule.command.protocol.InteractiveProtocol
                            .readMessage(socketIn);
                    if (packet == null) {
                        logger.debug("Agent 交互连接关闭");
                        break;
                    }

                    byte frameType = (byte) packet[0];
                    byte[] frameData = (byte[]) packet[1];
                    lastResponseTime.set(System.currentTimeMillis());

                    // 帧分发（完全对齐 SocketStreamReader.handleInteractivePacket）
                    switch (frameType) {
                        case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_SERVER_OUTPUT:
                            // 对齐 SocketStreamReader.handleServerOutput: System.out.print(text)
                            if (frameData != null) {
                                String text = new String(frameData, StandardCharsets.UTF_8);
                                callback.onOutput(text);
                            }
                            break;

                        case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_COLORED_OUTPUT:
                            // 对齐 SocketStreamReader.handleColoredOutput
                            if (frameData != null && frameData.length > 0) {
                                Object[] decoded = com.justnothing.testmodule.command.protocol.InteractiveProtocol
                                        .decodeColoredOutput(frameData);
                                byte color = (byte) decoded[0];
                                String text = (String) decoded[1];
                                callback.onColoredOutput(text, color);
                            }
                            break;

                        case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_SERVER_ERROR:
                            // 对齐 SocketStreamReader.handleServerError
                            if (frameData != null) {
                                String errorText = new String(frameData, StandardCharsets.UTF_8);
                                callback.onError(errorText);
                            }
                            break;

                        case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_SERVER_INPUT_REQUEST:
                            // 对齐 SocketStreamReader.handleInputRequest (line 393-430)
                            // 异步处理输入（不阻塞帧循环），确保能继续响应 PING/PONG 保活
                            // callback.onInputRequest() 内部走 context.readLine() → 标准交互式协议
                            if (frameData != null) {
                                final String requestData = new String(frameData, StandardCharsets.UTF_8);
                                final OutputStream finalOut = socketOut;
                                java.util.concurrent.ExecutorService inputExec =
                                        java.util.concurrent.Executors.newSingleThreadExecutor();
                                inputExec.submit(() -> {
                                    try {
                                        String[] parts = requestData.split(":", 2);
                                        if (parts.length != 2) {
                                            logger.debug("_dispatch 输入请求格式异常: " + requestData);
                                            return;
                                        }
                                        String requestId = parts[0];
                                        String prompt = parts[1];
                                        boolean isPassword = prompt.startsWith("PASSWORD:");
                                        if (isPassword) {
                                            prompt = prompt.substring(9);
                                        }
                                        // 通过 callback 走 context.readLine(prompt) → InteractiveOutputHandler
                                        // → 向原始客户端发 TYPE_SERVER_INPUT_REQUEST → TerminalManager 读输入 → 返回
                                        String userInput = callback.onInputRequest(prompt);
                                        String response = requestId + ":"
                                                + (userInput == null ? "" : userInput);
                                        synchronized (writeLock) {
                                            com.justnothing.testmodule.command.protocol.InteractiveProtocol.writeMessage(
                                                    finalOut,
                                                    com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_INPUT_RESPONSE,
                                                    response.getBytes(StandardCharsets.UTF_8));
                                        }
                                    } catch (Exception e) {
                                        logger.error("_dispatch 处理输入请求失败", e);
                                    }
                                });
                                inputExec.shutdown();
                            }
                            break;

                        case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_SERVER_PING:
                            // 对齐 SocketStreamReader.handleServerPing
                            try {
                                synchronized (writeLock) {
                                    com.justnothing.testmodule.command.protocol.InteractiveProtocol.writeMessage(
                                            socketOut,
                                            com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_CLIENT_PONG,
                                            null);
                                }
                            } catch (IOException ignored) {}
                            break;

                        case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_SERVER_PONG:
                            logger.debug("_dispatch 收到 SERVER_PONG");
                            break;

                        case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_INPUT_PING:
                            // 对齐 SocketStreamReader.handleInputPing
                            try {
                                com.justnothing.testmodule.command.protocol.InteractiveProtocol.writeMessage(
                                        socketOut,
                                        com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_INPUT_PONG,
                                        null);
                            } catch (IOException ignored) {}
                            break;

                        case com.justnothing.testmodule.command.protocol.InteractiveProtocol.TYPE_COMMAND_END:
                            // 对齐 SocketStreamReader: 收到 COMMAND_END 时退出循环
                            logger.debug("_dispatch 收到 COMMAND_END");
                            running.set(false);
                            break;

                        default:
                            logger.debug("_dispatch 未知的消息类型: " +
                                    com.justnothing.testmodule.command.protocol.InteractiveProtocol
                                            .getMessageTypeName(frameType));
                            break;
                    }
                }
            } catch (IOException e) {
                if (running.get()) {
                    logger.debug("Agent 交互连接异常: " + e.getMessage());
                }
            } finally {
                if (pingFuture != null) pingFuture.cancel(true);
                callback.onSessionEnd();
            }
        }
    }

    /**
     * 交互式 dispatch 的回调接口
     * <p>
     * 用于将 Agent 的输出/输入请求桥接到 CLI 上下文
     */
    public interface InteractiveDispatchCallback {
        /** 会话开始（ACK 已确认） */
        void onSessionStart(String command);

        /** 收到普通文本输出 */
        void onOutput(String text);

        /** 收到带颜色的文本输出 */
        void onColoredOutput(String text, byte color);

        /** 收到错误输出 */
        void onError(String errorText);

        /** 收到输入请求，返回用户输入 */
        String onInputRequest(String prompt);

        /** 会话结束 */
        void onSessionEnd();
    }

    // ==================== 类型化方法 (返回 CommandResult 子类) ====================

    public static SpListResult executeSpList(String packageName) throws Exception {
        JSONObject resp = execute(packageName, "sp_list", null);
        return parseResultData(resp, SpListResult.class);
    }

    public static SpReadResult executeSpRead(String packageName, String spName,
                                              String keyFilter) throws Exception {
        JSONObject params = new JSONObject();
        params.put("spName", spName);
        if (keyFilter != null && !keyFilter.isEmpty()) {
            params.put("keyFilter", keyFilter);
        }
        JSONObject resp = execute(packageName, "sp_read", params);
        return parseResultData(resp, SpReadResult.class);
    }

    public static SpWriteResult executeSpWrite(String packageName, String spName,
                                                String key, Object value,
                                                int valueType) throws Exception {
        JSONObject params = new JSONObject();
        params.put("spName", spName);
        params.put("key", key);
        params.put("valueType", valueType);
        putValue(params, "value", value);
        JSONObject resp = execute(packageName, "sp_write", params);
        return parseResultData(resp, SpWriteResult.class);
    }

    public static DbListResult executeDbList(String packageName) throws Exception {
        JSONObject resp = execute(packageName, "db_list", null);
        return parseResultData(resp, DbListResult.class);
    }

    public static DbQueryResult executeDbQuery(String packageName, String dbName,
                                                String sql, int limit) throws Exception {
        JSONObject params = new JSONObject();
        params.put("dbName", dbName);
        params.put("sql", sql);
        params.put("limit", limit);
        JSONObject resp = execute(packageName, "db_query", params);
        return parseResultData(resp, DbQueryResult.class);
    }

    public static DbTablesResult executeDbTables(String packageName,
                                                  String dbName) throws Exception {
        JSONObject params = new JSONObject();
        params.put("dbName", dbName);
        JSONObject resp = execute(packageName, "db_tables", params);
        return parseResultData(resp, DbTablesResult.class);
    }

    private static <T extends CommandResult> T parseResultData(JSONObject response,
                                                               Class<T> resultType) throws Exception {
        Object data = response.opt("data");
        if (data == null || data == JSONObject.NULL) {
            T empty = resultType.getDeclaredConstructor().newInstance();
            empty.setSuccess(true);
            return empty;
        }
        String jsonStr = data.toString();
        return GsonFactory.getInstance().fromJson(jsonStr, resultType);
    }

    private static void putValue(JSONObject obj, String key, Object value) throws JSONException {
        if (value == null) {
            obj.put(key, JSONObject.NULL);
        } else if (value instanceof Integer) {
            obj.put(key, ((Integer) value).intValue());
        } else if (value instanceof Long) {
            obj.put(key, ((Long) value).longValue());
        } else if (value instanceof Double) {
            obj.put(key, ((Double) value).doubleValue());
        } else if (value instanceof Float) {
            obj.put(key, ((Float) value).doubleValue());
        } else if (value instanceof Boolean) {
            obj.put(key, value);
        } else {
            obj.put(key, value.toString());
        }
    }

    private static boolean isAgentInfoExists(String packageName) {
        File infoFile = new File(AGENT_WORK_DIR,
                InspectionAgent.DESCRIPTOR_PREFIX + packageName + ".info");
        return infoFile.exists();
    }

    private static String getSocketName(String packageName) {
        return InspectionAgent.DESCRIPTOR_PREFIX + packageName;
    }

    private static JSONObject sendRequest(String packageName, JSONObject request)
            throws Exception {
        try (LocalSocket socket = new LocalSocket()) {

            socket.connect(new LocalSocketAddress(
                    "\0" + getSocketName(packageName),
                    LocalSocketAddress.Namespace.ABSTRACT));

            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8));
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));

            writer.write(request.toString());
            writer.newLine();
            writer.flush();

            String responseJson = reader.readLine();
            if (responseJson == null || responseJson.isEmpty()) {
                throw new AgentDeadException(packageName);
            }

            return new JSONObject(responseJson);

        }
    }

    private static Map<String, Object> jsonObjectToMap(JSONObject json) throws JSONException {
        Map<String, Object> map = new java.util.LinkedHashMap<>();
        java.util.Iterator<String> keys = json.keys();
        while (keys.hasNext()) {
            String key = keys.next();
            Object val = json.opt(key);
            if (val == null || val == JSONObject.NULL) {
                map.put(key, null);
            } else if (val instanceof JSONObject) {
                map.put(key, jsonObjectToMap((JSONObject) val));
            } else if (val instanceof org.json.JSONArray) {
                map.put(key, jsonArrayToList((org.json.JSONArray) val));
            } else {
                map.put(key, val);
            }
        }
        return map;
    }

    private static List<Object> jsonArrayToList(org.json.JSONArray arr) throws JSONException {
        List<Object> list = new java.util.ArrayList<>(arr.length());
        for (int i = 0; i < arr.length(); i++) {
            Object item = arr.get(i);
            if (item == null || item == JSONObject.NULL) {
                list.add(null);
            } else if (item instanceof JSONObject) {
                list.add(jsonObjectToMap((JSONObject) item));
            } else if (item instanceof org.json.JSONArray) {
                list.add(jsonArrayToList((org.json.JSONArray) item));
            } else {
                list.add(item);
            }
        }
        return list;
    }

    public record AgentInfo(String packageName, long startTime, String version) {
    }

    /** Agent 在线状态（用于 listAllAgents 返回） */
    public record AgentStatus(
            String packageName,
            long startTime,
            String version,
            boolean online,
            String error
    ) {
    }

    public static class AgentNotFoundException extends Exception {
        public AgentNotFoundException(String pkg) {
            super("目标应用 " + pkg + " 未注册 InspectionAgent。" +
                    "可能原因: 1) 应用未运行 2) 应用未被 Xposed 注入 Agent" +
                    " 3) 请确认应用已重启（Xposed Hook 需要应用重新启动才生效）");
        }
    }

    public static class AgentDeadException extends Exception {
        public AgentDeadException(String pkg) {
            super("目标应用 " + pkg + " 的 InspectionAgent 无响应（应用可能已崩溃）");
        }
    }

    public static class AgentCommandFailedException extends Exception {
        private final String errorCode;

        public AgentCommandFailedException(String code, String msg) {
            super(msg);
            this.errorCode = code;
        }

        public String getErrorCode() { return errorCode; }
    }
}
