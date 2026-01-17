package com.justnothing.testmodule.command.functions.beanshell;

import static com.justnothing.testmodule.constants.CommandServer.CMD_BEAN_SHELL_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BeanShellExecutorMain {

    private static final ConcurrentHashMap<ClassLoader, BeanShellExecutor>
            beanShellExecutors = new ConcurrentHashMap<>();
    private static final BeanShellExecutor systemBeanShellExecutor = new BeanShellExecutor(null);


    private static final Map<String, Object> beanShellExecutionContext = new HashMap<>();


    public static class BeanShellLogger extends Logger {
        @Override
        public String getTag() {
            return "BeanShellExecutor";
        }
    }

    public static final BeanShellLogger logger = new BeanShellLogger();

    public static String getHelpText() {
        return String.format("""
                语法: bsh <beanShell code>
                
                用BeanShell解释器执行代码.
                (说实话这个我也不会用, AI推荐给我的, 其实能算是废稿, 但留着好玩)
                
                示例:
                  bsh 'a = "114514";'
                  bsh 'println("Hello, World!");'
                  bsh 'for (i = 0; i < 5; i++) { println("i = " + i); }'
                
                提示: BeanShell不需要也不能指定类型
                    a = "114514";
                    String a = "114514"; // 爆炸
                
                (Submodule bsh %s)
                """, CMD_BEAN_SHELL_VER);
    }
    public static String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        if (args.length < 1) {
            return getHelpText();
        }

        try {
            StringBuilder code = new StringBuilder();
            for (int i = 0; i < args.length; i++) {
                code.append(args[i]);
                if (i < args.length - 1) {
                    code.append(" ");
                }
            }
            logger.info("执行BeanShell代码: " + code);
            String result = getBeanShellExecutor(classLoader).execute(code.toString(), beanShellExecutionContext);
            return "BeanShell执行器结果:\n\n" + result;

        } catch (Exception e) {
            return "BeanShell执行出错: " + e.getMessage() + "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    public static String listVariables(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();

        StringBuilder sb = new StringBuilder("(当前ClassLoader: " + (targetPackage == null ? "默认加载器" : targetPackage) + ")");

        sb.append("\n\nBeanShell执行器的变量列表:\n");
        Map<String, Object> bshVars = getBeanShellExecutor(classLoader).getVariables();
        if (bshVars.isEmpty()) {
            sb.append("  (空)\n");
        } else {
            for (Map.Entry<String, Object> entry : bshVars.entrySet()) {
                Object value = entry.getValue();
                sb.append("  ").append(entry.getKey())
                        .append(" = ").append(value)
                        .append(" (").append(value != null ? value.getClass().getSimpleName() : "null")
                        .append(")\n");
            }
        }

        return sb.toString();
    }

    public static String clearVariables(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        getBeanShellExecutor(classLoader).clearVariables();
        return "已清空BeanShell执行器的所有变量\n提示: 只清空了" +
                (targetPackage == null ? "默认" : targetPackage) + "的ClassLoader的执行器的变量，其他ClassLoader的并没有被清空";
    }

    private static BeanShellExecutor getBeanShellExecutor(ClassLoader cl) {
        if (cl == null) return systemBeanShellExecutor;
        return beanShellExecutors.computeIfAbsent(cl, BeanShellExecutor::new);
    }

}
