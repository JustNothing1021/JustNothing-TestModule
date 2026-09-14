package com.justnothing.testmodule.command.functions.agent.inspect;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.command.framework.utils.GsonFactory;
import com.justnothing.testmodule.command.framework.protocol.InteractiveProtocol;
import com.justnothing.testmodule.command.framework.protocol.ProtocolMethods;
import com.justnothing.testmodule.command.framework.protocol.TerminalRpcChannel;
import com.justnothing.testmodule.command.functions.agent.response.DbListResult;
import com.justnothing.testmodule.command.functions.agent.response.DbQueryResult;
import com.justnothing.testmodule.command.functions.agent.response.DbTablesResult;
import com.justnothing.testmodule.command.functions.agent.response.SpListResult;
import com.justnothing.testmodule.command.functions.agent.response.SpReadResult;
import com.justnothing.testmodule.command.functions.agent.response.SpWriteResult;
import com.justnothing.testmodule.hooks.agent.InspectionAgentHook;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.io.ShellExecutorProvider;
import com.justnothing.testmodule.utils.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
     * 与普通 executeWithResult() 不同，此方法不会在收到首个 JSON 响应后关闭连接，
     * 而是切换到 InteractiveProtocol 二进制帧模式进行双向通信。
     * 帧处理逻辑完全对齐 {@link com.justnothing.methodsclient.executor.SocketStreamReader}。
     * <p>
     * 流程：
     * 1. 连接目标 Agent 的 LocalSocket
     * 2. 发送 _dispatch JSON 请求
     * 3. 读取 ACK 响应（确认切换到交互模式）
     * 4. 进入二进制帧循环（与 SocketStreamReader.runInteractiveMainLoop 一致的逻辑）
     *
     * @param packageName  目标应用包名
     * @param command      要执行的命令字符串
     * @param requirements 最终客户端的能力（宽高 / ANSI / 色彩）。目标进程没有自己的终端，
     *                     它要靠这个才能渲染出正常内容 —— 不传的话它拿到的是 0x0 + 不支持 ANSI，
     *                     渲染结果就是一片空白。
     * @param termRelay    目标进程发来的终端渲染消息的中继出口。目标进程的 RPC 对端是<b>我们</b>，
     *                     不是用户的终端；不转出去的话高亮/进度条就停在这一层了。可为 null。
     * @param callback     交互回调（用于输出显示 + 输入读取）
     */
    public static void executeInteractive(String packageName, String command,
                                          ClientRequirements requirements,
                                          TerminalRelay termRelay,
                                          InteractiveDispatchCallback callback) throws Exception {
        if (!isAgentInfoExists(packageName)) throw new AgentNotFoundException(packageName);

        try (LocalSocket socket = new LocalSocket()) {
            socket.connect(new LocalSocketAddress(
                    "\0" + getSocketName(packageName),
                    LocalSocketAddress.Namespace.ABSTRACT));

            OutputStream socketOut = socket.getOutputStream();
            InputStream socketIn = socket.getInputStream();

            // Step 1: 发送 _dispatch JSON 请求
            JSONObject request = new JSONObject();
            request.put("command", "_dispatch");
            JSONObject params = new JSONObject();
            params.put("command", command);
            if (requirements != null) {
                // 用现成的 toRpcParams 序列化成字符串再放进来：这里用的是 org.json，
                // 而它返回的是 Gson 的 JsonObject，两边字段类型不同没法直接嵌套。
                // 多一层字符串包裹换来"字段列表只有一处定义"，值得。
                params.put("client", ClientRequirements.toRpcParamsJson(requirements));
            }
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

            // Step 3: 进入统一 RPC 通道交互模式（TYPE_RPC 单帧 + 逻辑多通道 cmd.* / term.* / sys.*）
            // 帧处理完全对齐 SocketStreamReader.readRpcStream
            callback.onSessionStart(command);

            AtomicBoolean running = new AtomicBoolean(true);
            Object writeLock = new Object();

            TerminalRpcChannel rpcChannel = new TerminalRpcChannel(socketOut, writeLock, InteractiveProtocol.TYPE_RPC);

            // cmd.output → 输出回调（带颜色 → onColoredOutput，纯文本 → onOutput）
            rpcChannel.onNotification(ProtocolMethods.CMD_OUTPUT, p -> {
                if (p == null) return;
                // 服务端会把连续输出合并成一帧的 segments；单段仍是旧的 {data,color} 形式。
                if (p.has("segments") && p.get("segments").isJsonArray()) {
                    for (JsonElement element : p.getAsJsonArray("segments")) {
                        JsonObject segment = element.getAsJsonObject();
                        String segText = segment.has("data") && !segment.get("data").isJsonNull()
                                ? segment.get("data").getAsString() : null;
                        if (segText == null || segText.isEmpty()) continue;
                        if (segment.has("color") && segment.get("color").getAsByte() != Colors.DEFAULT) {
                            callback.onColoredOutput(segText, segment.get("color").getAsByte());
                        } else {
                            callback.onOutput(segText);
                        }
                    }
                    return;
                }
                String data = p.has("data") && !p.get("data").isJsonNull()
                        ? p.get("data").getAsString() : null;
                if (data == null || data.isEmpty()) return;
                if (p.has("color") && p.get("color").getAsByte() != Colors.DEFAULT) {
                    callback.onColoredOutput(data, p.get("color").getAsByte());
                } else {
                    callback.onOutput(data);
                }
            });

            // cmd.prompt → 输入回调（异步处理，不占 reader 循环，确保能继续响应 sys.ping 保活）
            // callback.onInputRequest() 内部走 context.readLine() → 主服务 cmd.prompt → 终端客户端
            rpcChannel.onRequest(ProtocolMethods.CMD_PROMPT, (id, p) -> {
                ThreadPoolManager.submitFastRunnable(() -> {
                    JsonObject response = new JsonObject();
                    try {
                        String type = p != null && p.has("type") ? p.get("type").getAsString() : "input";
                        String title = p != null && p.has("title") && !p.get("title").isJsonNull()
                                ? p.get("title").getAsString() : "";
                        boolean isPassword = "password".equals(type);
                        // 复用旧协议 PASSWORD: 前缀约定，便于回调侧区分密码输入
                        String userInput = callback.onInputRequest(isPassword ? "PASSWORD:" + title : title);
                        if (userInput != null) {
                            response.addProperty("value", userInput);
                        } else {
                            response.addProperty("cancelled", true);
                        }
                    } catch (Exception e) {
                        logger.error("处理 cmd.prompt 出错: " + id, e);
                        response.addProperty("cancelled", true);
                    }
                    rpcChannel.sendResponse(id, response);
                });
            });

            // cmd.done → 命令结束，退出循环
            rpcChannel.onNotification(ProtocolMethods.CMD_DONE, p -> {
                logger.debug("收到 cmd.done，命令执行完成");
                running.set(false);
            });

            // sys.ping → sys.pong（RPC 层活性）
            rpcChannel.onNotification(ProtocolMethods.SYS_PING,
                    p -> rpcChannel.sendNotification(ProtocolMethods.SYS_PONG, null));

            // === 终端渲染中继 ===
            // 目标进程的 RPC 对端是"我们"，不是用户的终端 —— 它的 RichConsole 渲染结果会发到这儿。
            // 必须继续往客户端送，否则高亮 / 进度条就停在这一层了（这正是之前"内容消失"的原因之一）。
            //
            // 只中继 服务端→客户端 方向的两条：
            //   term.output    渲染输出（高亮代码、进度条重绘）
            //   term.highlight 高亮片段
            // 刻意【不】中继 term.enterRawMode / exitRawMode：那会让客户端进入 raw mode，
            // 而 raw mode 下的按键要靠 term.input 回传，这条链路我们没打通 —— 结果是终端
            // 卡在 raw mode。好在 ANSI 光标控制序列在正常模式下一样有效，进度条不受影响。
            relayTerm(rpcChannel, termRelay, ProtocolMethods.TERM_OUTPUT);
            relayTerm(rpcChannel, termRelay, ProtocolMethods.TERM_HIGHLIGHT);

            // term.querySize：目标进程在问"你那边终端多大"。我们就是它的对端，
            // 而且手里已经有最终客户端的能力，直接答 —— 不用再往客户端问一轮。
            rpcChannel.onRequest(ProtocolMethods.TERM_QUERY_SIZE, (id, p) -> {
                JsonObject size = new JsonObject();
                size.addProperty("width", requirements != null ? requirements.getWidth() : 0);
                size.addProperty("height", requirements != null ? requirements.getHeight() : 0);
                rpcChannel.sendResponse(id, size);
            });

            // 保活线程：只要会话未结束就一直周期性发 sys.ping（服务端回 sys.pong 维持活性），
            // 与 SocketStreamReader.startPingThread 的停止条件对齐。
            // 注意：停止条件不能包含 lastResponseTime 新鲜度——空闲期（用户在看输出 / 等下一次 prompt）
            // 连接其实完全健康，此时若停掉保活，会让依赖"最近有 ping/pong 活跃"的判定在 30s 后误判超时。
            // 停止只应发生在会话结束（running 置 false）或读取循环退出后。
            ScheduledFuture<?> pingFuture = null;
            try {
                pingFuture = ThreadPoolManager.scheduleWithFixedDelayUntil(
                        () -> rpcChannel.sendNotification(ProtocolMethods.SYS_PING, null),
                        0, 5000, TimeUnit.MILLISECONDS,
                        () -> !running.get() || Thread.currentThread().isInterrupted()
                );
            } catch (Exception e) {
                logger.warn("启动 PING 线程失败（非致命）: " + e.getMessage());
            }

            try {
                while (running.get() && !Thread.currentThread().isInterrupted()) {
                    Object[] packet = InteractiveProtocol.readMessage(socketIn);
                    if (packet == null) {
                        logger.debug("Agent 交互连接关闭");
                        break;
                    }

                    byte frameType = (byte) packet[0];
                    byte[] frameData = (byte[]) packet[1];

                    if (frameType == InteractiveProtocol.TYPE_RPC) {
                        rpcChannel.handleMessage(frameData);
                    } else {
                        logger.debug("Agent 交互收到未知帧类型: " +
                                InteractiveProtocol.getMessageTypeName(frameType));
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
     * 目标进程终端消息的中继出口。
     *
     * <p>目标进程渲染出来的东西是发给<b>我们</b>的（我们是它的 RPC 对端），
     * 要真正显示到用户眼前，还得由调用方转给最终客户端的通道。</p>
     */
    public interface TerminalRelay {
        void onTermNotification(String method, JsonObject params);
    }

    /** 把某条 {@code term.*} 通知转交出去；调用方没接中继时静默丢弃（等于维持"渲染不可达"）。 */
    private static void relayTerm(TerminalRpcChannel channel, TerminalRelay relay, String method) {
        channel.onNotification(method, params -> {
            if (relay != null) {
                relay.onTermNotification(method, params);
            }
        });
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
            super("目标应用 " + pkg + " 未注册 InspectionAgent, " +
                    "可能原因: 应用未运行或者应用未被 Xposed 注入 Agent;" +
                    " 请确认应用已重启（Xposed Hook 需要应用重新启动才生效）");
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
