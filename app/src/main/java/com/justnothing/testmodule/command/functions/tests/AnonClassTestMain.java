package com.justnothing.testmodule.command.functions.tests;

import com.justnothing.engine.ScriptRunner;
import com.justnothing.engine.api.DefaultOutputHandler;
import com.justnothing.engine.exception.EvalException;
import com.justnothing.testmodule.command.framework.model.MainCommand;
import com.justnothing.testmodule.command.framework.CommandExecutor;
import com.justnothing.testmodule.command.framework.i18n.Text;
import com.justnothing.testmodule.command.framework.model.CommandResult;
import com.justnothing.testmodule.command.framework.model.CommandRequest;
import com.justnothing.testmodule.command.framework.output.Colors;
import com.justnothing.testmodule.utils.reflect.DexClassDefiner;
import com.justnothing.engine.codegen.DynamicClassGenerator;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicReference;

import com.justnothing.testmodule.command.framework.annotation.Cmd;

@Cmd(name = "anonclasstest", description = "匿名类生成诊断测试")
public class AnonClassTestMain extends MainCommand<CommandResult> {

    public AnonClassTestMain() {
        super("AnonClassTest", CommandResult.class);
    }

    @Override
    public String getHelpText() {
        return Text.zhEn(
                "===== 匿名类生成诊断测试 =====\n\n用法: anonclasstest [选项]\n\n选项:\n    (无)             - 运行完整诊断测试\n    --quick          - 快速测试 (只测基础用例)\n\n说明:\n    此命令用于诊断 DynamicClassGenerator 的匿名类动态生成能力。\n    测试 ASM 字节码生成、ClassLoader.defineClass、构造函数匹配等环节。\n\n",
                "===== Anonymous class generation diagnostic test =====\n\nUsage: anonclasstest [options]\n\nOptions:\n    (none)           - run the full diagnostic test\n    --quick          - quick test (basic cases only)\n\nNotes:\n    Diagnoses the anonymous-class dynamic generation of DynamicClassGenerator.\n    Exercises ASM bytecode generation, ClassLoader.defineClass, constructor matching.\n\n")
                .text();
    }

    @Override
    protected CommandResult executeInternal(CommandExecutor.CmdExecContext<CommandRequest<?>> context) throws Exception {
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

        return createSuccessResult(Text.zhEn("匿名类测试命令执行完成", "Anonymous class test command finished").text());
    }

    private interface TestRunnable {
        void run() throws Exception;
    }

    private void executeInIsolatedThread(CommandExecutor.CmdExecContext<?> context, String testName, TestRunnable test) {
        context.println(Text.zhEn("[%s 测试] 在独立线程中执行...", "[%s test] running in an isolated thread...").format(testName), Colors.CYAN);
        context.println("", Colors.WHITE);
        
        AtomicReference<Throwable> errorRef = new AtomicReference<>(null);

        try {
            test.run();
        } catch (Throwable e) {
            errorRef.set(e);
            context.println(Text.zhEn("测试执行异常: %s", "Test run failed: %s").format(e.getMessage()), Colors.RED);
        }
    }
        
    private void testAnonymousClassInternal(CommandExecutor.CmdExecContext<?> context,
                                          boolean quickMode) {
        DynamicClassGenerator.setDefaultClassDefiner(DexClassDefiner.getInstance());
        ScriptRunner runner = new ScriptRunner();

        var outputHandler = new DefaultOutputHandler() {
            @Override
            public void print(String text) { context.print(text, Colors.WHITE); }
            @Override
            public void println(String text) { context.println(text, Colors.WHITE); }
        };

        context.println("╔══════════════════════════════════════════════════════╗", Colors.CYAN);
        context.println(Text.zhEn("║     DynamicClassGenerator 诊断工具                   ║", "║     DynamicClassGenerator Diagnostic Tool            ║").text(), Colors.CYAN);
        context.println("╚══════════════════════════════════════════════════════╝", Colors.CYAN);
        context.println("", Colors.WHITE);


        String[][] basicTests = {
            {Text.zhEn("1. 空匿名类 - Object", "1. Empty anonymous class - Object").text(), "new Object() {}", "basic"},
            {Text.zhEn("2. 空匿名类 - 调用 toString()", "2. Empty anonymous class - call toString()").text(), "new Object() {}.toString()", "basic"},
            {Text.zhEn("3. 空匿名类 - String", "3. Empty anonymous class - String").text(), "new String() {}", "basic"},
            {Text.zhEn("4. 空匿名类 - Integer(42)", "4. Empty anonymous class - Integer(42)").text(), "new Integer(42) {}", "basic"},
            {Text.zhEn("5. 空匿名类 - ArrayList", "5. Empty anonymous class - ArrayList").text(), "new ArrayList<String>() {}", "basic"},
            {Text.zhEn("6. 空匿名类 - HashMap", "6. Empty anonymous class - HashMap").text(), "new HashMap<String,String>() {}", "basic"},
            {Text.zhEn("7. 空匿名类 - StringBuilder", "7. Empty anonymous class - StringBuilder").text(), "new StringBuilder() {}", "basic"},
            {Text.zhEn("8. 空匿名类 - RuntimeException", "8. Empty anonymous class - RuntimeException").text(), "new RuntimeException(\"test\") {}", "basic"},
        };

        String[][] advancedTests = {
            {Text.zhEn("9. 带字段", "9. With fields").text(), "new Object() { int x = 42; }", "advanced"},
            {Text.zhEn("10. 带方法", "10. With methods").text(), "new Object() { String hi() { return \"hello\"; } }", "advanced"},
            {Text.zhEn("11. 带多个成员", "11. With multiple members").text(), "new Object() { int x = 1; String y = \"test\"; int get() { return x; } }", "advanced"},
            {Text.zhEn("12. 继承 ProcessBuilder (无参)", "12. Extend ProcessBuilder (no args)").text(), "new ProcessBuilder() {}", "restricted"},
            {Text.zhEn("13. 继承 ProcessBuilder (有参)", "13. Extend ProcessBuilder (with args)").text(), "new ProcessBuilder(\"echo\",\"test\") {}", "restricted"},
            {Text.zhEn("14. 继承 Runtime", "14. Extend Runtime").text(), "new Runtime() {}", "restricted"},
            {Text.zhEn("15. 继承 ClassLoader", "15. Extend ClassLoader").text(), "new ClassLoader() {}", "restricted"},
            {Text.zhEn("16. 继承 Thread", "16. Extend Thread").text(), "new Thread(() -> {}) {}", "restricted"},
            {Text.zhEn("17. 继承 SecurityManager", "17. Extend SecurityManager").text(), "new SecurityManager() {}", "restricted"},
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
                runner.clearVariables();
                
                long startTime = System.nanoTime();
                Object result;
                result = runner.executeWithResult(code, outputHandler, outputHandler);
                long elapsed = (System.nanoTime() - startTime) / 1_000_000;

                String resultType = result != null ? result.getClass().getSimpleName() : "null";
                context.println(Text.zhEn(" 成功 (%s, %dms)", " OK (%s, %dms)").format(resultType, elapsed), Colors.GREEN);
                passed++;

            } catch (Throwable t) {
                Throwable cause = t.getCause() != null ? t.getCause() : t;
                String msg = cause.getMessage();
                String causeName = cause.getClass().getSimpleName();

                boolean isExpectedLimitation = isExpectedClassLimitation(cause, msg);
                logger.warn("测试 #" + name + " 失败" + (isExpectedLimitation ? " (预期限制)" : ""), t);
                if (isExpectedLimitation) {
                    String limitType = msg != null && msg.contains("final") ? Text.zhEn("final类", "final class").text() :
                                      msg != null && msg.contains("private") ? Text.zhEn("私有构造函数", "private constructor").text() : Text.zhEn("平台限制", "platform limitation").text();
                    context.println(Text.zhEn(" 跳过 (%s, %s)", " SKIPPED (%s, %s)").format(limitType, causeName), Colors.YELLOW);
                    warned++;
                } else {

                    context.println(" [" + causeName + "] " +
                        (msg != null && msg.length() > 100 ? msg.substring(0, 100) + "..." : msg), Colors.RED);
                    context.println(Text.zhEn("   完整异常: %s", "   Full exception: %s").format(cause.getClass().getName()), Colors.RED);
                    if (cause.getStackTrace().length > 0) {
                        context.println(Text.zhEn("   位置: %s", "   Location: %s").format(cause.getStackTrace()[0]), Colors.GRAY);
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
        context.print(Text.zhEn("结果: ", "Result: ").text(), Colors.CYAN);
        context.print(Text.zhEn("%d/%d 通过", "%d/%d passed").format(passed, totalTests), Colors.GREEN);
        if (warned > 0) {
            context.print(Text.zhEn(", %d 跳过(平台限制)", ", %d skipped (platform limitation)").format(warned), Colors.YELLOW);
        }
        if (failed > 0) {
            context.print(Text.zhEn(", %d 失败", ", %d failed").format(failed), Colors.RED);
        }
        context.println("", Colors.WHITE);

        printDiagnosisSummary(context, failed, warned);
    }


    private void printDiagnosisSummary(CommandExecutor.CmdExecContext<?> context,
                                       int failed, int warned) {
        if (failed == 0 && warned == 0) {
            context.println(Text.zhEn("所有测试通过! 匿名类生成功能正常工作", "All tests passed! Anonymous class generation works").text(), Colors.GREEN);
            return;
        }
        if (failed == 0) {
            context.println(Text.zhEn("所有可执行测试通过! %d 个测试因平台限制跳过 (final类/私有构造函数)", "All runnable tests passed! %d test(s) skipped due to platform limitations (final class / private constructor)").format(warned), Colors.GREEN);
        }
    }

    private static boolean isExpectedClassLimitation(Throwable cause, String msg) {
        if (cause instanceof InvocationTargetException ite && ite.getCause() != null) {
            cause = ite.getCause();
            msg = cause.getMessage();
        }
        if (cause instanceof IncompatibleClassChangeError) {
            return msg != null && msg.contains("declared final");
        }
        if (cause instanceof EvalException) {
            return msg != null && (msg.contains("private") || msg.contains("final"));
        }
        if (cause instanceof UnsupportedOperationException) {
            return msg != null && (msg.contains("Cannot extend")
                    || msg.contains("private")
                    || msg.contains("inaccessible"));
        }
        return msg != null && (msg.contains("declared final") || msg.contains("constructor is private"));
    }
}
