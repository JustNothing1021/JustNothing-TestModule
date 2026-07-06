package com.justnothing.testmodule.command.framework.output;

import com.justnothing.testmodule.command.framework.protocol.TerminalRpcChannel;
import com.justnothing.testmodule.utils.logging.Logger;
import com.google.gson.JsonObject;
import org.jline.terminal.Attributes;
import org.jline.terminal.Size;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.ExternalTerminal;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 服务端远程终端，基于 TerminalRpcChannel 的 JSON-RPC 通道。
 *
 * <p>继承 ExternalTerminal 获得完整的行规范（line discipline）处理，
 * 同时通过 RPC 通道发送 raw mode 切换信号给客户端。</p>
 */
public class RemoteServerTerminal extends ExternalTerminal {

    private static final Logger logger = Logger.getLoggerForName("RemoteServerTerminal");

    private final TerminalRpcChannel rpcChannel;
    private final AtomicReference<Size> cachedSize = new AtomicReference<>();
    private volatile boolean inRawMode = false;

    public RemoteServerTerminal(TerminalRpcChannel rpcChannel,
                                String terminalType,
                                int initialWidth,
                                int initialHeight) throws Exception {
        super(
            null,                          // provider
            "remote-socket",               // name
            terminalType,                  // type (e.g., "xterm-256color")
            rpcChannel.createInputStream(),  // masterInput (Lambda InputStream)
            rpcChannel.createOutputStream(), // masterOutput (Lambda OutputStream)
            StandardCharsets.UTF_8,        // encoding
            Terminal.SignalHandler.SIG_IGN, // signalHandler
            false                          // paused=false, start MasterReader immediately
        );
        this.rpcChannel = rpcChannel;

        // 设置初始尺寸
        if (initialWidth > 0 && initialHeight > 0) {
            Size size = new Size(initialWidth, initialHeight);
            setSize(size);
            cachedSize.set(size);
        }

        // 注册 RPC handlers
        registerRpcHandlers();
    }

    private void registerRpcHandlers() {
        // 处理 "sizeUpdate" 通知（客户端主动推送尺寸变更）
        rpcChannel.onNotification("sizeUpdate", params -> {
            if (params != null) {
                int w = params.has("width") ? params.get("width").getAsInt() : 0;
                int h = params.has("height") ? params.get("height").getAsInt() : 0;
                if (w > 0 && h > 0) {
                    Size size = new Size(w, h);
                    setSize(size);
                    cachedSize.set(size);
                    logger.debug("终端尺寸更新: " + w + "x" + h);
                }
            }
        });

        // 处理 "querySize" 的响应（由 TerminalRpcChannel.call() 自动处理）
        // 不需要额外注册，call() 返回时已处理
    }

    @Override
    public Attributes enterRawMode() {
        // 先通知客户端进入 raw mode
        inRawMode = true;
        rpcChannel.sendNotification("enterRawMode", null);
        logger.debug("已发送 enterRawMode 信号");
        // 然后本地也切换（ExternalTerminal 的 line discipline）
        return super.enterRawMode();
    }

    @Override
    public void setAttributes(Attributes attr) {
        // 仅当从 raw mode 恢复到正常模式（ICANON/ECHO 重新开启）时通知客户端
        // 注意：enterRawMode() 内部也会调 setAttributes(rawAttrs)，
        // 此时 inRawMode=true 但 attr 是 raw 属性（ICANON=false），不能发 exitRawMode
        if (inRawMode) {
            boolean restoringToNormal = attr.getLocalFlag(Attributes.LocalFlag.ICANON)
                    || attr.getLocalFlag(Attributes.LocalFlag.ECHO);
            if (restoringToNormal) {
                inRawMode = false;
                rpcChannel.sendNotification("exitRawMode", null);
                logger.debug("已发送 exitRawMode 信号");
            }
        }
        super.setAttributes(attr);
    }

    /**
     * 通过 RPC 主动查询客户端终端尺寸
     *
     * @param timeoutMs 超时毫秒
     * @return 查询到的尺寸，超时返回 null
     */
    public Size queryRemoteSize(long timeoutMs) {
        JsonObject result = rpcChannel.call("querySize", null, timeoutMs);
        if (result != null && result.has("width") && result.has("height")) {
            int w = result.get("width").getAsInt();
            int h = result.get("height").getAsInt();
            if (w > 0 && h > 0) {
                Size size = new Size(w, h);
                setSize(size);
                cachedSize.set(size);
                return size;
            }
        }
        return null;
    }

    /**
     * 更新缓存的终端尺寸（由 InteractiveOutputHandler 调用）
     */
    public void updateCachedSize(int width, int height) {
        if (width > 0 && height > 0) {
            Size size = new Size(width, height);
            setSize(size);
            cachedSize.set(size);
        }
    }

    public TerminalRpcChannel getRpcChannel() {
        return rpcChannel;
    }

    /**
     * 设置输出流的 autoFlush 模式
     */
    public void setAutoFlushEveryWrite(boolean auto) {
        TerminalRpcChannel.RpcOutputStream out = rpcChannel.getRpcOutputStream();
        if (out != null) {
            out.setAutoFlushEveryWrite(auto);
        }
    }
}
