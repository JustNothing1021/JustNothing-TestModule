package com.justnothing.testmodule.command.functions.script;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SCRIPT_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.util.concurrent.ConcurrentHashMap;

public class ScriptExecutorMain extends CommandBase {


    private static final ConcurrentHashMap<ClassLoader, TestInterpreter.ScriptRunner>
            scriptRunners = new ConcurrentHashMap<>();
    private static final TestInterpreter.ScriptRunner systemScriptRunner = new TestInterpreter.ScriptRunner(null);

    private final String commandName;

    public ScriptExecutorMain() {
        this("script");
    }

    public ScriptExecutorMain(String commandName) {
        super("ScriptExecutor");
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        if (commandName.equals("sclear")) {
            return String.format("""
                    语法: sclear
                    
                    清空脚本执行器的所有变量.
                    
                    示例:
                        sclear
                    
                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
        } else if (commandName.equals("svars")) {
            return String.format("""
                    语法: svars
                    
                    显示脚本执行器的变量列表.
                    
                    示例:
                        svars
                    
                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
        } else {
            return String.format("""
                    
                    语法: script <java-like code>
                    
                    用JustNothing1021写的解释器跑Java代码.
                    就是目前还有一些语法不支持...习惯就好, 不会写AST(
                    
                    另外, 这玩意的性能很低, 不建议执行超过100次循环的代码
                    (是这样的, 以后会试着优化, 以我的技术力现在能用就已经是个奇迹了)
                    
                    
                    目前支持的内置函数:
                    
                        print(String s)                      - 将字符串输出到缓冲区, 最终会作为结果返回
                        println(String s)                    - 将字符串输出到缓冲区并且换行, 最终会作为结果返回
                        range(int end)
                        range(int begin, int end)
                        range(int begin, int end, int step)  - 都能写Java了, 相信你也会用Python的range(真的吗...?)
                    
                        analyze(Object obj)   -> null             - 分析一个对象, 打印出一些它的相关信息
                        getContext()          -> Context          - 获取上下文, DeepSeek神力
                        getApplicationInfo()  -> ApplicationInfo  - 也是DeepSeek写的
                        getPackageName()      -> String           - 和上面一样
                        createSafeExecutor()  -> Object() {       - 创建一个Android中线程安全的执行器, 防止出现诡异错误崩掉系统
                            Object runOnMainThread(Callable<Object> task);                     - 在主线程执行任务
                            Object runOnLooperThread(Callable<Object> task);                   - 在有Looper的新线程中执行任务
                            Object runWithLooper(Callable<Object> task);                       - 在当前线程用Lopper执行任务
                            Object createInstanceWithHandler(String className, Object... args) - 直接用有Looper的当前线程创建实例
                            (这几个方法都 throws Exception)
                        }
                        asRunnable(Lambda lambda)                  - 本来是为了防止Lambda不兼容的, 现在如果没必要其实可以不用了
                        asFunction(Lambda lambda)                  - 一样的
                        runLater(Lambda lambda)                    - 把Lambda放新线程跑
                    
                    
                    需要注意的几个点(语法点):
                    
                        (1) 支持auto力! (但是如果类型是auto一定要有初始值, 不然我咋推导类型)
                            e.g.
                             auto number = 114.5f; // number -> java.lang.Float
                             auto context = getContext(); // context -> android.app.Application
                             auto wtf; // boom
                    
                        (2) 用InitializerList(我也不知道这玩意中文是啥, 初始化列表?)初始化的数组类型可以指定默认长度
                            这样如果指定的初始列表短了它会补齐(我也不知道是用啥补齐的, 用的Array.newInstance, 所以还是不建议让系统自己补)
                            但是如果制定了就必须全部维度的都指定, 不然就推导不出类型了
                            e.g.
                             new int[3][3] {new int[]{1, 2, 3}, new int[]{4, 5, 6}, new int[]{7, 8, 9}}; // ok
                             new int[][] {new int[]{1, 2, 3}, new int[]{4, 5, 6}, new int[]{7, 8, 9}};   // ok
                             new int[3][] {new int[]{1, 2, 3}, new int[]{4, 5, 6}, new int[]{7, 8, 9}};  // boom
                    
                        (3) 模板类目前还没有很好的支持, 类型信息会在运行的时候被擦除(逝情不大, 能用)
                            e.g.
                             List<Integer> list = new ArrayList(); list.add(1); list.add("wtf"); // 甚至能跑还不会报错...
                    
                        (4) 由于Object的限制, 原始类(比如int, char之类的)会被解析成封装类
                            e.g.
                             int a = 33550336; // 实际上a是java.lang.Integer
                    
                        (5) 现在这个东西还在测试阶段, 看到报错不一定是你的代码问题, 大概率是我的解析器不支持这个语法/解析错误了...
                            e.g.
                             try { throw RuntimeException("HIHIHEHA"); } catch (Exception e) {} // boom
                    
                        (6) 已经自动导入了下面几个包, 不用谢我 (bushi)
                            addImport("java.util.*");
                            addImport("java.lang.*");
                            addImport("java.lang.reflect.*");
                            addImport("android.util.*");
                            addImport("android.os.*");
                            (甚至懒得把多余的抠掉)

                        (7) 可以用delete把变量删掉
                            e.g.
                             int a = 114514; 
                             delete a; // a没了

                        (8) 可以直接调用lambda表达式
                            e.g.
                             auto a = () -> { return 1145; }; // a -> TestInterpreter$LambdaNode$$Lambda$14
                             a.call(); // 1145
                             a(); // 1145
                    
                        (9) 不能很好地解析分号分割, 建议写代码的时候小心点, 谨防爆炸
                    
                    
                    示例:
                      script 'String b = new String("1919810");'
                      script 'String a = "114514"; String b = "1919810"; Log.i("Tag", a + " " + b);'
                      script 'int x = (10 + 5) * 2;'
                      script 'for (int i = 0; i < 114; i++) println(i);
                      script 'int[][] arr = new int[3][3] {new int[]{1, 2, 3}, new int[]{4, 5, 6}, new int[]{7, 8, 9}}; println(arr);'
                    
                    
                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
        }
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String cmdName = context.cmdName();
        ClassLoader classLoader = context.classLoader();
        
        if (cmdName.equals("sclear")) {
            return clearExecutorVariables(context);
        } else if (cmdName.equals("svars")) {
            return listExecutorVariables(context);
        } else {
            if (context.origCommand().isEmpty()) {
                return getHelpText();
            }
            try {
                logger.info("执行脚本代码: " + context.origCommand());
                TestInterpreter.ScriptRunner runner = getScriptExecutor(classLoader);
                runner.execute(context.origCommand().toString(), context.output(), context.output());
                return "";

            } catch (Exception e) {
                logger.error("脚本执行失败", e);
                return "脚本执行失败: " + e.getMessage() + "\n堆栈追踪: \n" + android.util.Log.getStackTraceString(e);
            }
        }
    }


    private TestInterpreter.ScriptRunner getScriptExecutor(ClassLoader cl) {
        if (cl == null) return systemScriptRunner;
        return scriptRunners.computeIfAbsent(cl, TestInterpreter.ScriptRunner::new);
    }

    public String clearExecutorVariables(CommandExecutor.CmdExecContext context) {
        getScriptExecutor(context.classLoader()).clearVariables();
        return "已清空所有执行器的变量\n提示: 只清空了" +
                (context.targetPackage() == null ? "默认" : context.targetPackage())
                + "的ClassLoader的执行器的变量，其他ClassLoader的并没有被清空";
    }

    public String listExecutorVariables(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();

        StringBuilder sb = new StringBuilder("(当前ClassLoader: " + (targetPackage == null ? "默认加载器" : targetPackage) + ")");

        sb.append("\n\n脚本执行器的变量列表:\n");
        java.util.Map<String, Object> scriptVars = getScriptExecutor(classLoader).getVariables();
        if (scriptVars.isEmpty()) {
            sb.append("  (空)\n");
        } else {
            for (java.util.Map.Entry<String, Object> entry : scriptVars.entrySet()) {
                Object value = entry.getValue();
                sb.append("  ").append(entry.getKey())
                        .append(" = ").append(value)
                        .append(" (").append(value != null ? value.getClass().getSimpleName() : "null")
                        .append(")\n");
            }
        }

        return sb.toString();
    }

}
