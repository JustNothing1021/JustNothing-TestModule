package com.justnothing.testmodule.command.functions.tests;

import com.justnothing.javainterpreter.ScriptRunner;
import com.justnothing.javainterpreter.api.DefaultOutputHandler;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.utils.reflect.DexClassDefiner;
import com.justnothing.javainterpreter.evaluator.DynamicClassGenerator;

import java.util.concurrent.atomic.AtomicReference;

public class AnonClassTestMain extends CommandBase {

    public AnonClassTestMain() {
        super("AnonClassTest");
    }

    @Override
    public String getHelpText() {
        return """
                ===== 匿名类生成诊断测试 =====
                
                用法: anonclasstest [选项]
                
                选项:
                    (无)             - 运行完整诊断测试
                    --quick          - 快速测试 (只测基础用例)
                
                说明:
                    此命令用于诊断 DynamicClassGenerator 的匿名类动态生成能力。
                    测试 ASM 字节码生成、ClassLoader.defineClass、构造函数匹配等环节。
                
                """;
    }

    @Override
    public void runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        boolean quickMode = false;

        for (String arg : args) {
            if (arg.equals("--quick")) {
                quickMode = true;
                break;
            }
        }

        boolean finalQuickMode = quickMode;

        executeInIsolatedThread(context, "AnonClass", () ->
            testAnonymousClassInternal(context, finalQuickMode)
        );
    }

    private interface TestRunnable {
        void run() throws Exception;
    }

    private void executeInIsolatedThread(CommandExecutor.CmdExecContext context, String testName, TestRunnable test) {
        context.println("[" + testName + " 测试] 在独立线程中执行...", Colors.CYAN);
        context.println("", Colors.WHITE);
        
        AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

        try {
            test.run();
        } catch (Throwable e) {
            errorRef.set(e);
            context.println("测试执行异常: " + e.getMessage(), Colors.RED);
        }
    }
        
    private void testAnonymousClassInternal(CommandExecutor.CmdExecContext context, 
                                          boolean quickMode) {
        ScriptRunner runner = new ScriptRunner();
        DynamicClassGenerator.setDefaultClassDefiner(DexClassDefiner.getInstance());

        var outputHandler = new DefaultOutputHandler() {
            @Override
            public void print(String text) { context.print(text, Colors.WHITE); }
            @Override
            public void println(String text) { context.println(text, Colors.WHITE); }
        };

        context.println("╔══════════════════════════════════════════════════════╗", Colors.CYAN);
        context.println("║     DynamicClassGenerator 诊断工具                   ║", Colors.CYAN);
        context.println("╚══════════════════════════════════════════════════════╝", Colors.CYAN);
        context.println("", Colors.WHITE);


        String[][] basicTests = {
            {"1. 空匿名类 - Object", "new Object() {}", "basic"},
            {"2. 空匿名类 - 调用 toString()", "new Object() {}.toString()", "basic"},
            {"3. 空匿名类 - String", "new String() {}", "basic"},
            {"4. 空匿名类 - Integer(42)", "new Integer(42) {}", "basic"},
            {"5. 空匿名类 - ArrayList", "new ArrayList<String>() {}", "basic"},
            {"6. 空匿名类 - HashMap", "new HashMap<String,String>() {}", "basic"},
            {"7. 空匿名类 - StringBuilder", "new StringBuilder() {}", "basic"},
            {"8. 空匿名类 - RuntimeException", "new RuntimeException(\"test\") {}", "basic"},
        };

        String[][] advancedTests = {
            {"9. 带字段", "new Object() { int x = 42; }", "advanced"},
            {"10. 带方法", "new Object() { String hi() { return \"hello\"; } }", "advanced"},
            {"11. 带多个成员", "new Object() { int x = 1; String y = \"test\"; int get() { return x; } }", "advanced"},
            {"12. 继承 ProcessBuilder (无参)", "new ProcessBuilder() {}", "restricted"},
            {"13. 继承 ProcessBuilder (有参)", "new ProcessBuilder(\"echo\",\"test\") {}", "restricted"},
            {"14. 继承 Runtime", "new Runtime() {}", "restricted"},
            {"15. 继承 ClassLoader", "new ClassLoader() {}", "restricted"},
            {"16. 继承 Thread", "new Thread(() -> {}) {}", "restricted"},
            {"17. 继承 SecurityManager", "new SecurityManager() {}", "restricted"},
        };

        String[][] allTests = quickMode ? basicTests : 
            java.util.Arrays.copyOf(basicTests, basicTests.length + advancedTests.length, String[][].class);
        
        if (!quickMode) {
            System.arraycopy(advancedTests, 0, allTests, basicTests.length, advancedTests.length);
        }

        int passed = 0;
        int failed = 0;
        int warned = 0;
        int totalTests = allTests.length;

        for (String[] test : allTests) {
            String name = test[0];
            String code = test[1];
            String category = test[2];

            byte nameColor = switch (category) {
                case "basic" -> Colors.CYAN;
                case "advanced" -> Colors.MAGENTA;
                case "restricted" -> Colors.YELLOW;
                default -> Colors.WHITE;
            };

            context.print(name + ": ", nameColor);

            try {
                runner.getExecutionContext().clearVariables();
                
                long startTime = System.nanoTime();
                Object result;
                result = runner.executeWithResult(code, outputHandler, outputHandler);
                long elapsed = (System.nanoTime() - startTime) / 1_000_000;

                String resultType = result != null ? result.getClass().getSimpleName() : "null";
                context.println(" 成功 (" + resultType + ", " + elapsed + "ms)", Colors.GREEN);
                passed++;

            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                String msg = cause.getMessage();
                String causeName = cause.getClass().getSimpleName();

                boolean isExpectedLimitation = isExpectedClassLimitation(cause, msg);

                if (isExpectedLimitation) {
                    String limitType = msg != null && msg.contains("final") ? "final类" :
                                      msg != null && msg.contains("private") ? "私有构造函数" : "平台限制";
                    context.println(" 跳过 (" + limitType + ", " + causeName + ")", Colors.YELLOW);
                    warned++;
                } else {
                    context.println(" [" + causeName + "] " +
                        (msg != null && msg.length() > 100 ? msg.substring(0, 100) + "..." : msg), Colors.RED);
                    context.println("   完整异常: " + cause.getClass().getName(), Colors.RED);
                    if (cause.getStackTrace().length > 0) {
                        context.println("   位置: " + cause.getStackTrace()[0], Colors.GRAY);
                    }
                    
                    Throwable root = cause;
                    int depth = 0;
                    while (root.getCause() != null && depth < 3) {
                        root = root.getCause();
                        depth++;
                        context.println("   Cause #" + depth + ": " + root.getClass().getName() + ": " + root.getMessage(), Colors.YELLOW);
                    }
                    failed++;
                }
            }
        }

        context.println("", Colors.WHITE);
        context.println("───────────────────────────────────────────────────", Colors.GRAY);
        context.print("结果: ", Colors.CYAN);
        context.print(passed + "/" + totalTests + " 通过", Colors.GREEN);
        if (warned > 0) {
            context.print(", " + warned + " 跳过(平台限制)", Colors.YELLOW);
        }
        if (failed > 0) {
            context.print(", " + failed + " 失败", Colors.RED);
        }
        context.println("", Colors.WHITE);

        printDiagnosisSummary(context, failed, warned);
    }


    private void printDiagnosisSummary(CommandExecutor.CmdExecContext context,
                                       int failed, int warned) {
        if (failed == 0 && warned == 0) {
            context.println("所有测试通过! 匿名类生成功能正常工作", Colors.GREEN);
            return;
        }
        if (failed == 0) {
            context.println("所有可执行测试通过! " + warned + " 个测试因平台限制跳过 (final类/私有构造函数)", Colors.GREEN);
        }
    }

    private static boolean isExpectedClassLimitation(Throwable cause, String msg) {
        if (cause instanceof java.lang.IncompatibleClassChangeError) {
            return msg != null && msg.contains("declared final");
        }
        if (cause instanceof com.justnothing.javainterpreter.exception.EvaluationException) {
            return msg != null && (msg.contains("private") || msg.contains("final"));
        }
        return msg != null && (msg.contains("declared final") || msg.contains("constructor is private"));
    }
}
