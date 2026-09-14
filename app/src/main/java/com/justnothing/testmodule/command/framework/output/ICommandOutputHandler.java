package com.justnothing.testmodule.command.framework.output;

import com.justnothing.engine.api.IOutputHandler;
import com.justnothing.richconsole.console.Console;
import com.justnothing.testmodule.command.framework.model.CommandResult;


/**
 * 交互式输入输出接口
 * 支持同步输出到多个目标，同时支持输入请求
 */
public interface ICommandOutputHandler extends IOutputHandler {
    default String readLineFromClient(String prompt) {
        throw new RuntimeException(getClass().getName() + " 并不支持readLineFromClient...");
    }
    default String readPasswordFromClient(String prompt) {
        throw new RuntimeException(getClass().getName() + " 并不支持readPasswordFromClient...");
    }
    
    @Override
    default String readLine(String prompt) {
        return readLineFromClient(prompt);
    }
    @Override
    default String readPassword(String prompt) {
        return readPasswordFromClient(prompt);
    }

    /**
     * 通知客户端切换输入模式。
     * <p>
     * 模式切换会影响客户端的三个组件：
     * <ul>
     *   <li><b>Highlighter</b> — 语法高亮器（如 Java 高亮 / 命令高亮 / 无高亮）</li>
     *   <li><b>Completer</b> — Tab 补全器（如命令补全 / Java 类名补全）</li>
     *   <li><b>TailTip</b> — 参数提示（如命令参数提示 / Java 关键字提示）</li>
     * </ul>
     *
     * <p>预定义模式名见 {@link InputMode}：
     * <ul>
     *   <li>{@link InputMode#COMMAND} — 命令输入模式（默认）</li>
     *   <li>{@link InputMode#JAVA} — Java 代码编辑模式</li>
     *   <li>{@link InputMode#SCRIPT} — 脚本编辑模式</li>
     *   <li>{@link InputMode#NONE} — 纯文本模式</li>
     * </ul>
     *
     * @param mode 模式名称（使用 {@link InputMode} 中的常量，或自定义注册的名称）
     */
    default void switchInputMode(String mode) {
        // 默认实现：不支持模式切换（非交互式 handler）
    }

    default void print(String text, byte color) {
        print(text);
    }
    
    default void println(String text, byte color) {
        println(text);
    }
    
    default void printf(byte color, String format, Object... args) {
        printf(format, args);
    }
    
    default void printSuccess(String text) {
        print(text);
    }
    
    default void printlnSuccess(String text) {
        println(text);
    }
    
    default void printWarning(String text) {
        print("[WARN] " + text);
    }
    
    default void printlnWarning(String text) {
        println("[WARN] " + text);
    }
    
    default void printInfo(String text) {
        print("[INFO] " + text);
    }
    
    default void printlnInfo(String text) {
        println("[INFO] " + text);
    }
    
    default void printDebug(String text) {
        print("[DEBUG] " + text);
    }
    
    default void printlnDebug(String text) {
        println("[DEBUG] " + text);
    }
    
    default void printStackTrace(Throwable t, byte color) {
        printStackTrace(t);
    }

    /**
     * 通知输出处理器：命令已执行完毕并产出结构化结果。
     *
     * <p>默认实现不处理（非交互式 handler）。{@link com.justnothing.testmodule.command.framework.output.InteractiveOutputHandler}
     * 会缓存该结果，并在 {@link #close()} 时随 cmd.done 通知一起发给客户端，
     * 实现"流式输出 (cmd.output) + 结尾结构化结果 (cmd.done)"的输出标准化。</p>
     *
     * @param result 命令执行的最终结果（可为 null）
     */
    default void finish(CommandResult result) {
        // 默认不处理
    }

    /**
     * 获取 RichConsole Console 实例（用于高级渲染：表格、面板、进度条等）。
     *
     * <p>仅 InteractiveOutputHandler 支持此方法，其他实现返回 null。</p>
     *
     * @return Console 实例，或 null（如果当前输出目标不支持 RichConsole 渲染）
     */
    default Console getConsole() {
        return null;
    }

    default boolean supportsRichRendering() {
        return false;
    }
}