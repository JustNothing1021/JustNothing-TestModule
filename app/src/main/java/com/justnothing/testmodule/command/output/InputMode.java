package com.justnothing.testmodule.command.output;

/**
 * 输入模式常量定义。
 * <p>
 * 每个模式代表客户端输入行为的一套完整配置（高亮器 + 补全器 + TailTip）。
 * 服务端通过 {@link ICommandOutputHandler#switchInputMode(String)} 通知客户端切换模式，
 * 客户端根据此常量名查找对应的三件套配置并应用。
 *
 * <h3>预定义模式</h3>
 * <table>
 *   <tr><th>常量</th><th>用途</th></tr>
 *   <tr>{@code COMMAND}<td>命令输入模式（默认）：命令语法高亮 + 命令补全 + 命令 TailTip</td></tr>
 *   <tr>{@code JAVA}<td>Java 代码编辑模式：Java 语法高亮 + Java 类名补全</td></tr>
 *   <tr>{@code SCRIPT}<td>脚本编辑模式：脚本语言高亮 + 脚本内置函数补全</td></tr>
 *   <tr>{@code NONE}<td>无高亮/补全：纯文本模式，最低开销</td></tr>
 * </table>
 *
 * <h3>扩展方式</h3>
 * <pre>{@code
 * // 注册自定义模式
 * InputMode.register("python",
 *     "PythonHighlighter", "PythonCompleter", "PythonTailTips");
 *
 * // 使用
 * outputHandler.switchInputMode(InputMode.PYTHON);
 * }</pre>
 */
public final class InputMode {

    private InputMode() {} // 纯常量类，禁止实例化

    // ==================== 预定义模式名称 ====================

    /** 命令输入模式（默认）— 命令语法高亮 + 命令补全 + 命令提示 */
    public static final String COMMAND = "command";

    /** Java 代码模式 — Java 语法高亮 + JDK 类名补全 */
    public static final String JAVA = "java";

    /** 脚本编辑模式 — 脚本语言高亮 + 脚本内置补全 */
    public static final String SCRIPT = "script";

    /** 无高亮/补全 — 纯文本模式 */
    public static final String NONE = "none";

    // ==================== 模式描述（供帮助/日志使用） ====================

    /**
     * 获取模式的可读描述
     */
    public static String getDescription(String mode) {
        return switch (mode) {
            case COMMAND -> "命令输入模式";
            case JAVA -> "Java 代码编辑模式";
            case SCRIPT -> "脚本编辑模式";
            case NONE -> "纯文本模式";
            default -> "自定义模式: " + mode;
        };
    }

    /**
     * 判断是否为已知的预定义模式
     */
    public static boolean isPredefined(String mode) {
        return switch (mode) {
            case COMMAND, JAVA, SCRIPT, NONE -> true;
            default -> false;
        };
    }

    // ==================== 所有预定义模式名称列表 ====================

    /** 返回所有预定义的模式名称 */
    public static String[] allModes() {
        return new String[] { COMMAND, JAVA, SCRIPT, NONE };
    }
}
