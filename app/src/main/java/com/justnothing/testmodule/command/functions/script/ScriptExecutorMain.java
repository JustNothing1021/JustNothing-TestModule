package com.justnothing.testmodule.command.functions.script;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SCRIPT_VER;


import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.javainterpreter.ScriptRunner;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;
import com.justnothing.javainterpreter.exception.EvaluationException;
import com.justnothing.javainterpreter.exception.ParseException;
import com.justnothing.testmodule.utils.script.AppClassFinder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;

public class ScriptExecutorMain extends CommandBase {

    private static final ConcurrentHashMap<ClassLoader, ScriptRunner>
            scriptRunners = new ConcurrentHashMap<>();
    private static final ScriptRunner systemScriptRunner;
    
    static {
        systemScriptRunner = new ScriptRunner(null);
        systemScriptRunner.setClassFinder(new AppClassFinder());
    }

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
        } else if (commandName.equals("srun")) {
            return String.format("""
                    语法: srun <code>
                    
                    快捷执行脚本代码.
                    具体执行逻辑与script run相同.
                    (注: 运行script可以查看说明)
                    
                    示例:
                        srun 'String a = "114514"; println(a);'
                        srun 'for (int i = 0; i < 10; i++) println(i);'
                    
                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
        } else if (commandName.equals("sinteractive")) {
            return String.format("""
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
        } else {
            return String.format("""
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
                        import <file>              - 导入脚本文件
                        export <name> <file>       - 导出脚本文件
                        manage                     - 启动交互式脚本管理器
                
                    选项:
                        name              - 脚本名称
                        file              - 文件路径


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
        }
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String cmdName = context.cmdName();

        switch (cmdName) {
            case "sclear" -> {
                return clearExecutorVariables(context);
            }
            case "svars" -> {
                return listExecutorVariables(context);
            }
            case "srun" -> {
                return executeScriptCode(context);
            }
            case "sinteractive" -> {
                return runInteractiveMode(context);
            }
            case "script" -> {
                String[] args = context.args();
                if (args.length == 0) return getHelpText();
                try {
                    return handleScriptManagerCommand(context);
                } catch (Exception e) {
                    return CommandExceptionHandler.handleException("script script", e, logger, "执行script命令失败");
                }
            }
        }
        
        return getHelpText();
    }

    private String executeScriptCode(CommandExecutor.CmdExecContext context) {
        try {
            logger.info("执行脚本代码: " + context.origCommand());
            ScriptRunner runner = getScriptExecutor(context.classLoader());
            runner.execute(context.origCommand(), context.output(), context.output());
            return "";
        } catch (Exception e) {
            return CommandExceptionHandler.handleException("script", e, logger, "脚本执行失败");
        }
    }

    private String handleScriptManagerCommand(CommandExecutor.CmdExecContext context) throws IOException {
        String[] args = context.args();
        
        logger.debug("执行script命令，参数: " + Arrays.toString(args));
        
        if (args.length < 1) {
            return getHelpText();
        }

        String subCommand = args[0];

        switch (subCommand) {
            case "create":
                return handleCreate(args);
            case "list":
                return handleList();
            case "show":
                return handleShow(args);
            case "delete":
                return handleDelete(args);
            case "run":
                return handleRun(args, context);
            case "run_code":
                return handleRunCode(context);
            case "import":
                return handleImport(args);
            case "export":
                return handleExport(args);
            case "manage":
                return handleManage(context);
            default:
                return "未知子命令: " + subCommand + "\n" + getHelpText();
        }
    }

    private ScriptRunner getScriptExecutor(ClassLoader cl) {
        if (cl == null) return systemScriptRunner;
        return scriptRunners.computeIfAbsent(cl, ScriptRunner::new);
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
        java.util.Map<String, Object> scriptVars = getScriptExecutor(classLoader).getAllVariablesAsObject();
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

    private String handleCreate(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定脚本名称\n用法: script create <name>";
        }

        String scriptName = args[1];
        if (!isValidScriptName(scriptName)) {
            return "错误: 脚本名称只能包含字母、数字和下划线";
        }

        File scriptFile = getScriptFile(scriptName);
        if (scriptFile.exists()) {
            return "错误: 脚本 '" + scriptName + "' 已存在";
        }

        IOManager.createDirectory(scriptFile.getParentFile().getAbsolutePath());
        
        StringBuilder content = new StringBuilder();
        content.append("// Script: ").append(scriptName).append("\n");
        content.append("// Created by: ").append(System.currentTimeMillis()).append("\n");
        content.append("\n");
        content.append("// 在这里编写你的脚本代码...\n");
        
        IOManager.writeFile(scriptFile.getAbsolutePath(), content.toString());

        return "脚本 '" + scriptName + "' 创建成功\n" +
                "路径: " + scriptFile.getAbsolutePath();
    }

    private String handleList() throws IOException {
        File scriptsDir = DataBridge.getScriptsDirectory();
        
        if (!scriptsDir.exists()) {
            scriptsDir.mkdirs();
        }

        File[] scriptFiles = scriptsDir.listFiles();

        if (scriptFiles == null || scriptFiles.length == 0) {
            return "没有找到脚本";
        }

        StringBuilder result = new StringBuilder();
        result.append("===== 脚本列表 =====\n\n");

        for (File scriptFile : scriptFiles) {
            String name = scriptFile.getName();
            long size = scriptFile.length();
            long lastModified = scriptFile.lastModified();
            
            result.append("名称: ").append(name).append("\n");
            result.append("  大小: ").append(formatSize(size)).append("\n");
            result.append("  修改时间: ").append(formatTime(lastModified)).append("\n");
            result.append("  路径: ").append(scriptFile.getAbsolutePath()).append("\n\n");
        }

        result.append("总计: ").append(scriptFiles.length).append(" 个脚本");
        return result.toString();
    }

    private String handleShow(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定文件名称\n用法: script show <name>";
        }

        String fileName = args[1];
        File scriptsDir = DataBridge.getScriptsDirectory();
        File targetFile = new File(scriptsDir, fileName);

        if (!targetFile.exists()) {
            return "错误: 文件 '" + fileName + "' 不存在";
        }

        String content = IOManager.readFile(targetFile.getAbsolutePath());
        return "===== 文件内容: " + fileName + " =====\n\n" + content;
    }

    private String handleDelete(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定文件名称\n用法: script delete <name>";
        }

        String fileName = args[1];
        File scriptsDir = DataBridge.getScriptsDirectory();
        File targetFile = new File(scriptsDir, fileName);

        if (!targetFile.exists()) {
            return "错误: 文件 '" + fileName + "' 不存在";
        }

        if (targetFile.delete()) {
            return "文件 '" + fileName + "' 已删除";
        } else {
            return "错误: 无法删除文件 '" + fileName + "'";
        }
    }

    private String handleRun(String[] args, CommandExecutor.CmdExecContext context) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定文件名称\n用法: script run <name>";
        }

        String fileName = args[1];
        File scriptsDir = DataBridge.getScriptsDirectory();
        File targetFile = new File(scriptsDir, fileName);

        if (!targetFile.exists()) {
            return "错误: 文件 '" + fileName + "' 不存在";
        }

        String content = IOManager.readFile(targetFile.getAbsolutePath());
        
        StringBuilder result = new StringBuilder();
        result.append("===== 执行脚本: ").append(fileName).append(" =====\n\n");
        
        try {
            ScriptRunner runner = new ScriptRunner(context.classLoader());
            runner.setClassFinder(new AppClassFinder());
            runner.execute(content, context.output(), context.output());
            result.append("脚本执行成功");
        } catch (Exception e) {
            result.append(CommandExceptionHandler.handleException("script run", e, logger, "脚本执行失败"));
        }
        
        return result.toString();
    }

    private String handleRunCode(CommandExecutor.CmdExecContext context) {
        String origCommand = context.origCommand();
        
        if (origCommand == null || origCommand.isEmpty()) {
            return "错误: 没有提供代码\n用法: script run_code <code>";
        }
        
        String code = extractCodeFromCommand(origCommand, "run_code");
        
        if (code == null || code.isEmpty()) {
            return "错误: 没有提供代码\n用法: script run_code <code>";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("===== 执行代码 =====\n\n");
        
        try {
            ScriptRunner runner = new ScriptRunner(context.classLoader());
            runner.setClassFinder(new AppClassFinder());
            runner.execute(code, context.output(), context.output());
            result.append("代码执行成功");
        } catch (Exception e) {
            result.append(CommandExceptionHandler.handleException("script runcode", e, logger, "代码执行失败"));
        }
        
        return result.toString();
    }

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

    private String handleImport(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定文件路径\n用法: script import <file>";
        }

        String filePath = args[1];
        File sourceFile = new File(filePath);

        if (!sourceFile.exists()) {
            return "错误: 文件 '" + filePath + "' 不存在";
        }

        String content = IOManager.readFile(sourceFile.getAbsolutePath());
        String fileName = sourceFile.getName();
        File scriptsDir = DataBridge.getScriptsDirectory();
        scriptsDir.mkdirs();
        File destFile = new File(scriptsDir, fileName);

        if (destFile.exists()) {
            return "错误: 文件 '" + fileName + "' 已存在\n" +
                    "提示: 使用 'script delete " + fileName + "' 删除旧文件";
        }

        IOManager.createDirectory(destFile.getParentFile().getAbsolutePath());
        IOManager.writeFile(destFile.getAbsolutePath(), content);

        return "文件导入成功\n" +
                "源文件: " + sourceFile.getAbsolutePath() + "\n" +
                "文件名称: " + fileName + "\n" +
                "目标路径: " + destFile.getAbsolutePath();
    }

    private String handleExport(String[] args) throws IOException {
        if (args.length < 3) {
            return "错误: 需要指定文件名称和导出路径\n用法: script export <name> <file>";
        }

        String fileName = args[1];
        String exportPath = args[2];
        File sourceFile = null;
        String fileType = null;
        
        File scriptFile = getScriptFile(fileName);
        if (scriptFile.exists()) {
            sourceFile = scriptFile;
            fileType = "脚本";
        } else {
            File codebaseDir = DataBridge.getCodebaseDirectory();
            File codebaseFile = new File(codebaseDir, fileName);
            if (codebaseFile.exists()) {
                sourceFile = codebaseFile;
                fileType = "Codebase文件";
            }
        }
        
        if (sourceFile == null) {
            return "错误: 文件 '" + fileName + "' 不存在（已检查脚本和codebase目录）";
        }

        File exportFile = new File(exportPath);
        String content = IOManager.readFile(sourceFile.getAbsolutePath());

        IOManager.writeFile(exportFile.getAbsolutePath(), content);

        return fileType + "导出成功\n" +
                "文件: " + fileName + "\n" +
                "导出路径: " + exportFile.getAbsolutePath();
    }

    private String handleManage(CommandExecutor.CmdExecContext context) {
        context.output().println("===== 交互式脚本管理器 =====");
        context.output().println("输入 'help' 查看可用命令, 'exit' 或 'quit' 退出\n");
        
        while (true) {
            String input = context.readLine("manage> ");
            
            if (input == null) {
                break;
            }
            
            input = input.trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            if (input.equals("exit") || input.equals("quit") || input.equals("0")) {
                context.output().println("退出脚本管理器");
                break;
            }
            
            if (input.equals("help") || input.equals("?")) {
                showManageHelp(context);
                continue;
            }
            
            String result = handleManageCommand(input, context);
            if (result != null) {
                context.output().println(result);
            }
        }
        
        return "脚本管理器已退出";
    }
    
    private void showManageHelp(CommandExecutor.CmdExecContext context) {
        context.output().println("");
        context.output().println("可用命令:");
        context.output().println("  create <name>        - 创建新脚本");
        context.output().println("  list                 - 列出所有脚本和codebase文件");
        context.output().println("  show <name>          - 显示文件内容");
        context.output().println("  edit <name>          - 编辑脚本内容");
        context.output().println("  delete <name>        - 删除文件");
        context.output().println("  run <name>           - 执行脚本或codebase文件");
        context.output().println("  import <path>        - 导入文件");
        context.output().println("  export <name> <path> - 导出文件");
        context.output().println("  codebase             - 列出codebase文件");
        context.output().println("  help                 - 显示此帮助");
        context.output().println("  exit / quit          - 退出管理器");
        context.output().println("");
    }
    
    private String handleManageCommand(String input, CommandExecutor.CmdExecContext context) {
        String[] parts = input.split("\\s+", 3);
        String cmd = parts[0].toLowerCase();
        
        try {
            switch (cmd) {
                case "1":
                case "create": {
                    if (parts.length < 2) {
                        return "用法: create <name>";
                    }
                    return handleCreate(new String[]{"create", parts[1]});
                }
                case "2":
                case "list": {
                    return handleList();
                }
                case "3":
                case "show": {
                    if (parts.length < 2) {
                        return "用法: show <name>";
                    }
                    return handleShow(new String[]{"show", parts[1]});
                }
                case "edit": {
                    if (parts.length < 2) {
                        return "用法: edit <name>";
                    }
                    return handleEdit(parts[1], context);
                }
                case "4":
                case "delete": {
                    if (parts.length < 2) {
                        return "用法: delete <name>";
                    }
                    return handleDelete(new String[]{"delete", parts[1]});
                }
                case "5":
                case "run": {
                    if (parts.length < 2) {
                        return "用法: run <name>";
                    }
                    return handleRun(new String[]{"run", parts[1]}, context);
                }
                case "6":
                case "import": {
                    if (parts.length < 2) {
                        return "用法: import <path>";
                    }
                    return handleImport(new String[]{"import", parts[1]});
                }
                case "7":
                case "export": {
                    if (parts.length < 3) {
                        return "用法: export <name> <path>";
                    }
                    return handleExport(new String[]{"export", parts[1], parts[2]});
                }
                case "codebase": {
                    return handleCodebaseList();
                }
                default:
                    return "未知命令: " + cmd + " (输入 'help' 查看帮助)";
            }
        } catch (Exception e) {
            return "错误: " + e.getMessage();
        }
    }
    
    private String handleEdit(String name, CommandExecutor.CmdExecContext context) throws IOException {
        File scriptFile = getScriptFile(name);
        
        if (!scriptFile.exists()) {
            return "错误: 脚本 '" + name + "' 不存在";
        }
        
        String existingContent = IOManager.readFile(scriptFile.getAbsolutePath());
        context.output().println("编辑脚本: " + name);
        context.output().println("当前内容 (输入空行结束编辑):\n");
        context.output().println(existingContent);
        context.output().println("\n--- 开始编辑 (输入空行保存并退出) ---\n");
        
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
            return "脚本已保存";
        } else {
            return "编辑已取消 (未做更改)";
        }
    }
    
    private String handleCodebaseList() {
        File codebaseDir = DataBridge.getCodebaseDirectory();
        
        if (!codebaseDir.exists()) {
            return "Codebase目录不存在";
        }
        
        File[] files = codebaseDir.listFiles();
        if (files == null || files.length == 0) {
            return "Codebase目录为空";
        }
        
        StringBuilder result = new StringBuilder();
        result.append("===== Codebase文件列表 =====\n\n");
        
        for (File file : files) {
            result.append("  ").append(file.getName());
            if (file.isDirectory()) {
                result.append("/");
            }
            result.append("\n");
        }
        
        result.append("\n总计: ").append(files.length).append(" 个文件");
        return result.toString();
    }

    private File getScriptFile(String scriptName) {
        File scriptsDir = DataBridge.getScriptsDirectory();
        return new File(scriptsDir, scriptName + ".java");
    }

    private boolean isValidScriptName(String name) {
        return name != null && name.matches("^[a-zA-Z0-9_]+$");
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.2f KB", bytes / 1024.0);
        } else if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", bytes / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        }
    }

    private String formatTime(long timestamp) {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new java.util.Date(timestamp));
    }

    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED = "\u001B[31m";
    private static final String ANSI_ORANGE = "\u001B[33m";
    private static final String ANSI_GRAY = "\u001B[90m";

    private String runInteractiveMode(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        ScriptRunner runner = getScriptExecutor(classLoader);
        
        logger.info("进入交互式脚本执行模式");
        context.output().println("====== 脚本交互执行模式 =====");
        context.output().println("输入 'exit' 或 'quit' 退出 (你不会闲到拿这俩做变量名, 对吧)");
        context.output().println("输入 ':multi' 进入多行模式, ':eval' 执行, ':clear' 清空");
        context.output().println("输入 'setPrintAST(true)' 开启 AST 打印");
        context.output().println("");

        StringBuilder multiLineBuffer = new StringBuilder();
        boolean multiLineMode = false;
        boolean autoMultiLine = false;

        while (true) {
            String prompt = (multiLineMode || autoMultiLine) ? "... " : ">>> ";
            String code = context.readLine(prompt);
            
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
                if (autoMultiLine) {
                    continue;
                }
                continue;
            }
            
            if (code.equals(":multi")) {
                multiLineMode = true;
                autoMultiLine = false;
                context.output().println("进入多行模式, 输入 ':eval' 执行, ':clear' 清空, ':exit' 退出多行模式");
                continue;
            }
            
            if (code.equals(":exit") && (multiLineMode || autoMultiLine)) {
                multiLineMode = false;
                autoMultiLine = false;
                multiLineBuffer.setLength(0);
                context.output().println("退出多行模式");
                continue;
            }
            
            if (code.equals(":clear") && (multiLineMode || autoMultiLine)) {
                multiLineBuffer.setLength(0);
                context.output().println("已清空");
                continue;
            }
            
            if (code.equals(":eval") && (multiLineMode || autoMultiLine)) {
                String fullCode = multiLineBuffer.toString();
                multiLineBuffer.setLength(0);
                multiLineMode = false;
                autoMultiLine = false;
                
                if (fullCode.isEmpty()) {
                    context.output().println("没有代码可执行");
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
        return "交互式模式已退出";
    }
    
    private void executeCode(ScriptRunner runner, String code, CommandExecutor.CmdExecContext context) {
        try {
            Object result = runner.executeWithResult(code, context.output(), context.output());
            if (result != null) {
                context.output().println(ANSI_GRAY + result.toString() + ANSI_RESET);
            }
        } catch (Exception e) {
            Throwable cause = e.getCause();
            String message = e.getMessage();
            boolean isParseError = message != null && message.startsWith("Parse error:");
            
            context.output().print(isParseError ? ANSI_ORANGE : ANSI_RED);
            
            if (cause instanceof EvaluationException evalEx) {
                Throwable innerCause = evalEx.getCause();
                if (innerCause != null) {
                    context.output().println("错误: " + evalEx.getMessage());
                    context.output().printStackTrace(innerCause);
                } else {
                    context.output().println("错误: " + evalEx.getMessage());
                }
            } else if (cause instanceof ParseException parseEx) {
                context.output().println("语法错误: " + parseEx.getMessage());
            } else if (cause != null) {
                context.output().println((isParseError ? "语法错误: " : "错误: ") + cause.getMessage());
                if (!isParseError) {
                    context.output().printStackTrace(cause);
                }
            } else {
                context.output().println((isParseError ? "语法错误: " : "错误: ") + message);
            }
            context.output().print(ANSI_RESET);
        }
    }
    
    private static boolean isCodeComplete(String code) {
        int braceCount = 0;
        int parenCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        boolean inChar = false;
        boolean inFString = false;
        boolean escape = false;
        
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
                    inFString = !inFString;
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
        
        return braceCount <= 0 && parenCount <= 0 && bracketCount <= 0;
    }

}