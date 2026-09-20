package com.justnothing.testmodule.command.framework.protocol;

/**
 * 统一 RPC 信封的 method 常量（逻辑多通道）。
 *
 * <p>所有 JSON-RPC 消息承载于单一 {@link InteractiveProtocol#TYPE_RPC} 帧，
 * 通过 method 名区分逻辑通道，详见代码。
 */
public final class ProtocolMethods {

    private ProtocolMethods() {}

    /** JSON-RPC 协议版本（所有信封共用）。 */
    public static final String JSONRPC_VERSION = "2.0";

    // ─── 命令域 (C→S / S→C) ────────────────────────────────

    /** C→S request：执行命令。params = {command?: String, request?: String(JSON), requestId?: String} */
    public static final String CMD_EXECUTE = "cmd.executeWithResult";

    /** S→C notification：命令输出。params = {data: String, color?: byte} */
    public static final String CMD_OUTPUT = "cmd.output";

    /** S→C request → C→S response：交互输入。params = {type: input|password|confirm|list|checkbox, title, defaultValue, options} */
    public static final String CMD_PROMPT = "cmd.prompt";

    /** S→C notification：命令结束。params = {result?: String(CommandResult JSON), success?: boolean, error?: String} */
    public static final String CMD_DONE = "cmd.done";

    // ─── 终端域 (S→C / C→S) ────────────────────────────────

    /** S→C notification：终端输出。params = {data: String} */
    public static final String TERM_OUTPUT = "term.output";

    /** C→S notification：终端按键输入。params = {bytes: int[]} */
    public static final String TERM_INPUT = "term.input";

    /** S→C notification：进入 raw mode（无 params） */
    public static final String TERM_ENTER_RAW_MODE = "term.enterRawMode";

    /** S→C notification：退出 raw mode（无 params） */
    public static final String TERM_EXIT_RAW_MODE = "term.exitRawMode";

    /** S→C request：查询终端尺寸（响应 = {width, height}） */
    public static final String TERM_QUERY_SIZE = "term.querySize";

    /** C→S notification：终端尺寸变更。params = {width, height} */
    public static final String TERM_SIZE_UPDATE = "term.sizeUpdate";

    /** S→C notification：高亮模式切换。params = {mode: String} */
    public static final String TERM_HIGHLIGHT = "term.highlight";

    // ─── 系统域 (双向) ─────────────────────────────────────

    /** C→S request：客户端握手。params = {supportsInput, isJsonMode, width, height, supportsAnsi, colorSystem} */
    public static final String SYS_HELLO = "sys.hello";

    /** 双向 notification：RPC 层活性探测 */
    public static final String SYS_PING = "sys.ping";

    /** 双向 notification：RPC 层活性响应 */
    public static final String SYS_PONG = "sys.pong";
}
