package com.justnothing.testmodule.command.functions.script;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SCRIPT_VER;


import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.javainterpreter.ScriptRunner;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.concurrent.ThreadPoolManager;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ParseException;
import com.justnothing.javainterpreter.security.PermissionType;
import com.justnothing.javainterpreter.security.SandboxConfig;
import com.justnothing.testmodule.utils.reflect.AppClassFinder;
import com.justnothing.testmodule.utils.sandbox.BlockGuardSandbox;
import com.justnothing.testmodule.utils.reflect.DexClassDefiner;
import com.justnothing.javainterpreter.evaluator.DynamicClassGenerator;

import java.io.File;
import java.util.Map;
import java.util.Date;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

public class ScriptExecutorMain extends CommandBase {

    private static final ConcurrentHashMap<ClassLoader, ScriptRunner>
            scriptRunners = new ConcurrentHashMap<>();
    private static final ScriptRunner systemScriptRunner;
    private static final AtomicReference<SandboxConfig> currentPermissionConfig = new AtomicReference<>(null);
    
    static {
        systemScriptRunner = new ScriptRunner(null);
        systemScriptRunner.setClassFinder(new AppClassFinder());
        DynamicClassGenerator.setDefaultClassDefiner(new DexClassDefiner());
    }

    private final String commandName;

    public ScriptExecutorMain(String commandName) {
        super("ScriptExecutor");
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        return switch (commandName) {
            case "sclear" -> String.format("""
                    语法: sclear
                    
                    清空脚本执行器的所有变量.
                    
                    示例:
                        sclear
                    
                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            case "svars" -> String.format("""
                    语法: svars
                    
                    显示脚本执行器的变量列表.
                    
                    示例:
                        svars
                    
                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            case "srun" -> String.format("""
                    语法: srun <code>
                    
                    快捷执行脚本代码.
                    具体执行逻辑与script run相同.
                    (注: 运行script可以查看说明)
                    
                    示例:
                        srun 'String a = "114514"; println(a);'
                        srun 'for (int i = 0; i < 10; i++) println(i);'
                    
                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            case "sinteractive" -> String.format("""
                    语法: sinteractive
                    
                    进入交互式脚本执行模式.
                    
                    多行模式:
                        :multi     - 进入多行模式
                        :eval      - 执行多行代码
                        :clear     - 清空缓冲区
                        (自动检测括号未闭合时也会进入多行模式)
                    
                    调试:
                        setPrintAST(true)  - 开启AST打印
                        setPrintAST(false) - 关闭AST打印
                    
                    退出命令:
                        exit, quit  - 退出交互式模式
                    
                    示例:
                        sinteractive
                    
                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
            default -> String.format("""
                            语法: script <subcmd> [args...] | script <code>
                            
                            用JustNothing1021 (在GLM4.7和GLM5的帮助下) 写的解释器跑Java代码.
                            就是目前还有一些语法不支持...习惯就好, 不会写AST(
                            
                            另外, 这玩意的性能很低, 不建议执行超过100次循环的代码
                            (是这样的, 以后会试着优化, 以我的技术力现在能用就已经是个奇迹了)
                            
                            子命令:
                                create <name>              - 创建新脚本
                                edit <name>                - 编辑脚本
                                list                       - 列出所有脚本
                                show <name>                - 显示脚本内容
                                delete <name>              - 删除脚本
                                run <name>                 - 执行脚本
                                run_code <code>            - 直接执行代码字符串（备用）
                                interactive                - 启动交互REPL执行器
                                import <file>              - 导入脚本文件
                                export <name> <file>       - 导出脚本文件
                                manage                     - 启动交互式脚本管理器
                                permission [action] [args] - 查看/修改权限配置
                                    grant <PERM1,PERM2>    - 授予权限
                                    deny <PERM1,PERM2>     - 拒绝权限
                                    preset <name>          - 应用预设
                                    reset                  - 重置为无限制
                                    list                   - 列出所有权限类型
                            
                            选项:
                                -p <preset>       - 权限预设 (放在子命令前)
                            
                            权限预设:
                                sandbox     - 沙箱模式 (禁止文件/网络/线程/反射)
                                expression  - 表达式模式 (仅允许计算)
                                minimal     - 最小权限 (允许读文件)
                                full        - 完全权限 (无限制)
                            
                            示例:
                                script -p sandbox run myscript    - 以沙箱权限执行脚本
                                script -p expression '1 + 2'      - 以表达式权限执行代码
                                script permission list            - 列出所有权限类型
                            
                            
                            ═══════════════════════════════════════════════════════════════
                                              语法糖说明 (不同于原版Java的新特性)
                            ═══════════════════════════════════════════════════════════════
                            
                            [0] 某些神秘的运算符
                                auto pow = 2 ** 3;                              // 8
                                auto spaceship = 1 <=> 2;                       // 左大会返回-1, 右大会返回1, 相等返回0
                                auto intersectionSet = {1, 2, 3} & {2, 3, 4};   // 交集, {2, 3}, 还有很多集合操作可以自己琢磨
                                auto doAsync = () -> async someTask();          // 异步执行然后返回Future, 往下面看
                                auto result = await doAsync();                  // 等待async任务完成, 并获取结果, 往下面看
                                auto address = database?.user?.address;         // 可选链, 往下面看
                            
                            [1] Lambda表达式 (有独立类型也可以转FunctionalInterface)
                                auto add = (x, y) -> x + y;
                                add(1, 2);  // 3
                            
                                auto greet = (name) -> { println("Hello, " + name); };
                                greet("World");
                            
                            [2] 方法引用 (可以直接调用, 转Lambda/FunctionalInterface)
                                auto parseInt = Integer::parseInt;
                                parseInt("123");  // 123
                            
                                auto strLen = "hello"::length;
                                strLen();  // 5
                            
                            [3] Range范围操作符, 生成整数 (或者说字符) 序列
                                for (i in 1..5) print(i);      // 12345
                                for (c in 'a'..'e') print(c);  // abcde
                                auto nums = 1..10;             // [1,2,3,4,5,6,7,8,9,10]
                            
                            [4] f-string字符串插值 (Python风格, 但是有点点区别)
                                auto name = "Alice";
                                auto age = 18;
                                f"My name is ${name}, age ${age}";  // "My name is Alice, age 18"
                                f"My name is $name";                // "My name is Alice"
                                f"My name is $name, age $age";      //  boom, 因为找不到"name,"这个变量
                            
                            [5] Pipeline管道操作符
                                auto result = "  hello  "
                                    |> String::trim
                                    |> String::toUpperCase;
                                // "HELLO"
                            
                                [1, 2, 3, 4, 5]
                                    |> filter((x) -> x > 2)
                                    |> map((x) -> x * 2);  // [6, 8, 10]
                            
                                auto twice = x -> x * 2;
                                auto addOne = x -> x + 1;
                                auto square = x -> x ** 2;
                            
                                auto result = 3 |> twice |> addOne |> square;  // 49 (3 -> 6 -> 7 -> 49)
                            
                            [6] 安全调用操作符 (在非null的时候调用, 不然返回null)
                                auto obj = null;
                                obj?.method();                   // null (不报错)
                                obj?.field;                      // null
                                obj?.method()?.anotherMethod();  // 链式安全调用
                            
                            [7] Elvis操作符 (空值合并)
                                auto value = null;
                                auto result = value ?: "default";  // "default"
                            
                            [8] 非空断言
                                auto value = null;
                                !!value;  // 抛出 NullPointerException
                            
                            [9] 条件赋值操作符
                                Integer a = null;
                                a ?= 1; // a = a != null ? a : 1, a
                            
                            [10] 集合操作符
                                auto a = [1, 2, 3];
                                auto b = [3, 4, 5];
                                a | b;  // 并集   [1, 2, 3, 4, 5]
                                a - b;  // 差集   [1, 2]
                                a & b;  // 交集   [3]
                                a ^ b;  // 异或集 [1, 2, 4, 5]
                            
                            [11] Async/Await 异步支持
                                auto future = async {
                                    Thread.sleep(1000);
                                    return "done";
                                };
                                auto result = await future;  // "done" (阻塞等待)
                            
                            [12] Switch 表达式
                                auto result = switch (x) {
                                    case 1 -> "one";
                                    case 2 -> "two";
                                    default -> "other";
                                };
                            
                            [13] Try-with-resources
                                try (auto is = new FileInputStream("/path")) {
                                    // 自动关闭资源
                                }
                            
                            [14] 类定义 (动态生成)
                                class Point {
                                    int x, y;
                                    Point(int x, int y) { this.x = x; this.y = y; }
                                    int sum() { return x + y; }
                                }
                                auto p = new Point(1, 2);
                                p.sum();  // 3
                            
                            [15] 短声明 (自动推导类型, 相当于auto, 会按最严格的类型推导)
                                x := 10;           // int
                                name := "hello";   // String
                                arr := [1, 2, 3];  // Object[], 这个是例外
                            
                            ═══════════════════════════════════════════════════════════════
                                                      内置函数
                            ═══════════════════════════════════════════════════════════════
                            
                            输出:
                                print(s)                 - 输出到缓冲区
                                println(s)               - 输出并换行
                            
                            输入:
                                readLine(prompt?)        - 读取用户输入 (可选提示)
                                readPassword(prompt?)    - 读取密码 (隐藏输入)
                                setupInteractiveInput()  - 重定向System.in到交互输入
                                                           (调用后Scanner等可正常使用)
                            
                            集合操作:
                                map(collection, fn)      - 映射
                                filter(collection, fn)   - 过滤
                                reduce(collection, fn)   - 归约
                                range(start, end)        - 生成范围
                                keys(map)                - 获取Map的键
                                values(map)              - 获取Map的值
                                size(collection)         - 获取大小
                                contains(coll, elem)     - 检查包含
                            
                            字符串:
                                split(str, delim)        - 分割字符串
                                join(coll, delim)        - 连接为字符串
                            
                            数学:
                                random()                 - 随机数 [0.0, 1.0)
                                randint(min, max)        - 随机整数
                                abs(n), min(a,b), max(a,b), clamp(v,min,max)
                            
                            反射:
                                getField(obj, name)                 - 获取字段
                                setField(obj, name, val)            - 设置字段
                                invokeMethod(obj, name, args...)    - 调用方法
                            
                            类型:
                                typeOf(obj)              - 类型名称
                                isInstanceOf(obj, cls)   - 类型检查
                                cast(obj, className)     - 类型转换
                            
                            Android:
                                getContext()             - 获取Context
                                getPackageName()         - 获取包名
                                getApplicationInfo()     - 获取应用信息
                            
                            调试:
                                analyze(obj)             - 分析对象
                                deepAnalyze(obj)         - 深度分析
                                trace()                  - 打印调用栈
                                setPrintAST(bool)        - 开关AST打印
                            
                            时间:
                                currentTimeMillis()      - 毫秒时间戳
                                nanoTime()               - 纳秒时间
                                sleep(ms)                - 休眠
                            
                            线程:
                                runLater(lambda)         - 新线程执行
                                createSafeExecutor()     - 创建线程安全执行器
                                asRunnable(lambda)       - Lambda转Runnable
                                asFunction(lambda)       - Lambda转Function
                            
                            ═══════════════════════════════════════════════════════════════
                                                      注意事项
                            ═══════════════════════════════════════════════════════════════
                            
                            (1) auto类型必须有初始值
                                auto x = 10;     // ok
                                auto y;          // boom
                            
                            (2) 泛型类型信息会被擦除
                                List<Integer> list = new ArrayList();
                                list.add("wtf"); // 能跑, 但不建议
                            
                            (3) 原始类型会自动装箱
                                >>> (1).getClass();
                                class java.lang.Integer
                            
                            (4) 自动导入的包:
                                java.util.*
                                java.lang.*
                                java.lang.reflect.*
                                android.util.*
                                android.os.*
                            
                            (5) delete 可以删除变量
                                delete x;    // 删除x
                                delete *;    // 删除所有
                            
                            (6) 遇到报错不一定是你的问题, 可能是解析器不支持这个语法 (
                            
                            示例:
                                script create my_hook
                                script list
                                script show my_hook
                                script run my_hook
                                script import /sdcard/script.java
                                script export my_hook /sdcard/backup.java
                                script delete my_hook
                                script manage
                                script 'String a = "114514"; println(a);'
                            
                            注:
                                - 脚本保存在 %s 下
                            
                            (Submodule script %s)
                            """,
                    DataBridge.getScriptsDirectory().getAbsolutePath(),
                    CMD_SCRIPT_VER);
        };
    }

    @Override
    public void runMain(CommandExecutor.CmdExecContext context) {
        String cmdName = context.cmdName();

        switch (cmdName) {
            case "sclear" -> clearExecutorVariables(context);
            case "svars" -> listExecutorVariables(context);
            case "srun" -> executeScriptCode(context);
            case "sinteractive" -> runInteractiveMode(context);
            case "script" -> {
                String[] args = context.args();
                if (args.length == 0) {
                    context.println(getHelpText(), Colors.WHITE);
                    return;
                }
                try {
                    handleScriptManagerCommand(context);
                } catch (Exception e) {
                    CommandExceptionHandler.handleException("script " + args[0], e, context, "执行script命令失败");
                }
            }
        }
    }

    private void executeScriptCode(CommandExecutor.CmdExecContext context) {
        String code = context.origCommand();
        context.println("[脚本执行] 在独立线程中执行...", Colors.CYAN);
        context.println("", Colors.WHITE);
        
        AtomicReference<Throwable> errorRef = new AtomicReference<>(null);
        
        Future<?> future = ThreadPoolManager.submitIOCallable(() -> {
            try {
                SandboxConfig config = currentPermissionConfig.get();
                
                if (config != null) {
                    BlockGuardSandbox.execute(config, () -> {
                        logger.info("执行脚本代码 (沙箱): " + code);
                        ScriptRunner runner = getScriptExecutor(context.classLoader());
                        if (config.getAstPermissionChecker() != null) {
                            runner.getExecutionContext().setPermissionChecker(config.getAstPermissionChecker());
                        }
                        runner.execute(code, context.output(), context.output());
                        return null;
                    });
                } else {
                    logger.info("执行脚本代码: " + code);
                    ScriptRunner runner = getScriptExecutor(context.classLoader());
                    runner.execute(code, context.output(), context.output());
                }
            } catch (Throwable e) {
                errorRef.set(e);
            }
            return null;
        });


        
        try {
            assert future != null : "无法获取用于执行脚本代码的Future";
            future.get(5, TimeUnit.MINUTES);
            
            if (errorRef.get() != null) {
                Throwable e = errorRef.get();
                Throwable cause = e;
                while (cause instanceof RuntimeException && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                if (cause instanceof SecurityException) {
                    context.print("权限拒绝: ", Colors.RED);
                    context.println(Objects.requireNonNullElse(cause.getMessage(), "没有详细信息"), Colors.ORANGE);
                } else {
                    CommandExceptionHandler.handleException("script", e, context, "脚本执行失败");
                }
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            context.println("脚本执行超时（5分钟），已取消", Colors.RED);
        } catch (Exception e) {
            context.print("等待脚本执行结果时发生异常: ", Colors.RED);
            context.println(Objects.requireNonNullElse(e.getMessage(), "没有错误信息"), Colors.ORANGE);
        }
    }

    private void handleScriptManagerCommand(CommandExecutor.CmdExecContext context) throws IOException {
        String[] args = context.args();
        
        logger.debug("执行script命令，参数: " + Arrays.toString(args));
        
        if (args.length < 1) {
            getHelpText();
            return;
        }

        SandboxConfig permissionConfig = null;
        String[] effectiveArgs = args;
        
        if (args.length >= 2 && "-p".equals(args[0])) {
            String preset = args[1];
            permissionConfig = resolvePreset(preset, context);
            if (permissionConfig == null && !"full".equalsIgnoreCase(preset)) {
                return;
            }
            effectiveArgs = new String[args.length - 2];
            System.arraycopy(args, 2, effectiveArgs, 0, args.length - 2);
            
            if (effectiveArgs.length < 1) {
                context.println("错误: -p 参数后需要指定子命令", Colors.RED);
                getHelpText();
                return;
            }
            
            context.print("[权限模式: ", Colors.YELLOW);
            context.print(preset, Colors.ORANGE);
            context.println("]", Colors.YELLOW);
        }

        String subCommand = effectiveArgs[0];
        SandboxConfig previousConfig = currentPermissionConfig.get();
        
        if (permissionConfig != null) {
            currentPermissionConfig.set(permissionConfig);
        }

        try {
            switch (subCommand) {
                case "create" -> handleCreate(effectiveArgs, context);
                case "list" -> handleList(context);
                case "show" -> handleShow(effectiveArgs, context);
                case "delete" -> handleDelete(effectiveArgs, context);
                case "run" -> handleRun(effectiveArgs, context);
                case "run_code" -> handleRunCode(context);
                case "import" -> handleImport(effectiveArgs, context);
                case "export" -> handleExport(effectiveArgs, context);
                case "manage" -> handleManage(context);
                case "permission" -> handlePermission(effectiveArgs, context);
                case "interactive" -> runInteractiveMode(context);
                default -> {
                    context.print("未知子命令: ", Colors.RED);
                    context.println(subCommand, Colors.YELLOW);
                    getHelpText();
                }
            }
        } finally {
            if (permissionConfig != null) {
                if (previousConfig != null) {
                    currentPermissionConfig.set(previousConfig);
                } else {
                    currentPermissionConfig.set(null);
                }
            }
        }
    }

    private ScriptRunner getScriptExecutor(ClassLoader cl) {
        if (cl == null) return systemScriptRunner;
        return scriptRunners.computeIfAbsent(cl, ScriptRunner::new);
    }

    public void clearExecutorVariables(CommandExecutor.CmdExecContext context) {
        getScriptExecutor(context.classLoader()).clearVariables();
        context.println("已清空所有执行器的变量", Colors.GREEN);
        context.print("提示: ", Colors.CYAN);
        context.print("只清空了", Colors.GRAY);
        context.print((context.targetPackage() == null ? "默认" : context.targetPackage()), Colors.YELLOW);
        context.println("的ClassLoader的执行器的变量，其他ClassLoader的并没有被清空", Colors.GRAY);
    }

    public void listExecutorVariables(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();

        context.print("(当前ClassLoader: ", Colors.GRAY);
        context.println((targetPackage == null ? "默认加载器" : targetPackage) + ")", Colors.YELLOW);
        context.println("", Colors.WHITE);
        context.println("脚本执行器的变量列表:", Colors.CYAN);
        
        Map<String, Object> scriptVars = getScriptExecutor(classLoader).getAllVariablesAsObject();
        if (scriptVars.isEmpty()) {
            context.println("  (空)", Colors.GRAY);
        } else {
            for (Map.Entry<String, Object> entry : scriptVars.entrySet()) {
                Object value = entry.getValue();
                context.print("  " + entry.getKey() + " = ", Colors.CYAN);
                context.print(String.valueOf(value), Colors.GREEN);
                context.println(" (" + (value != null ? value.getClass().getSimpleName() : "null") + ")", Colors.GRAY);
            }
        }

    }

    private void handleCreate(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            context.println("错误: 需要指定脚本名称", Colors.RED);
            context.println("用法: script create <name>", Colors.GRAY);
            return;
        }

        String scriptName = args[1];
        if (!isValidScriptName(scriptName)) {
            context.println("错误: 脚本名称只能包含字母、数字和下划线", Colors.RED);
            return;
        }

        File scriptFile = DataBridge.getScriptFile(scriptName);
        if (scriptFile.exists()) {
            context.print("错误: 脚本 '", Colors.RED);
            context.print(scriptName, Colors.YELLOW);
            context.println("' 已存在", Colors.RED);
            return;
        }

        IOManager.createDirectory(Objects.requireNonNull(scriptFile.getParentFile()).getAbsolutePath());

        String content = "// Script: " + scriptName + "\n" +
                         "// Created by: " + 
                         new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            .format(new Date()) +
                         "\n" +
                         "// 在这里编写你的脚本代码...\n";
        
        IOManager.writeFile(scriptFile.getAbsolutePath(), content);

        context.print("脚本 '", Colors.GREEN);
        context.print(scriptName, Colors.YELLOW);
        context.println("' 创建成功", Colors.GREEN);
        context.print("路径: ", Colors.CYAN);
        context.println(scriptFile.getAbsolutePath(), Colors.GREEN);
    }

    private void handleList(CommandExecutor.CmdExecContext context) {
        File scriptsDir = DataBridge.getScriptsDirectory();
        
        if (!scriptsDir.exists()) {
            IOManager.createDirectory(scriptsDir);
        }

        File[] scriptFiles = scriptsDir.listFiles();

        if (scriptFiles == null || scriptFiles.length == 0) {
            context.println("没有找到脚本", Colors.GRAY);
            return;
        }

        context.println("===== 脚本列表 =====", Colors.CYAN);
        context.println("", Colors.WHITE);

        for (File scriptFile : scriptFiles) {
            String name = scriptFile.getName();
            long size = scriptFile.length();
            long lastModified = scriptFile.lastModified();
            
            context.print("名称: ", Colors.CYAN);
            context.println(name, Colors.YELLOW);
            context.print("  大小: ", Colors.CYAN);
            context.println(formatSize(size), Colors.GREEN);
            context.print("  修改时间: ", Colors.CYAN);
            context.println(formatTime(lastModified), Colors.GREEN);
            context.print("  路径: ", Colors.CYAN);
            context.println(scriptFile.getAbsolutePath(), Colors.GRAY);
            context.println("", Colors.WHITE);
        }

        context.print("总计: ", Colors.CYAN);
        context.print(String.valueOf(scriptFiles.length), Colors.YELLOW);
        context.println(" 个脚本", Colors.CYAN);
    }

    private void handleShow(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            context.println("错误: 需要指定文件名称", Colors.RED);
            context.println("用法: script show <name>", Colors.GRAY);
            return;
        }

        String fileName = args[1];
        File scriptsDir = DataBridge.getScriptsDirectory();
        File targetFile = new File(scriptsDir, fileName);

        if (!targetFile.exists()) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("' 不存在", Colors.RED);
            return;
        }

        String content = IOManager.readFile(targetFile.getAbsolutePath());
        context.print("===== 文件内容: ", Colors.CYAN);
        context.print(fileName, Colors.YELLOW);
        context.println(" =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        context.println(content, Colors.WHITE);
    }

    private void handleDelete(String[] args, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            context.println("错误: 需要指定文件名称", Colors.RED);
            context.println("用法: script delete <name>", Colors.GRAY);
            return;
        }

        String fileName = args[1];
        File scriptsDir = DataBridge.getScriptsDirectory();
        File targetFile = new File(scriptsDir, fileName);

        if (!targetFile.exists()) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("' 不存在", Colors.RED);
            return;
        }

        if (IOManager.deleteFile(targetFile.getAbsolutePath())) {
            context.print("文件 '", Colors.GREEN);
            context.print(fileName, Colors.YELLOW);
            context.println("' 已删除", Colors.GREEN);
        } else {
            context.print("错误: 无法删除文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("'", Colors.RED);
        }
    }

    private void handleRun(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            context.println("错误: 需要指定文件名称", Colors.RED);
            context.println("用法: script [-p preset] run <name>", Colors.GRAY);
            return;
        }

        String fileName = args[1];

        File scriptsDir = DataBridge.getScriptsDirectory();
        File targetFile = new File(scriptsDir, fileName);

        if (!targetFile.exists()) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("' 不存在", Colors.RED);
            return;
        }

        String content = IOManager.readFile(targetFile.getAbsolutePath());
        
        context.print("===== 执行脚本: ", Colors.CYAN);
        context.print(fileName, Colors.YELLOW);
        context.println(" =====", Colors.CYAN);
        context.println("[脚本执行] 在独立线程中执行...", Colors.CYAN);
        context.println("", Colors.WHITE);
        
        SandboxConfig config = currentPermissionConfig.get();
        AtomicReference<Throwable> errorRef = new AtomicReference<>(null);
        
        Future<?> future = ThreadPoolManager.submitIOCallable(() -> {
            try {
                ScriptRunner runner = new ScriptRunner(context.classLoader());
                runner.setClassFinder(new AppClassFinder());
                
                if (config != null) {
                    if (config.getAstPermissionChecker() != null) {
                        runner.getExecutionContext().setPermissionChecker(config.getAstPermissionChecker());
                    }
                    BlockGuardSandbox.execute(config, () -> {
                        try {
                            runner.execute(content, context.output(), context.output());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else {
                    runner.execute(content, context.output(), context.output());
                }
            } catch (Throwable e) {
                errorRef.set(e);
            }
            return null;
        });
        
        try {
            assert future != null : "无法获取用于执行代码的Future";
            future.get(5, TimeUnit.MINUTES);
            
            if (errorRef.get() != null) {
                Throwable e = errorRef.get();
                Throwable cause = e;
                while (cause instanceof RuntimeException && cause.getCause() != null) {
                    cause = cause.getCause();
                }
                if (cause instanceof SecurityException) {
                    context.print("权限拒绝: ", Colors.RED);
                    context.println(Objects.requireNonNullElse(cause.getMessage(), "没有详细信息"), Colors.ORANGE);
                } else {
                    CommandExceptionHandler.handleException("script run", e, context, "脚本执行失败");
                }
            } else {
                context.println("脚本执行成功", Colors.GREEN);
            }
        } catch (TimeoutException e) {
            future.cancel(true);
            context.println("脚本执行超时（5分钟），已取消", Colors.RED);
        } catch (Exception e) {
            context.print("等待脚本执行结果时发生异常: ", Colors.RED);
            context.println(Objects.requireNonNullElse(e.getMessage(), "没有详细信息"), Colors.ORANGE);
        }
    }

    private void handleRunCode(CommandExecutor.CmdExecContext context) {
        String origCommand = context.origCommand();
        
        if (origCommand == null || origCommand.isEmpty()) {
            context.println("错误: 没有提供代码", Colors.RED);
            context.println("用法: script run_code <code>", Colors.GRAY);
            return;
        }
        
        String code = extractCodeFromCommand(origCommand, "run_code");
        
        if (code == null || code.isEmpty()) {
            context.println("错误: 没有提供代码", Colors.RED);
            context.println("用法: script run_code <code>", Colors.GRAY);
            return;
        }
        
        context.println("===== 执行代码 =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        
        SandboxConfig config = currentPermissionConfig.get();
        
        try {
            ScriptRunner runner = new ScriptRunner(context.classLoader());
            runner.setClassFinder(new AppClassFinder());
            
            if (config != null) {
                if (config.getAstPermissionChecker() != null) {
                    runner.getExecutionContext().setPermissionChecker(config.getAstPermissionChecker());
                }
                BlockGuardSandbox.execute(config, () -> {
                    runner.execute(code, context.output(), context.output());
                });
            } else {
                runner.execute(code, context.output(), context.output());
            }
            context.println("代码执行成功", Colors.GREEN);
        } catch (Exception e) {
            CommandExceptionHandler.handleException("script run_code", e, context, "代码执行失败");
        }

    }

    @SuppressWarnings("SameParameterValue")
    private String extractCodeFromCommand(String command, String keyword) {
        int keywordIndex = command.indexOf(keyword);
        if (keywordIndex == -1) {
            return null;
        }
        
        int startIndex = keywordIndex + keyword.length();
        
        while (startIndex < command.length() && Character.isWhitespace(command.charAt(startIndex))) {
            startIndex++;
        }
        
        if (startIndex >= command.length()) {
            return null;
        }
        
        return command.substring(startIndex);
    }

    private void handleImport(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            context.println("错误: 需要指定文件路径", Colors.RED);
            context.println("用法: script import <file>", Colors.GRAY);
            return;
        }

        String filePath = args[1];
        File sourceFile = new File(filePath);

        if (!sourceFile.exists()) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(filePath, Colors.YELLOW);
            context.println("' 不存在", Colors.RED);
            return;
        }

        String content = IOManager.readFile(sourceFile.getAbsolutePath());
        String fileName = sourceFile.getName();
        File scriptsDir = DataBridge.getScriptsDirectory();
        IOManager.createDirectory(scriptsDir);
        File destFile = new File(scriptsDir, fileName);

        if (destFile.exists()) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("' 已存在", Colors.RED);
            context.print("提示: 使用 '", Colors.GRAY);
            context.print("script delete " + fileName, Colors.CYAN);
            context.println("' 删除旧文件", Colors.GRAY);
            return;
        }

        IOManager.createDirectory(Objects.requireNonNull(destFile.getParentFile()).getAbsolutePath());
        IOManager.writeFile(destFile.getAbsolutePath(), content);

        context.println("文件导入成功", Colors.GREEN);
        context.print("源文件: ", Colors.CYAN);
        context.println(sourceFile.getAbsolutePath(), Colors.GREEN);
        context.print("文件名称: ", Colors.CYAN);
        context.println(fileName, Colors.YELLOW);
        context.print("目标路径: ", Colors.CYAN);
        context.println(destFile.getAbsolutePath(), Colors.GREEN);
    }

    private void handleExport(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 3) {
            context.println("错误: 需要指定文件名称和导出路径", Colors.RED);
            context.println("用法: script export <name> <file>", Colors.GRAY);
            return;
        }

        String fileName = args[1];
        String exportPath = args[2];
        File sourceFile = null;
        String fileType = null;
        
        File scriptFile = DataBridge.getScriptFile(fileName);
        if (scriptFile.exists()) {
            sourceFile = scriptFile;
            fileType = "脚本";
        } else {
            File codebaseDir = DataBridge.getScriptsDirectory();
            File codebaseFile = new File(codebaseDir, fileName);
            if (codebaseFile.exists()) {
                sourceFile = codebaseFile;
                fileType = "Codebase文件";
            }
        }
        
        if (sourceFile == null) {
            context.print("错误: 文件 '", Colors.RED);
            context.print(fileName, Colors.YELLOW);
            context.println("' 不存在（已检查脚本和codebase目录）", Colors.RED);
            return;
        }

        File exportFile = new File(exportPath);
        String content = IOManager.readFile(sourceFile.getAbsolutePath());

        IOManager.writeFile(exportFile.getAbsolutePath(), content);

        context.print(fileType, Colors.GREEN);
        context.println("导出成功", Colors.GREEN);
        context.print("文件: ", Colors.CYAN);
        context.println(fileName, Colors.YELLOW);
        context.print("导出路径: ", Colors.CYAN);
        context.println(exportFile.getAbsolutePath(), Colors.GREEN);
    }

    private void handleManage(CommandExecutor.CmdExecContext context) {
        context.println("===== 交互式脚本管理器 =====", Colors.CYAN);
        context.println("输入 'help' 查看可用命令, 'exit' 或 'quit' 退出", Colors.GRAY);
        context.println("", Colors.WHITE);

        label:
        while (true) {
            String input = context.readLine("manage> ");
            
            if (input == null) {
                break;
            }
            
            input = input.trim();

            switch (input) {
                case "":
                    continue;
                case "exit":
                case "quit":
                case "0":
                    context.println("退出脚本管理器", Colors.GREEN);
                    break label;
                case "help":
                case "?":
                    showManageHelp(context);
                    continue;
            }

            handleManageCommand(input, context);
        }

        context.println("脚本管理器已退出", Colors.GREEN);
    }
    
    private void showManageHelp(CommandExecutor.CmdExecContext context) {
        context.println("", Colors.WHITE);
        context.println("可用命令:", Colors.CYAN);
        context.print("  create <name>        ", Colors.YELLOW);
        context.println("- 创建新脚本", Colors.GRAY);
        context.print("  list                 ", Colors.YELLOW);
        context.println("- 列出所有脚本和codebase文件", Colors.GRAY);
        context.print("  show <name>          ", Colors.YELLOW);
        context.println("- 显示文件内容", Colors.GRAY);
        context.print("  edit <name>          ", Colors.YELLOW);
        context.println("- 编辑脚本内容", Colors.GRAY);
        context.print("  delete <name>        ", Colors.YELLOW);
        context.println("- 删除文件", Colors.GRAY);
        context.print("  run <name>           ", Colors.YELLOW);
        context.println("- 执行脚本或codebase文件", Colors.GRAY);
        context.print("  import <path>        ", Colors.YELLOW);
        context.println("- 导入文件", Colors.GRAY);
        context.print("  export <name> <path> ", Colors.YELLOW);
        context.println("- 导出文件", Colors.GRAY);
        context.print("  codebase             ", Colors.YELLOW);
        context.println("- 列出codebase文件", Colors.GRAY);
        context.print("  help                 ", Colors.YELLOW);
        context.println("- 显示此帮助", Colors.GRAY);
        context.print("  exit / quit          ", Colors.YELLOW);
        context.println("- 退出管理器", Colors.GRAY);
        context.println("", Colors.WHITE);
    }
    
    private void handleManageCommand(String input, CommandExecutor.CmdExecContext context) {
        String[] parts = input.split("\\s+", 3);
        String cmd = parts[0].toLowerCase();
        
        try {
            switch (cmd) {
                case "1", "create" -> {
                    if (parts.length < 2) {
                        context.println("用法: create <name>", Colors.GRAY);
                        break;
                    }
                    handleCreate(new String[]{"create", parts[1]}, context);
                }
                case "2", "list" -> handleList(context);
                case "3", "show" -> {
                    if (parts.length < 2) {
                        context.println("用法: show <name>", Colors.GRAY);
                        break;
                    }
                    handleShow(new String[]{"show", parts[1]}, context);
                }
                case "edit" -> {
                    if (parts.length < 2) {
                        context.println("用法: edit <name>", Colors.GRAY);
                        break;
                    }
                    handleEdit(parts[1], context);
                }
                case "4", "delete" -> {
                    if (parts.length < 2) {
                        context.println("用法: delete <name>", Colors.GRAY);
                        break;
                    }
                    handleDelete(new String[]{"delete", parts[1]}, context);
                }
                case "5", "run" -> {
                    if (parts.length < 2) {
                        context.println("用法: run <name>", Colors.GRAY);
                        break;
                    }
                    handleRun(new String[]{"run", parts[1]}, context);
                }
                case "6", "import" -> {
                    if (parts.length < 2) {
                        context.println("用法: import <path>", Colors.GRAY);
                        break;
                    }
                    handleImport(new String[]{"import", parts[1]}, context);
                }
                case "7", "export" -> {
                    if (parts.length < 3) {
                        context.println("用法: export <name> <path>", Colors.GRAY);
                        break;
                    }
                    handleExport(new String[]{"export", parts[1], parts[2]}, context);
                }
                case "codebase" -> handleCodebaseList(context);
                default -> {
                    context.print("未知命令: ", Colors.RED);
                    context.println(cmd, Colors.YELLOW);
                    context.println("输入 'help' 查看帮助", Colors.GRAY);
                }
            }
        } catch (Exception e) {
            context.print("错误: ", Colors.RED);
            context.println(Objects.requireNonNullElse(e.getMessage(), "没有详细信息"), Colors.ORANGE);
        }
    }
    
    private void handleEdit(String name, CommandExecutor.CmdExecContext context) throws IOException {
        File scriptFile = DataBridge.getScriptFile(name);
        
        if (!scriptFile.exists()) {
            context.print("错误: 脚本 '", Colors.RED);
            context.print(name, Colors.YELLOW);
            context.println("' 不存在", Colors.RED);
            return;
        }
        
        String existingContent = IOManager.readFile(scriptFile.getAbsolutePath());
        context.print("编辑脚本: ", Colors.CYAN);
        context.println(name, Colors.YELLOW);
        context.println("当前内容 (输入空行结束编辑):", Colors.GRAY);
        context.println("", Colors.WHITE);
        context.println(existingContent, Colors.WHITE);
        context.println("", Colors.WHITE);
        context.println("--- 开始编辑 (输入空行保存并退出) ---", Colors.CYAN);
        context.println("", Colors.WHITE);
        
        StringBuilder newContent = new StringBuilder();
        
        while (true) {
            String line = context.readLine("");
            if (line == null || line.isEmpty()) {
                break;
            }
            newContent.append(line).append("\n");
        }
        
        if (newContent.length() > 0) {
            IOManager.writeFile(scriptFile.getAbsolutePath(), newContent.toString());
            context.println("脚本已保存", Colors.GREEN);
        } else {
            context.println("编辑已取消 (未做更改)", Colors.GRAY);
        }
    }
    
    private void handlePermission(String[] args, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            showPermissionStatus(context);
            return;
        }

        String action = args[1];

        switch (action) {
            case "grant" -> {
                if (args.length < 3) {
                    context.println("用法: script permission grant <PERM1,PERM2,...>", Colors.GRAY);
                    context.println("可用权限: " + getPermissionList(), Colors.GRAY);
                    return;
                }
                modifyPermissions(args[2], true, context);
            }
            case "deny" -> {
                if (args.length < 3) {
                    context.println("用法: script permission deny <PERM1,PERM2,...>", Colors.GRAY);
                    context.println("可用权限: " + getPermissionList(), Colors.GRAY);
                    return;
                }
                modifyPermissions(args[2], false, context);
            }
            case "preset" -> {
                if (args.length < 3) {
                    context.println("用法: script permission preset <sandbox|expression|minimal|full>", Colors.GRAY);
                    return;
                }
                applyPreset(args[2], context);
            }
            case "reset" -> {
                currentPermissionConfig.set(null);
                context.println("权限配置已重置为默认 (无限制)", Colors.GREEN);
            }
            case "list" -> {
                context.println("可用权限类型:", Colors.CYAN);
                for (PermissionType pt : PermissionType.values()) {
                    context.print("  " + pt.getId(), Colors.YELLOW);
                    context.println(" - " + pt.getDescription(), Colors.GRAY);
                }
                context.println("", Colors.WHITE);
                context.println("预设:", Colors.CYAN);
                context.print("  sandbox    ", Colors.YELLOW);
                context.println("- 沙箱模式 (禁止文件/网络/线程/反射)", Colors.GRAY);
                context.print("  expression ", Colors.YELLOW);
                context.println("- 表达式模式 (仅允许计算)", Colors.GRAY);
                context.print("  minimal    ", Colors.YELLOW);
                context.println("- 最小权限 (允许读文件)", Colors.GRAY);
                context.print("  full       ", Colors.YELLOW);
                context.println("- 完全权限 (无限制)", Colors.GRAY);
            }
            default -> {
                context.print("未知权限操作: ", Colors.RED);
                context.println(action, Colors.YELLOW);
                context.println("可用操作: grant, deny, preset, reset, list", Colors.GRAY);
            }
        }
    }

    private void showPermissionStatus(CommandExecutor.CmdExecContext context) {
        SandboxConfig config = currentPermissionConfig.get();
        
        context.println("===== 当前权限配置 =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        
        if (config == null) {
            context.println("  未配置权限限制 (完全权限)", Colors.GREEN);
            return;
        }

        context.print("  磁盘读取: ", Colors.CYAN);
        context.println(config.isDiskReadAllowed() ? "允许" : "禁止", config.isDiskReadAllowed() ? Colors.GREEN : Colors.RED);
        context.print("  磁盘写入: ", Colors.CYAN);
        context.println(config.isDiskWriteAllowed() ? "允许" : "禁止", config.isDiskWriteAllowed() ? Colors.GREEN : Colors.RED);
        context.print("  网络操作: ", Colors.CYAN);
        context.println(config.isNetworkAllowed() ? "允许" : "禁止", config.isNetworkAllowed() ? Colors.GREEN : Colors.RED);
        context.print("  创建线程: ", Colors.CYAN);
        context.println(config.isThreadCreateAllowed() ? "允许" : "禁止", config.isThreadCreateAllowed() ? Colors.GREEN : Colors.RED);
        context.print("  创建进程: ", Colors.CYAN);
        context.println(config.isProcessCreateAllowed() ? "允许" : "禁止", config.isProcessCreateAllowed() ? Colors.GREEN : Colors.RED);
        context.print("  反射操作: ", Colors.CYAN);
        context.println(config.isReflectionAllowed() ? "允许" : "禁止", config.isReflectionAllowed() ? Colors.GREEN : Colors.RED);
        context.print("  系统退出: ", Colors.CYAN);
        context.println(config.isSystemExitAllowed() ? "允许" : "禁止", config.isSystemExitAllowed() ? Colors.GREEN : Colors.RED);
        context.print("  系统属性: ", Colors.CYAN);
        context.println(config.isSystemPropertyAllowed() ? "允许" : "禁止", config.isSystemPropertyAllowed() ? Colors.GREEN : Colors.RED);
        context.print("  类加载器: ", Colors.CYAN);
        context.println(config.isClassLoaderAllowed() ? "允许" : "禁止", config.isClassLoaderAllowed() ? Colors.GREEN : Colors.RED);
        
        context.println("", Colors.WHITE);
        context.println("使用 'script permission grant/deny <PERM>' 修改", Colors.GRAY);
        context.println("使用 'script permission preset <name>' 应用预设", Colors.GRAY);
    }

    private void modifyPermissions(String permList, boolean grant, CommandExecutor.CmdExecContext context) {
        SandboxConfig current = currentPermissionConfig.get();
        SandboxConfig.Builder builder = SandboxConfig.builder();

        if (current != null) {
            if (current.isDiskReadAllowed()) builder.allowDiskRead(); else builder.denyDiskRead();
            if (current.isDiskWriteAllowed()) builder.allowDiskWrite(); else builder.denyDiskWrite();
            if (current.isNetworkAllowed()) builder.allowNetwork(); else builder.denyNetwork();
            if (current.isThreadCreateAllowed()) builder.allowThreadCreate(); else builder.denyThreadCreate();
            if (current.isProcessCreateAllowed()) builder.allowProcessCreate(); else builder.denyProcessCreate();
            if (current.isReflectionAllowed()) builder.allowReflection(); else builder.denyReflection();
            if (current.isSystemExitAllowed()) builder.allowSystemExit(); else builder.denySystemExit();
            if (current.isSystemPropertyAllowed()) builder.allowSystemProperty(); else builder.denySystemProperty();
            if (current.isClassLoaderAllowed()) builder.allowClassLoader(); else builder.denyClassLoader();
        } else {
            builder.allowDiskRead().allowDiskWrite().allowNetwork()
                .allowThreadCreate().allowProcessCreate().allowReflection()
                .allowSystemExit().allowSystemProperty().allowClassLoader();
        }

        String[] perms = permList.split(",");
        int count = 0;

        for (String perm : perms) {
            String p = perm.trim().toLowerCase();
            switch (p) {
                case "file.read", "disk.read" -> { if (grant) builder.allowDiskRead(); else builder.denyDiskRead(); count++; }
                case "file.write", "disk.write" -> { if (grant) builder.allowDiskWrite(); else builder.denyDiskWrite(); count++; }
                case "network" -> { if (grant) builder.allowNetwork(); else builder.denyNetwork(); count++; }
                case "thread", "thread.create" -> { if (grant) builder.allowThreadCreate(); else builder.denyThreadCreate(); count++; }
                case "exec", "process", "process.create" -> { if (grant) builder.allowProcessCreate(); else builder.denyProcessCreate(); count++; }
                case "reflection" -> { if (grant) builder.allowReflection(); else builder.denyReflection(); count++; }
                case "system.exit" -> { if (grant) builder.allowSystemExit(); else builder.denySystemExit(); count++; }
                case "system.property" -> { if (grant) builder.allowSystemProperty(); else builder.denySystemProperty(); count++; }
                case "classloader" -> { if (grant) builder.allowClassLoader(); else builder.denyClassLoader(); count++; }
                default -> {
                    context.print("  未知权限: ", Colors.RED);
                    context.println(p, Colors.YELLOW);
                }
            }
        }

        currentPermissionConfig.set(builder.build());
        context.print((grant ? "已授予" : "已拒绝") + " " + count + " 项权限", Colors.GREEN);
        context.println(" (" + permList + ")", Colors.GRAY);
    }

    private void applyPreset(String preset, CommandExecutor.CmdExecContext context) {
        SandboxConfig config;

        switch (preset.toLowerCase()) {
            case "sandbox" -> {
                config = SandboxConfig.DEFAULT;
                context.println("已应用预设: sandbox (沙箱模式)", Colors.GREEN);
            }
            case "expression" -> {
                config = SandboxConfig.EXPRESSION_ONLY;
                context.println("已应用预设: expression (表达式模式)", Colors.GREEN);
            }
            case "minimal" -> {
                config = SandboxConfig.MINIMAL;
                context.println("已应用预设: minimal (最小权限)", Colors.GREEN);
            }
            case "full" -> {
                config = null;
                context.println("已应用预设: full (完全权限)", Colors.GREEN);
            }
            default -> {
                context.print("未知预设: ", Colors.RED);
                context.println(preset, Colors.YELLOW);
                context.println("可用预设: sandbox, expression, minimal, full", Colors.GRAY);
                return;
            }
        }

        currentPermissionConfig.set(config);
    }

    private String getPermissionList() {
        StringBuilder sb = new StringBuilder();
        for (PermissionType pt : PermissionType.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(pt.getId());
        }
        return sb.toString();
    }

    private SandboxConfig resolvePreset(String preset, CommandExecutor.CmdExecContext context) {
        switch (preset.toLowerCase()) {
            case "sandbox" -> { return SandboxConfig.SANDBOX; }
            case "expression" -> { return SandboxConfig.EXPRESSION_ONLY; }
            case "minimal" -> { return SandboxConfig.MINIMAL; }
            case "full" -> { return null; }
            default -> {
                context.print("未知预设: ", Colors.RED);
                context.println(preset, Colors.YELLOW);
                context.println("可用预设: sandbox, expression, minimal, full", Colors.GRAY);
                return null;
            }
        }
    }

    private void handleCodebaseList(CommandExecutor.CmdExecContext context) {
        File codebaseDir = DataBridge.getScriptsDirectory();
        
        if (!codebaseDir.exists()) {
            context.println("Codebase目录不存在", Colors.GRAY);
            return;
        }
        
        File[] files = codebaseDir.listFiles();
        if (files == null || files.length == 0) {
            context.println("Codebase目录为空", Colors.GRAY);
            return;
        }
        
        context.println("===== Codebase文件列表 =====", Colors.CYAN);
        context.println("", Colors.WHITE);
        
        for (File file : files) {
            context.print("  ", Colors.GRAY);
            context.print(file.getName(), Colors.GREEN);
            if (file.isDirectory()) {
                context.println("/", Colors.CYAN);
            } else {
                context.println("", Colors.WHITE);
            }
        }
        
        context.println("", Colors.WHITE);
        context.print("总计: ", Colors.CYAN);
        context.print(String.valueOf(files.length), Colors.YELLOW);
        context.println(" 个文件", Colors.CYAN);
    }


    private boolean isValidScriptName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format(Locale.getDefault(), "%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format(Locale.getDefault(), "%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private String formatTime(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return sdf.format(new Date(timestamp));
    }

    private void runInteractiveMode(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        ScriptRunner runner = getScriptExecutor(classLoader);
        
        logger.info("进入交互式脚本执行模式");
        context.println("====== 脚本交互执行模式 =====", Colors.CYAN);
        context.println("输入 'exit' 或 'quit' 退出 (你不会闲到拿这俩做变量名, 对吧)", Colors.GRAY);
        context.println("输入 ':multi' 进入多行模式, ':eval' 执行, ':clear' 清空", Colors.GRAY);
        context.println("输入 'setPrintAST(true)' 开启 AST 打印", Colors.GRAY);
        context.println("", Colors.WHITE);

        StringBuilder multiLineBuffer = new StringBuilder();
        boolean multiLineMode = false;
        boolean autoMultiLine = false;

        label:
        while (true) {
            String prompt = (multiLineMode || autoMultiLine) ? "... " : ">>> ";
            String code = context.readLine(prompt);
            
            if (code == null) {
                context.println("", Colors.WHITE);
                break;
            }
            
            code = code.trim();

            switch (code) {
                case "exit":
                case "quit":
                    context.println("Say goodbye~~~", Colors.GREEN);
                    break label;
                case "":
                    if (autoMultiLine) {
                        continue;
                    }
                    continue;
                case ":multi":
                    multiLineMode = true;
                    autoMultiLine = false;
                    context.println("进入多行模式, 输入 ':eval' 执行, ':clear' 清空, ':exit' 退出多行模式", Colors.CYAN);
                    continue;
            }

            if (code.equals(":exit") && (multiLineMode || autoMultiLine)) {
                multiLineMode = false;
                autoMultiLine = false;
                multiLineBuffer.setLength(0);
                context.println("退出多行模式", Colors.CYAN);
                continue;
            }
            
            if (code.equals(":clear") && (multiLineMode || autoMultiLine)) {
                multiLineBuffer.setLength(0);
                context.println("已清空", Colors.GREEN);
                continue;
            }
            
            if (code.equals(":eval") && (multiLineMode || autoMultiLine)) {
                String fullCode = multiLineBuffer.toString();
                multiLineBuffer.setLength(0);
                multiLineMode = false;
                autoMultiLine = false;
                
                if (fullCode.isEmpty()) {
                    context.println("没有代码可执行", Colors.GRAY);
                    continue;
                }
                
                executeCode(runner, fullCode, context);
                continue;
            }
            
            if (multiLineMode || autoMultiLine) {
                multiLineBuffer.append(code).append("\n");
                
                if (autoMultiLine && isCodeComplete(multiLineBuffer.toString())) {
                    String fullCode = multiLineBuffer.toString();
                    multiLineBuffer.setLength(0);
                    autoMultiLine = false;
                    
                    executeCode(runner, fullCode, context);
                }
                continue;
            }
            
            if (!isCodeComplete(code)) {
                multiLineBuffer.append(code).append("\n");
                autoMultiLine = true;
                continue;
            }
            
            executeCode(runner, code, context);
        }
        context.println("交互式模式已退出", Colors.GREEN);
    }
    
    private void executeCode(ScriptRunner runner, String code, CommandExecutor.CmdExecContext context) {
        SandboxConfig config = currentPermissionConfig.get();
        
        if (config != null) {
            if (config.getAstPermissionChecker() != null) {
                runner.getExecutionContext().setPermissionChecker(config.getAstPermissionChecker());
            }
            try {
                BlockGuardSandbox.execute(config, () -> {
                    Object result = runner.executeWithResult(code, context.output(), context.output());
                    if (result != null) {
                        context.println(String.valueOf(result), Colors.GRAY);
                    }
                });
            } catch (Throwable e) {
                handleExecutionException(e, context);
            }
        } else {
            runner.getExecutionContext().setPermissionChecker(null);
            try {
                Object result = runner.executeWithResult(code, context.output(), context.output());
                if (result != null) {
                    context.println(String.valueOf(result), Colors.GRAY);
                }
            } catch (Throwable e) {
                handleExecutionException(e, context);
            }
        }
    }
    
    private void handleExecutionException(Throwable e, CommandExecutor.CmdExecContext context) {
        Throwable cause = e.getCause();
        String message = e.getMessage();
        boolean isParseError = message != null && message.startsWith("Parse error:");
        
        byte errorColor = isParseError ? Colors.ORANGE : Colors.RED;
        
        if (cause instanceof EvaluationException evalEx) {
            Throwable innerCause = evalEx.getCause();
            context.print("错误: ", errorColor);
            context.println(evalEx.getMessage(), errorColor);
            if (innerCause != null) {
                context.output().printStackTrace(innerCause, Colors.GRAY);
            }
        } else if (cause instanceof ParseException parseEx) {
            context.print("语法错误: ", errorColor);
            context.println(parseEx.getMessage(), errorColor);
        } else if (cause != null) {
            context.println((isParseError ? "语法错误: " : "错误: ") + cause.getMessage(), errorColor);
            context.output().printStackTrace(cause, Colors.GRAY);
        } else {
            context.println((isParseError ? "语法错误: " : "错误: ") + message, errorColor);
            context.output().printStackTrace(e, Colors.GRAY);
        }
    }
    
    private static boolean isCodeComplete(String code) {
        int braceCount = 0;
        int parenCount = 0;
        int bracketCount = 0;
        int preprocessorConditionCount = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean inFString = false;
        boolean escape = false;
        
        String[] lines = code.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#ifdef") || trimmed.startsWith("#ifndef") || trimmed.startsWith("#if ")) {
                preprocessorConditionCount++;
            } else if (trimmed.equals("#endif")) {
                preprocessorConditionCount--;
            }
        }
        
        for (int i = 0; i < code.length(); i++) {
            char c = code.charAt(i);
            
            if (escape) {
                escape = false;
                continue;
            }
            
            if (c == '\\' && (inString || inChar || inFString)) {
                escape = true;
                continue;
            }
            
            if (c == '"' && !inChar) {
                if (i > 0 && code.charAt(i - 1) == 'f' && !inString && !inFString) {
                    inFString = true;
                } else if (inFString) {
                    inFString = false;
                } else {
                    inString = !inString;
                }
                continue;
            }
            
            if (c == '\'' && !inString && !inFString) {
                inChar = !inChar;
                continue;
            }
            
            if (inString || inChar || inFString) {
                continue;
            }
            
            switch (c) {
                case '{': braceCount++; break;
                case '}': braceCount--; break;
                case '(': parenCount++; break;
                case ')': parenCount--; break;
                case '[': bracketCount++; break;
                case ']': bracketCount--; break;
            }
        }
        
        return braceCount <= 0 && parenCount <= 0 && bracketCount <= 0 && preprocessorConditionCount <= 0;
    }

}
