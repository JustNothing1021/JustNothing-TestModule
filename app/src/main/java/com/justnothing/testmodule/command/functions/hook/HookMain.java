package com.justnothing.testmodule.command.functions.hook;

import static com.justnothing.testmodule.constants.CommandServer.CMD_HOOK_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.utils.data.DataBridge;

public class HookMain extends CommandBase {

    public HookMain() {
        super("HookMain");
    }

    private String stripQuotes(String str) {
        if (str == null) {
            return null;
        }
        str = str.trim();
        if (str.startsWith("\"") && str.endsWith("\"")) {
            return str.substring(1, str.length() - 1);
        }
        if (str.startsWith("'") && str.endsWith("'")) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    @Override
    public String getHelpText() {
        return String.format("""
                语法: hook <subcmd> [args...]
                
                动态Hook注入器，通过脚本灵活地实现 Hook 功能.
                
                子命令:
                    add <class_name> <method_name> [sig <signature>] 
                            [before (code <code> | codebase <file>)] 
                            [after (code <code> | codebase <file>)] 
                            [replace (code <code> | codebase <file>)]
                                                - 添加Hook，每个阶段可以单独指定code或codebase
                    remove <id>                 - 移除指定 Hook
                    list                        - 列出所有 Hook
                    info <id>                   - 显示 Hook 详细信息
                    output <id> [count]         - 获取 Hook 输出
                    enable <id>                 - 启用Hook
                    disable <id>                - 禁用Hook
                    clear                       - 清除所有Hook
                
                选项:
                    sig              - 指定方法签名，如 "String,int" 表示(String, int)参数的方法
                    before code      - 在方法调用前执行的代码
                    before codebase  - 包含 before 代码的文件，可以是文件路径或script脚本名称
                    after code       - 在方法调用后执行的代码
                    after codebase   - 包含 after 代码的文件，可以是文件路径或script脚本名称
                    replace code     - 替换方法执行的代码（会覆盖原方法）
                    replace codebase - 包含 replace 代码的文件，可以是文件路径或script脚本名称
                
                示例:
                    hook add com.example.MainActivity onCreate before code 'println("onCreate called");'
                    hook add com.example.MainActivity onCreate before codebase activity_lifecycle_before.java
                    hook add com.example.MainActivity onCreate before codebase my_before_script
                    hook add com.example.MyClass myMethod sig "int,String" before code 'println("myMethod called");'
                    hook add com.example.Utils getValue replace code 'result = 999;'
                    hook add com.example.MainActivity onCreate before code 'println("before");' after code 'println("after");'
                    hook add com.example.MainActivity onCreate before codebase my_before_script after codebase my_after_script
                    hook list
                    hook remove hook_1
                    hook enable hook_1
                    hook disable hook_1
                
                提示:
                    - Hook代码使用script的语法（基本就和Java没啥区别）
                    - codebase参数支持两种方式：
                        1. 脚本名称（如：activity_lifecycle_before.java，会自动查找scripts目录）
                        2. 完整文件路径（如：/data/data/com.test/scripts/before.java）
                    - 默认 scripts 目录位于: %s
                    - Hook在后台运行，不会阻塞其他命令执行
                    - 每个阶段只能指定一次，不能重复
                    - 每个Hook会记录调用次数和详细信息
                    - 有很多内置函数，如下:
                        getMethodHookParam() -> MethodHookParam   - 获取当前Hook的参数
                        getLoadPackageParam() -> LoadPackageParam - 获取当前Hook的加载包参数
                        getHookInfo() -> HookInfo                 - 获取当前Hook的信息
                        getPhase() -> String                      - 获取当前Hook的阶段
                        getHookId() -> String                     - 获取当前Hook的ID
                        setReturnValue() -> void                  - 设置返回值
                        setThrowable() -> void                    - 抛出异常
                        getReturnValue() -> Object                - 获取返回值

                
                (Submodule hook %s)
                """, DataBridge.getCodebaseDirectory().getAbsolutePath(), CMD_HOOK_VER);
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String[] args = context.args();
        
        if (args.length < 1) {
            return getHelpText();
        }

        String subCommand = args[0];

        try {
            switch (subCommand) {
                case "add":
                    return handleAdd(args, context);
                case "remove":
                    return handleRemove(args);
                case "list":
                    return handleList();
                case "info":
                    return handleInfo(args);
                case "output":
                    return handleOutput(args);
                case "enable":
                    return handleEnable(args);
                case "disable":
                    return handleDisable(args);
                case "clear":
                    return handleClear();
                default:
                    return "未知子命令: " + subCommand + "\n" + getHelpText();
            }
        } catch (Exception e) {
            logger.error("Hook 命令执行失败", e);
            return "Hook 命令执行失败: " + e.getMessage() + "\n" + 
                   "堆栈追踪:\n" + android.util.Log.getStackTraceString(e);
        }
    }

    private String handleAdd(String[] args, CommandExecutor.CmdExecContext context) {
        if (args.length < 3) {
            return "错误: 需要指定类名和方法名\n用法: hook add <类名> <方法名> [sig <签名>] [before code <代码>|before codebase <文件>] [after code <代码>|after codebase <文件>] [replace code <代码>|replace codebase <文件>]";
        }

        String className = stripQuotes(args[1]);
        String methodName = stripQuotes(args[2]);
        String signature = null;
        String beforeCode = null;
        String afterCode = null;
        String replaceCode = null;
        String beforeCodebase = null;
        String afterCodebase = null;
        String replaceCodebase = null;

        int i = 3;
        while (i < args.length) {
            String arg = args[i];
            
            if (arg.equals("sig") && i + 1 < args.length) {
                signature = stripQuotes(args[i + 1]);
                i += 2;
            } else if (arg.equals("before") && i + 2 < args.length) {
                if (beforeCode != null || beforeCodebase != null) {
                    return "错误: before 阶段已经指定过，不能重复指定";
                }
                String type = args[i + 1];
                String value = args[i + 2];
                if (type.equals("code")) {
                    beforeCode = value;
                } else if (type.equals("codebase")) {
                    beforeCodebase = value;
                } else {
                    return "错误: before 参数必须为 'code' 或 'codebase'";
                }
                i += 3;
            } else if (arg.equals("after") && i + 2 < args.length) {
                if (afterCode != null || afterCodebase != null) {
                    return "错误: after 阶段已经指定过，不能重复指定";
                }
                String type = args[i + 1];
                String value = args[i + 2];
                if (type.equals("code")) {
                    afterCode = value;
                } else if (type.equals("codebase")) {
                    afterCodebase = value;
                } else {
                    return "错误: after 参数必须为 'code' 或 'codebase'";
                }
                i += 3;
            } else if (arg.equals("replace") && i + 2 < args.length) {
                if (replaceCode != null || replaceCodebase != null) {
                    return "错误: replace 阶段已经指定过，不能重复指定";
                }
                String type = args[i + 1];
                String value = args[i + 2];
                if (type.equals("code")) {
                    replaceCode = value;
                } else if (type.equals("codebase")) {
                    replaceCodebase = value;
                } else {
                    return "错误: replace 参数必须为 'code' 或 'codebase'";
                }
                i += 3;
            } else {
                return "错误: 无效参数 '" + arg + "'\n" + getHelpText();
            }
        }

        if (beforeCode == null && afterCode == null && replaceCode == null && 
            beforeCodebase == null && afterCodebase == null && replaceCodebase == null) {
            return "错误: 需要指定至少一个 Hook 阶段（before/after/replace）";
        }

        return HookManager.addHook(className, methodName, signature, 
                                  beforeCode, afterCode, replaceCode,
                                  beforeCodebase, afterCodebase, replaceCodebase,
                                  context.classLoader());
    }

    private String handleRemove(String[] args) {
        if (args.length < 2) {
            return "错误: 需要指定 Hook ID\n用法: hook remove <id>";
        }

        String hookId = args[1];
        return HookManager.removeHook(hookId);
    }

    private String handleList() {
        return HookManager.listHooks();
    }

    private String handleInfo(String[] args) {
        if (args.length < 2) {
            return "错误: 需要指定 Hook ID\n用法: hook info <id>";
        }

        String hookId = args[1];
        return HookManager.getHookInfo(hookId);
    }

    private String handleOutput(String[] args) {
        if (args.length < 2) {
            return "错误: 需要指定 Hook ID\n用法: hook output <id> [count]";
        }

        String hookId = args[1];
        int count = 10;
        
        if (args.length >= 3) {
            try {
                count = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                return "错误: count 必须是数字";
            }
        }

        return HookManager.getHookOutput(hookId, count);
    }

    private String handleEnable(String[] args) {
        if (args.length < 2) {
            return "错误: 需要指定 Hook ID\n用法: hook enable <id>";
        }

        String hookId = args[1];
        return HookManager.enableHook(hookId);
    }

    private String handleDisable(String[] args) {
        if (args.length < 2) {
            return "错误: 需要指定 Hook ID\n用法: hook disable <id>";
        }

        String hookId = args[1];
        return HookManager.disableHook(hookId);
    }

    private String handleClear() {
        int count = HookManager.getHookCount();
        HookManager.clearAllHooks();
        return "已清除 " + count + " 个 Hook";
    }
}
