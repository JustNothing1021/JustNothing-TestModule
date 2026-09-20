package com.justnothing.testmodule.test;

import com.google.gson.JsonObject;
import com.justnothing.testmodule.command.framework.output.ClientRequirements;
import com.justnothing.testmodule.command.framework.protocol.InteractiveProtocol;
import com.justnothing.testmodule.command.framework.protocol.ProtocolMethods;
import com.justnothing.testmodule.command.framework.protocol.TerminalRpcChannel;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.testmodule.utils.io.LocalShellExecutor;
import com.justnothing.testmodule.utils.io.ShellExecutor;
import com.justnothing.testmodule.utils.io.ShellExecutorProvider;
import com.justnothing.testmodule.utils.io.ShellExecutionException;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * 测试服务器 - 在电脑上运行，模拟服务端行为。
 * <p>
 * 使用统一 RPC 信封协议（{@link InteractiveProtocol#TYPE_RPC} 单帧 + 逻辑多通道 cmd.* / sys.*），
 * 流程：sys.hello 握手 → cmd.executeWithResult → cmd.output + cmd.done。
 * </p>
 *
 * <h3>启动方式</h3>
 * <pre>
 *   // 作为 JUnit 测试运行（默认端口 12345）
 *   ./gradlew test --tests "*TestServer*"
 *
 *   // 或在 IDE 中直接运行 {@link #main(String[])}
 * </pre>
 */
public class TestServer {

    private static final int DEFAULT_PORT = 12345;

    private final int port;
    private final ShellExecutor executor;
    private volatile boolean running = false;
    private volatile ServerSocket serverSocket;

    /** 实际绑定的端口；传 0 时由系统分配，绑定后通过 {@link #getPort()} 获取 */
    private volatile int boundPort = -1;
    /** 绑定完成（或绑定失败）后放行，供测试等待服务器就绪 */
    private final CountDownLatch started = new CountDownLatch(1);

    public TestServer() {
        this(DEFAULT_PORT);
    }

    public TestServer(int port) {
        this.port = port;
        // 强制使用本地执行器，不依赖 root
        this.executor = new LocalShellExecutor();
        ShellExecutorProvider.setForcedExecutor(this.executor);
    }

    /** 返回实际绑定的端口（传 0 时为系统分配的空闲端口） */
    public int getPort() {
        return boundPort;
    }

    /**
     * 等待服务器完成端口绑定，避免测试在绑定前就去连接。
     *
     * @return true 表示已在超时前完成绑定
     */
    public boolean awaitStarted(long timeoutMs) throws InterruptedException {
        return started.await(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * 启动服务器（阻塞）。
     */
    public void start() throws IOException {
        running = true;
        serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        try {
            serverSocket.bind(new java.net.InetSocketAddress(port));
        } catch (IOException e) {
            // 绑定失败也要放行等待者，否则测试会一直等到超时
            started.countDown();
            throw e;
        }
        boundPort = serverSocket.getLocalPort();
        started.countDown();

        System.out.println("[TestServer] 已启动，监听端口: " + boundPort);
        System.out.println("[TestServer] 按 Ctrl+C 停止");
        System.out.println("[TestServer] RPC 客户端测试: ReplClient / StreamClient");
        System.out.println();

        try {
            while (running) {
                try {
                    Socket client = serverSocket.accept();
                    System.out.println("[TestServer] 收到客户端连接: " + client.getInetAddress());
                    new Thread(() -> handleClient(client), "TestServer-client").start();
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[TestServer] 接受连接失败: " + e.getMessage());
                    }
                }
            }
        } finally {
            closeQuietly(serverSocket);
        }
    }

    /**
     * 停止服务器。
     */
    public void stop() {
        running = false;
        closeQuietly(serverSocket);
    }

    private static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    private void handleClient(Socket client) {
        try (client;
             InputStream input = client.getInputStream();
             OutputStream output = client.getOutputStream()) {

            Object[] firstPacket = InteractiveProtocol.readMessage(input);
            if (firstPacket == null) {
                System.out.println("[TestServer] 客户端未发首帧（可能只是端口探测），关闭");
                return;
            }

            byte firstType = (byte) firstPacket[0];
            if (firstType != InteractiveProtocol.TYPE_RPC) {
                System.err.println("[TestServer] 首帧非 TYPE_RPC（旧协议已废弃），拒绝: "
                        + InteractiveProtocol.getMessageTypeName(firstType));
                return;
            }

            System.out.println("[TestServer] 使用统一 RPC 协议");
            handleRpcClient(input, output, (byte[]) firstPacket[1]);

        } catch (Exception e) {
            System.err.println("[TestServer] 客户端处理异常: " + e.getMessage());
        }
    }

    /**
     * 统一 RPC 客户端处理：sys.hello 握手 → cmd.executeWithResult → cmd.output + cmd.done。
     */
    private void handleRpcClient(InputStream input, OutputStream output, byte[] firstData) throws Exception {
        Object writeLock = new Object();
        TerminalRpcChannel channel = new TerminalRpcChannel(output, writeLock, InteractiveProtocol.TYPE_RPC);

        // sys.hello → 握手响应
        channel.onRequest(ProtocolMethods.SYS_HELLO, (id, params) -> {
            ClientRequirements requirements = ClientRequirements.fromRpcParams(params);
            System.out.println("[TestServer] 客户端能力: " + requirements);
            JsonObject resp = new JsonObject();
            channel.sendResponse(id, resp);
        });

        // sys.ping → sys.pong（RPC 层活性）
        channel.onNotification(ProtocolMethods.SYS_PING,
                p -> channel.sendNotification(ProtocolMethods.SYS_PONG, null));

        // cmd.executeWithResult → 回执 ack，异步执行（不占 reader 循环）
        channel.onRequest(ProtocolMethods.CMD_EXECUTE, (id, params) -> {
            String command = params != null && params.has("command") && !params.get("command").isJsonNull()
                    ? params.get("command").getAsString() : "";
            System.out.println("[TestServer] 收到命令: " + command);
            channel.sendResponse(id, new JsonObject());
            new Thread(() -> {
                try {
                    String result = executeCommand(command);
                    JsonObject out = new JsonObject();
                    out.addProperty("data", result + "\n");
                    channel.sendNotification(ProtocolMethods.CMD_OUTPUT, out);
                } finally {
                    channel.sendNotification(ProtocolMethods.CMD_DONE, new JsonObject());
                }
            }, "TestServer-exec").start();
        });

        // 同步处理首帧（应为 sys.hello）
        channel.handleMessage(firstData);

        // 读循环：仅 TYPE_RPC 帧
        while (running) {
            Object[] packet = InteractiveProtocol.readMessage(input);
            if (packet == null) {
                System.out.println("[TestServer] 客户端关闭连接");
                break;
            }
            byte type = (byte) packet[0];
            byte[] data = (byte[]) packet[1];
            if (type == InteractiveProtocol.TYPE_RPC) {
                channel.handleMessage(data);
            } else {
                System.err.println("[TestServer] 收到未知帧类型: " + InteractiveProtocol.getMessageTypeName(type));
                break;
            }
        }
    }

    private String executeCommand(String command) {
        try {
            if (command.startsWith("echo ")) {
                // 模拟 echo
                return command.substring(5);
            } else if (command.startsWith("ls")) {
                // 模拟 ls
                return "file1.txt\nfile2.txt\ndir1";
            } else if (command.startsWith("pwd")) {
                return System.getProperty("user.dir");
            } else if (command.startsWith("help")) {
                return "支持的命令: echo, ls, pwd, help, ping";
            } else {
                // 尝试本地执行
                IOManager.ProcessResult result = executor.execute(command, 5000);
                if (result.isSuccess()) {
                    return result.stdout() != null ? result.stdout() : "(空输出)";
                } else {
                    return "执行失败 (退出码 " + result.exitCode() + "): " +
                            (result.stderr() != null ? result.stderr() : "未知错误");
                }
            }
        } catch (ShellExecutionException e) {
            return "Shell 执行异常: " + e.getMessage();
        }
    }

    // ==================== 启动入口 ====================

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
        new TestServer(port).start();
    }
}
