package com.justnothing.testmodule.command.agent;

import android.content.Context;
import android.net.LocalServerSocket;
import android.net.LocalSocket;

import com.justnothing.testmodule.utils.logging.Logger;
import com.justnothing.methodsclient.executor.AsyncChmodExecutor;

import org.json.JSONArray;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

                AgentCommandHandler<?> handler = AgentCommandRouter.getHandler(commandType);
                if (handler == null) {
                    writer.write(buildError(-1, "UNKNOWN_COMMAND",
                            "未知命令: " + commandType + ", 可用: " + AgentCommandRouter.getAvailableCommands()));
                    writer.newLine();
                    writer.flush();
                    return;
                }

                Object result = handler.handle(params, applicationContext);

                JSONObject response = new JSONObject();
                response.put("returnCode", 0);
                response.put("data", serializeResult(result));
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

    private Object serializeResult(Object result) throws JSONException {
        if (result == null) return JSONObject.NULL;
        if (result instanceof JSONObject) return result;
        if (result instanceof JSONArray) return result;
        if (result instanceof Map) return new JSONObject((Map<?, ?>) result);
        if (result instanceof List) return new JSONArray((List<?>) result);
        if (result instanceof Set) return new JSONArray(Collections.singletonList(result));
        if (result instanceof String[]) return new JSONArray(result);
        if (result instanceof int[]) {
            JSONArray arr = new JSONArray();
            for (int v : (int[]) result) arr.put(v);
            return arr;
        }
        if (result instanceof boolean[]) {
            JSONArray arr = new JSONArray();
            for (boolean v : (boolean[]) result) arr.put(v);
            return arr;
        }
        return result;
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
}
