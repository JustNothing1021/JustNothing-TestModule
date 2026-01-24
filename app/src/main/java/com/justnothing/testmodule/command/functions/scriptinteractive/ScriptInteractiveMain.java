package com.justnothing.testmodule.command.functions.scriptinteractive;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SCRIPT_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.functions.script.TestInterpreter;

import java.util.concurrent.ConcurrentHashMap;

public class ScriptInteractiveMain extends CommandBase {

    private static final ConcurrentHashMap<ClassLoader, TestInterpreter.ScriptRunner>
            scriptRunners = new ConcurrentHashMap<>();
    private static final TestInterpreter.ScriptRunner systemScriptRunner = new TestInterpreter.ScriptRunner(null);

    public ScriptInteractiveMain() {
        super("ScriptInteractive");
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: script_interactive
                
                进入交互式脚本执行模式，支持JustNothing1021写的解释器语法.
                
                支持的内置函数:
                    print(String s)                      - 将字符串输出到缓冲区
                    println(String s)                    - 将字符串输出到缓冲区并且换行
                    range(int end)
                    range(int begin, int end)
                    range(int begin, int end, int step)
                    analyze(Object obj)   -> null             - 分析一个对象
                    getContext()          -> Context          - 获取上下文
                    getApplicationInfo()  -> ApplicationInfo  - 获取应用信息
                    getPackageName()      -> String           - 获取包名
                    createSafeExecutor()  -> Object()       - 创建线程安全的执行器
                    asRunnable(Lambda lambda)                  - 防止Lambda不兼容
                    asFunction(Lambda lambda)                  - 同上
                    runLater(Lambda lambda)                    - 把Lambda放新线程跑
                
                退出命令:
                    exit, quit                         - 退出交互式模式
                
                示例:
                    script_interactive
                
                (Submodule script_interactive %s)
                """, CMD_SCRIPT_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        TestInterpreter.ScriptRunner runner = getScriptExecutor(classLoader);
        
        getLogger().info("进入交互式脚本执行模式");
        context.output().println("====== 脚本交互执行模式 ===");
        context.output().println("输入 'exit' 或 'quit' 退出");
        context.output().println("");

        while (true) {
            String code = context.readLine(">>> ");
            
            if (code == null) {
                context.output().println("");
                break;
            }
            
            code = code.trim();
            
            if (code.equals("exit") || code.equals("quit")) {
                context.output().println("Say goodbye~~~");
                break;
            }
            
            if (code.isEmpty()) {
                continue;
            }
            
            try {
                Object result = runner.executeWithResult(code);
                if (result != null) {
                    context.output().println(result.toString());
                }
            } catch (Exception e) {
                context.output().println("执行出错: " + e.getMessage());
                getLogger().error("脚本执行失败", e);
            }
        }
        
        return "交互式模式已退出";
    }

    private TestInterpreter.ScriptRunner getScriptExecutor(ClassLoader cl) {
        if (cl == null) return systemScriptRunner;
        return scriptRunners.computeIfAbsent(cl, TestInterpreter.ScriptRunner::new);
    }
}
