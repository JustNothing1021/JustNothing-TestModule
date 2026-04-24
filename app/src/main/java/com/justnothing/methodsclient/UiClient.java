package com.justnothing.methodsclient;

import com.justnothing.methodsclient.executor.SocketCommandExecutor;
import com.justnothing.testmodule.utils.logging.Logger;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * UI客户端通信层。
 * 
 * <p>提供在Android应用内部与服务端通信的API。
 * 使用JSON协议进行结构化数据传输。</p>
 */
public class UiClient {
    
    private static final Logger logger = Logger.getLoggerForName("UiClient");
    
    private static volatile UiClient instance;
    
    private static final int DEFAULT_PORT = 12345;
    private static final int CONNECT_TIMEOUT_MS = 5000;
    
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private volatile int port = DEFAULT_PORT;
    private volatile boolean serverAvailable = false;
    
    private UiClient() {}
    
    public static UiClient getInstance() {
        if (instance == null) {
            synchronized (UiClient.class) {
                if (instance == null) {
                    instance = new UiClient();
                }
            }
        }
        return instance;
    }
    
    /**
     * 检查服务端是否可用。
     * 
     * @return 如果服务端可用返回true
     */
    public boolean isServerAvailable() {
        return checkServer();
    }
    
    /**
     * 检查服务端状态。
     * 
     * @return 如果服务端可用返回true
     */
    public boolean checkServer() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", port), CONNECT_TIMEOUT_MS);
            serverAvailable = true;
            return true;
        } catch (Exception e) {
            serverAvailable = false;
            logger.debug("服务端不可用: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 设置服务端端口。
     * 
     * @param port 端口号
     */
    public void setPort(int port) {
        this.port = port;
    }
    
    /**
     * 获取当前端口。
     * 
     * @return 端口号
     */
    public int getPort() {
        return port;
    }
    
    /**
     * 执行命令请求并返回结果。
     * 
     * @param requestJson 请求JSON字符串
     * @return 响应JSON字符串
     */
    public String executeCommandRequest(String requestJson) {
        try {
            logger.debug("执行命令请求: " + requestJson);
            SocketCommandExecutor executor = new SocketCommandExecutor();
            String response = executor.executeCommandRequest(requestJson);
            logger.debug("收到响应: " + response);
            return response;
        } catch (Exception e) {
            logger.error("执行命令请求失败", e);
            return "{\"success\":false,\"error\":{\"code\":\"EXECUTION_ERROR\",\"message\":\"执行失败: " + e.getMessage() + "\"}}";
        }
    }
    
    /**
     * 关闭客户端。
     */
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
