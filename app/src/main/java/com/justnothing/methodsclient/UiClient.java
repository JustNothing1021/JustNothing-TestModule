package com.justnothing.methodsclient;

import com.justnothing.methodsclient.executor.SocketCommandExecutor;
import com.justnothing.methodsclient.metadata.CommandMetadataScanner;
import com.justnothing.testmodule.utils.logging.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * UI客户端通信层。
 * 
 * <p>提供在Android应用内部与服务端通信的API。
 * 使用JSON协议进行结构化数据传输。</p>
 */
public class UiClient {
    
    private static final Logger logger = Logger.getLoggerForName("UiClient");
    
    private static volatile UiClient instance;
    
    private static final int DEFAULT_PORT = 11451;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    
    private volatile int port = DEFAULT_PORT;

    private UiClient() {}
    
    public static UiClient getInstance() {
        if (instance == null) {
            synchronized (UiClient.class) {
                if (instance == null) {
                    // 先建好路由表：Request 的 commandType 由路由派生，
                    // 而 GUI 进程不会执行服务端的自动注册，必须在构造任何 Request 之前补上。
                    CommandMetadataScanner.ensureRegistered();
                    instance = new UiClient();
                }
            }
        }
        return instance;
    }
    
    public boolean isServerAvailable() {
        return checkServer();
    }
    
    public boolean checkServer() {
        port = StreamClient.getSocketPort();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), CONNECT_TIMEOUT_MS);
            return true;
        } catch (Exception e) {
            logger.debug("服务端不可用: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 执行命令请求并返回JSON结果。
     *
     * @param requestJson 请求JSON字符串
     * @return 响应JSON字符串
     */
    public String executeCommandRequest(String requestJson) {
        try {
            logger.debug("执行命令请求: " + requestJson);
            SocketCommandExecutor executor = new SocketCommandExecutor();
            SocketCommandExecutor.ExecutionResult result = executor.executeWithResult(requestJson, SocketCommandExecutor.Format.JSON);
            logger.debug("收到响应: " + result.output());
            return result.success() ? result.output() : "{\"success\":false,\"error\":{\"code\":\"EXECUTION_ERROR\",\"message\":\"" + escapeJson(result.error()) + "\"}}";
        } catch (Exception e) {
            logger.error("执行命令请求失败", e);
            return "{\"success\":false,\"error\":{\"code\":\"EXECUTION_ERROR\",\"message\":\"执行失败: " + escapeJson(e.getMessage()) + "\"}}";
        }
    }
    
    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}