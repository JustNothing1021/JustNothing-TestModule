package com.justnothing.testmodule.command.functions.beanshell;

import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.TargetError;
import com.justnothing.testmodule.utils.functions.Logger;

import java.util.HashMap;
import java.util.Map;

// 这个全是AI写的
public class BeanShellExecutor {

    public static class BeanShellLogger extends Logger {
        @Override public String getTag() { return "BeanShellExecutor"; }
    }

    public static final BeanShellLogger logger = new BeanShellLogger();

    private final Interpreter interpreter;
    private final Map<String, Object> persistentVariables = new HashMap<>();

    public BeanShellExecutor(ClassLoader classLoader) {
        this.interpreter = new Interpreter();
        this.interpreter.setClassLoader(classLoader);
        setupDefaultBindings();
    }

    private void setupDefaultBindings() {
        try {
            interpreter.eval("import java.util.*;");
            interpreter.eval("import java.lang.*;");
            interpreter.eval("import android.util.*;");
            interpreter.eval("""
                void println(Object obj) {
                    if (obj == null) {
                        System.out.println("null");
                    } else {
                        System.out.println(obj.toString());
                    }
                }
            
                void print(Object obj) {
                    if (obj == null) {
                        System.out.print("null");
                    } else {
                        System.out.print(obj.toString());
                    }
                }
            """);

            // 恢复持久化变量
            for (Map.Entry<String, Object> entry : persistentVariables.entrySet()) {
                try {
                    interpreter.set(entry.getKey(), entry.getValue());
                } catch (EvalError e) {
                    logger.warn("无法恢复变量 " + entry.getKey(), e);
                }
            }

        } catch (EvalError e) {
            logger.error("初始化BeanShell失败", e);
        }
    }

    public String execute(String code, Map<String, Object> context) {
        StringBuilder result = new StringBuilder();

        try {
            // 设置上下文变量
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                try {
                    interpreter.set(entry.getKey(), entry.getValue());
                } catch (EvalError e) {
                    logger.warn("无法设置变量 " + entry.getKey(), e);
                }
            }

            // 执行代码 - 使用无类型变量声明（BeanShell风格）
            Object evalResult = interpreter.eval(code);

            // 获取执行结果
            if (evalResult != null) {
                result.append("Result: ").append(evalResult)
                        .append("\nType: ").append(evalResult.getClass().getName());
            } else {
                result.append("Result: null (void)");
            }

            // 收集所有定义的变量
            NameSpace namespace = interpreter.getNameSpace();
            String[] variableNames = namespace.getVariableNames();

            if (variableNames.length > 0) {
                result.append("\n\nVariables defined:\n");
                for (String varName : variableNames) {
                    if (!varName.startsWith("bsh.")) {
                        try {
                            Object value = interpreter.get(varName);
                            String typeName = value != null ? value.getClass().getSimpleName() : "null";
                            result.append("  ").append(varName)
                                    .append(" = ").append(value)
                                    .append(" (").append(typeName).append(")\n");

                            // 保存到持久化变量
                            persistentVariables.put(varName, value);
                            // 更新上下文
                            context.put(varName, value);
                        } catch (EvalError e) {
                            // 忽略无法访问的变量
                        }
                    }
                }
            }

        } catch (TargetError e) {
            // 这是代码执行中的异常
            Throwable target = e.getTarget();
            result.append("Execution Error: ").append(target.toString())
                    .append("\nAt line: ").append(e.getErrorLineNumber())
                    .append("\nError text: ").append(e.getErrorText())
                    .append("\nStack Trace:\n");

            for (StackTraceElement ste : target.getStackTrace()) {
                if (ste.getClassName().startsWith("bsh.")) {
                    result.append("  [BeanShell] ").append(ste.getMethodName()).append("\n");
                } else {
                    result.append("  ").append(ste).append("\n");
                }
            }

            logger.error("BeanShell执行错误", e);

        } catch (EvalError e) {
            result.append("Evaluation Error: ").append(e.getMessage())
                    .append("\nAt line: ").append(e.getErrorLineNumber())
                    .append("\nError text: ").append(e.getErrorText());
            logger.error("BeanShell解析错误", e);
        }

        return result.toString();
    }

    public Map<String, Object> getVariables() {
        return new HashMap<>(persistentVariables);
    }

    public void clearVariables() {
        persistentVariables.clear();
        try {
            // 重置解释器
            interpreter.getNameSpace().clear();
            setupDefaultBindings();
        } catch (Exception e) {
            logger.error("清除变量失败", e);
        }
    }
}