package com.justnothing.testmodule.test;

import com.google.gson.JsonObject;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.protocol.InteractiveProtocol;
import com.justnothing.testmodule.command.framework.protocol.ProtocolMethods;
import com.justnothing.testmodule.command.framework.protocol.RpcChannel;
import com.justnothing.testmodule.utils.io.LocalShellExecutor;
import com.justnothing.testmodule.utils.io.ShellExecutorProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * TestServer 集成测试 - 验证统一 RPC 协议。
 * <p>
 * 启动 TestServer 在随机端口，用 RpcChannel 客户端连接，
 * 经 sys.hello 握手 + cmd.executeWithResult 执行命令，验证 cmd.output / cmd.done 响应。
 * </p>
 */
public class TestServerTest {

    private TestServer server;
    private Thread serverThread;
    private volatile Exception serverError;

    @Before
    public void setUp() throws Exception {
        ShellExecutorProvider.setForcedExecutor(new LocalShellExecutor());
        serverError = null;

        // 端口传 0：由系统分配空闲端口，避免固定端口在测试间复用导致
        // 上一个 server 未完全退出时新 server 抢不到端口（TIME_WAIT / 绑定失败）
        server = new TestServer(0);
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (Exception e) {
                serverError = e;
            }
        });
        serverThread.setDaemon(true);
        serverThread.start();

        // 等待服务器完成端口绑定，避免在就绪前连接
        assertTrue("服务器应在 5 秒内完成端口绑定", server.awaitStarted(5000));
        assertNull("服务器启动错误: " + (serverError != null ? serverError.getMessage() : ""), serverError);
    }

    @After
    public void tearDown() {
        ShellExecutorProvider.setForcedExecutor(null);
        if (server != null) {
            server.stop();
        }
        if (serverThread != null) {
            serverThread.interrupt();
            try {
                // 等 accept 线程真正退出并释放端口，避免影响下一个测试
                serverThread.join(3000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // ==================== RPC 协议（统一信封）测试 ====================

    /**
     * 走统一 RPC：sys.hello 握手 → cmd.executeWithResult → 收 cmd.output 直到 cmd.done。
     */
    private String runRpcCommand(String command) throws Exception {
        try (Socket socket = new Socket("localhost", server.getPort());
             OutputStream out = socket.getOutputStream();
             InputStream in = socket.getInputStream()) {

            Object writeLock = new Object();
            RpcChannel channel = new RpcChannel(out, writeLock, InteractiveProtocol.TYPE_RPC);
            StringBuilder output = new StringBuilder();
            AtomicBoolean done = new AtomicBoolean(false);

            channel.onNotification(ProtocolMethods.CMD_OUTPUT, p -> {
                if (p != null && p.has("data") && !p.get("data").isJsonNull()) {
                    output.append(p.get("data").getAsString());
                }
            });
            channel.onNotification(ProtocolMethods.CMD_DONE, p -> done.set(true));

            // sys.hello 握手（能力协商）
            ClientRequirements req = new ClientRequirements(false, false);
            req.setWidth(80);
            req.setHeight(24);
            req.setSupportsAnsi(true);
            req.setColorSystem(ClientRequirements.COLOR_TRUECOLOR);
            channel.sendRequest(ProtocolMethods.SYS_HELLO, ClientRequirements.toRpcParams(req));

            // cmd.executeWithResult
            JsonObject params = new JsonObject();
            params.addProperty("command", command);
            channel.sendRequest(ProtocolMethods.CMD_EXECUTE, params);

            // 读循环直到 cmd.done（5s 超时兜底）
            long deadline = System.currentTimeMillis() + 5000;
            while (!done.get() && System.currentTimeMillis() < deadline) {
                Object[] packet = InteractiveProtocol.readMessage(in);
                if (packet == null) break;
                if ((byte) packet[0] == InteractiveProtocol.TYPE_RPC) {
                    channel.handleMessage((byte[]) packet[1]);
                }
            }
            return output.toString();
        }
    }

    @Test
    public void testRpcEcho() throws Exception {
        assertNull("服务器启动错误: " + (serverError != null ? serverError.getMessage() : ""), serverError);
        String response = runRpcCommand("echo hello from client");
        assertTrue("RPC 响应应包含命令内容", response.contains("hello from client"));
    }

    @Test
    public void testRpcLs() throws Exception {
        assertNull("服务器启动错误", serverError);
        String response = runRpcCommand("ls");
        assertTrue("ls 响应应包含文件列表", response.contains("file1.txt") || response.contains("dir1"));
    }

    @Test
    public void testRpcPwd() throws Exception {
        assertNull("服务器启动错误", serverError);
        String response = runRpcCommand("pwd");
        assertTrue("pwd 响应应包含当前目录", response.contains(System.getProperty("user.dir")));
    }

    @Test
    public void testRpcHelp() throws Exception {
        assertNull("服务器启动错误", serverError);
        String response = runRpcCommand("help");
        assertTrue("help 响应应包含支持的命令列表", response.contains("支持的命令"));
    }

    @Test
    public void testRpcUnknownCommand() throws Exception {
        assertNull("服务器启动错误", serverError);
        String response = runRpcCommand("nonexistent_command_xyz");
        assertTrue("未知命令应返回错误信息", response.contains("执行失败"));
    }
}
