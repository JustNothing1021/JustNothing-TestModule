package com.justnothing.testmodule.command.framework.protocol;

import com.justnothing.testmodule.utils.logging.Logger;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 客户端远程终端 handler。
 *
 * <p>接收服务端通过 TerminalRpcChannel 发来的 Terminal RPC 消息，
 * 委托给本地 JLine Terminal 执行实际操作。</p>
 */
public class RemoteClientTerminal {

    private static final Logger logger = Logger.getLoggerForName("RemoteClientTerminal");

    private final Terminal localTerminal;
    private final TerminalRpcChannel rpcChannel;
    private final AtomicBoolean inRawMode = new AtomicBoolean(false);
    private volatile Attributes savedAttributes;
    private volatile Thread keyForwardThread;

    public RemoteClientTerminal(Terminal localTerminal, TerminalRpcChannel rpcChannel) {
        this.localTerminal = localTerminal;
        this.rpcChannel = rpcChannel;
        registerHandlers();
    }

    private void registerHandlers() {
        // "output" → 本地终端显示
        rpcChannel.onNotification(ProtocolMethods.TERM_OUTPUT, params -> {
            if (params != null && params.has("data")) {
                String data = params.get("data").getAsString();
                localTerminal.writer().print(data);
                localTerminal.writer().flush();
            }
        });

        // "enterRawMode" → 本地进入 raw mode + 启动按键转发
        rpcChannel.onNotification(ProtocolMethods.TERM_ENTER_RAW_MODE, params -> {
            if (inRawMode.compareAndSet(false, true)) {
                try {
                    savedAttributes = localTerminal.getAttributes();
                    localTerminal.enterRawMode();
                    startKeyForwarding();
                    logger.debug("已进入 raw mode，启动按键转发");
                } catch (Exception e) {
                    logger.error("进入 raw mode 失败", e);
                    inRawMode.set(false);
                }
            }
        });

        // "exitRawMode" → 恢复 Attributes + 停止按键转发
        rpcChannel.onNotification(ProtocolMethods.TERM_EXIT_RAW_MODE, params -> {
            if (inRawMode.compareAndSet(true, false)) {
                try {
                    stopKeyForwarding();
                    if (savedAttributes != null) {
                        localTerminal.setAttributes(savedAttributes);
                        savedAttributes = null;
                    }
                    logger.debug("已退出 raw mode，停止按键转发");
                } catch (Exception e) {
                    logger.error("退出 raw mode 失败", e);
                }
            }
        });

        // "querySize" 请求 → 返回本地终端尺寸
        rpcChannel.onRequest(ProtocolMethods.TERM_QUERY_SIZE, (id, params) -> {
            Size size = localTerminal.getSize();
            JsonObject result = new JsonObject();
            result.addProperty("width", size.getColumns());
            result.addProperty("height", size.getRows());
            rpcChannel.sendResponse(id, result);
        });

    }

    /**
     * 启动按键转发线程：从本地 Terminal 读取按键，通过 RPC 发送给服务端
     */
    private void startKeyForwarding() {
        keyForwardThread = new Thread(() -> {
            try {
                var reader = localTerminal.reader();
                while (inRawMode.get() && !Thread.currentThread().isInterrupted()) {
                    int c = reader.read(100); // 100ms timeout
                    if (c == -1) break;
                    if (c == -2) continue; // timeout, just retry

                    // 发送 "input" 通知
                    JsonObject params = new JsonObject();
                    JsonArray bytes = new JsonArray();
                    // 处理多字节字符
                    if (c <= 0x7F) {
                        bytes.add(c);
                    } else {
                        String ch = String.valueOf((char) c);
                        byte[] encoded = ch.getBytes(StandardCharsets.UTF_8);
                        for (byte b : encoded) {
                            bytes.add(b & 0xFF);
                        }
                    }
                    params.add("bytes", bytes);
                    rpcChannel.sendNotification(ProtocolMethods.TERM_INPUT, params);
                }
            } catch (Exception e) {
                if (inRawMode.get()) {
                    logger.error("按键转发线程异常", e);
                }
            }
        }, "key-forward");
        keyForwardThread.setDaemon(true);
        keyForwardThread.start();
    }

    /**
     * 停止按键转发线程
     */
    private void stopKeyForwarding() {
        if (keyForwardThread != null) {
            keyForwardThread.interrupt();
            keyForwardThread = null;
        }
    }
}
