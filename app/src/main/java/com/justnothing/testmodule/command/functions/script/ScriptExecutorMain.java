package com.justnothing.testmodule.command.functions.script;

import static com.justnothing.testmodule.constants.CommandServer.CMD_SCRIPT_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.utils.data.DataBridge;
import com.justnothing.testmodule.utils.io.IOManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
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
                    
                    注, 支持的内置函数:
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
                        sinteractive
                    
                    (Submodule script %s)
                    """, CMD_SCRIPT_VER);
        } else {
            return String.format("""
                    语法: script <subcmd> [args...] | script <code>
                    
                    用JustNothing1021写的解释器跑Java代码.
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
                        interactive                - 启动交互式脚本执行模式
                
                    选项:
                        name              - 脚本名称
                        file              - 文件路径


                    脚本语法说明:
                        
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
                        
                        更多示例:
                            String b = new String("1919810");
                            String a = "114514"; Log.i("Tag", a + " " + b);
                            int x = (10 + 5) * 2;
                            for (int i = 0; i < 114; i++) println(i);
                            int[][] arr = new int[3][3] {new int[]{1, 2, 3}, new int[]{4, 5, 6}, new int[]{7, 8, 9}}; println(arr);
                    
                    示例:
                        script create my_hook
                        script edit my_hook
                        script list
                        script show my_hook
                        script run my_hook
                        script import /sdcard/script.java
                        script import /sdcard/code.java codebase
                        script export my_hook /sdcard/backup.java
                        script delete my_hook
                        script interactive
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
        
        if (cmdName.equals("sclear")) {
            return clearExecutorVariables(context);
        } else if (cmdName.equals("svars")) {
            return listExecutorVariables(context);
        } else if (cmdName.equals("srun")) {
            return executeScriptCode(context);
        } else if (cmdName.equals("sinteractive")) {
            return runInteractiveMode(context);
        } else if (cmdName.equals("script")) {
            String[] args = context.args();
            if (args.length == 0) return getHelpText();
            try {
                return handleScriptManagerCommand(context);
            } catch (Exception e) {
                logger.error("执行script命令失败", e);
                return "错误: " + e.getMessage() +
                        "\n堆栈追踪: \n" + Log.getStackTraceString(e);
            }

        }
        
        return getHelpText();
    }

    private String executeScriptCode(CommandExecutor.CmdExecContext context) {
        try {
            logger.info("执行脚本代码: " + context.origCommand());
            TestInterpreter.ScriptRunner runner = getScriptExecutor(context.classLoader());
            runner.execute(context.origCommand().toString(), context.output(), context.output());
            return "";
        } catch (Exception e) {
            logger.error("脚本执行失败", e);
            return "脚本执行失败: " + e.getMessage() + "\n堆栈追踪: \n" + android.util.Log.getStackTraceString(e);
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
            case "edit":
                return handleEdit(args);
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
            case "interactive":
                return handleInteractive(context);
            default:
                return "未知子命令: " + subCommand + "\n" + getHelpText();
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

        scriptFile.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(scriptFile))) {
            writer.write("// Script: " + scriptName);
            writer.newLine();
            writer.write("// Created by: " + System.currentTimeMillis());
            writer.newLine();
            writer.newLine();
            writer.write("// 在这里编写你的脚本代码...");
            writer.newLine();
        }

        return "脚本 '" + scriptName + "' 创建成功\n" +
                "路径: " + scriptFile.getAbsolutePath() + "\n" +
                "提示: 使用 'script edit " + scriptName + "' 编辑脚本";
    }

    private String handleEdit(String[] args) throws IOException {
        if (args.length < 2) {
            return "错误: 需要指定脚本名称\n用法: script edit <name>";
        }

        String scriptName = args[1];
        File scriptFile = getScriptFile(scriptName);

        if (!scriptFile.exists()) {
            return "错误: 脚本 '" + scriptName + "' 不存在";
        }

        return "脚本 '" + scriptName + "' 已准备好编辑\n" +
                "路径: " + scriptFile.getAbsolutePath() + "\n" +
                "提示: 使用外部编辑器编辑脚本文件";
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
            TestInterpreter.ScriptRunner runner = new TestInterpreter.ScriptRunner(context.classLoader());
            runner.execute(content, context.output(), context.output());
            result.append("脚本执行成功");
        } catch (Exception e) {
            result.append("脚本执行失败: ").append(e.getMessage());
            result.append("\n堆栈追踪:\n").append(Log.getStackTraceString(e));
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
            TestInterpreter.ScriptRunner runner = new TestInterpreter.ScriptRunner(context.classLoader());
            runner.execute(code, context.output(), context.output());
            result.append("代码执行成功");
        } catch (Exception e) {
            result.append("代码执行失败: ").append(e.getMessage());
            result.append("\n堆栈追踪:\n").append(Log.getStackTraceString(e));
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

        destFile.getParentFile().mkdirs();
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destFile))) {
            writer.write(content);
        }

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

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(exportFile))) {
            writer.write(content);
        }

        return fileType + "导出成功\n" +
                "文件: " + fileName + "\n" +
                "导出路径: " + exportFile.getAbsolutePath();
    }

    private String handleInteractive(CommandExecutor.CmdExecContext context) {
        StringBuilder result = new StringBuilder();
        result.append("===== 交互式脚本管理器 =====\n\n");
        result.append("可用命令:\n");
        result.append("  1. create - 创建新脚本\n");
        result.append("  2. edit - 编辑脚本\n");
        result.append("  3. list - 列出所有脚本和codebase文件\n");
        result.append("  4. show - 显示文件内容\n");
        result.append("  5. delete - 删除文件\n");
        result.append("  6. run - 执行脚本或codebase文件\n");
        result.append("  7. import - 导入文件（可指定codebase）\n");
        result.append("  8. export - 导出文件\n");
        result.append("  0. exit - 退出\n\n");
        result.append("提示: 输入命令编号或命令名称\n");
        
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

    private String runInteractiveMode(CommandExecutor.CmdExecContext context) {
        ClassLoader classLoader = context.classLoader();
        TestInterpreter.ScriptRunner runner = getScriptExecutor(classLoader);
        
        logger.info("进入交互式脚本执行模式");
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
                logger.error("脚本执行失败", e);
            }
        }
        return "交互式模式已退出";
    }

}