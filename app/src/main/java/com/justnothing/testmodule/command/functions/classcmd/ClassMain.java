package com.justnothing.testmodule.command.functions.classcmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_CLASS_VER;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

import java.util.Arrays;
import java.util.Locale;

public class ClassMain extends CommandBase {

    private final String commandName;

    public ClassMain() {
        this("class");
    }

    public ClassMain(String commandName) {
        super(commandName);
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        return switch (commandName) {
            case "cinfo" -> String.format(
                    Locale.getDefault(),
                    """
                    语法: cinfo [options] <class_name>
                    
                    查看类的详细信息.
                    
                    选项:
                        -v, --verbose     显示详细信息
                        -i, --interfaces  显示实现的接口
                        -c, --constructors 显示构造函数
                        -s, --super       显示父类信息
                        -m, --modifiers   显示修饰符信息
                        -a, --all         显示所有信息 (默认)
                    
                    示例:
                        cinfo java.lang.String
                        cinfo -i java.util.ArrayList
                        cinfo -c android.view.View
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "cgraph" -> String.format(
                    Locale.getDefault(),
                    """
                    语法: cgraph <class_name>
                    
                    生成类继承图.
                    
                    示例:
                        cgraph java.util.ArrayList
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "canalyze" -> String.format(
                    Locale.getDefault(),
                    """
                    语法: canalyze [options] <class_name>
                    
                    分析类的字段和方法.
                    
                    选项:
                        -f, --fields      只显示字段
                        -m, --methods     只显示方法
                        -a, --all         显示所有信息 (默认)
                    
                    示例:
                        canalyze java.lang.String
                        canalyze -f com.android.server.am.ActivityManagerService
                        canalyze -a java.util.ArrayList
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "cinvoke" -> String.format(
                    Locale.getDefault(),
                    """
                    语法: cinvoke <class> <method> [params...]
                    
                    调用某个类中的单一方法.
                    提供参数的格式: Type:value (e.g. Integer:114514)
                    
                    示例:
                        cinvoke java.lang.Integer parseInt String:"123"
                        cinvoke android.app.ActivityThread currentActivityThread
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "cfield" -> String.format(
                    Locale.getDefault(),
                    """
                    语法: cfield [options] <class_name> [field_name]
                    
                    查看类的字段详细信息或获取/设置字段值.
                    
                    选项:
                        -g, --get <值>    获取字段值 (需要提供字段名)
                        -s, --set <值>    设置字段值 (需要提供字段名)
                        -v, --value       显示字段值
                        -t, --type        显示字段类型
                        -m, --modifiers   显示修饰符
                        -a, --all         显示所有信息 (默认)
                    
                    示例:
                        cfield java.lang.String
                        cfield -g java.lang.System out
                        cfield -s java.lang.System out "test"
                        cfield -v com.example.MyClass myField
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "clist" -> String.format(
                    Locale.getDefault(),
                    """
                        语法: clist [options] <class>
                    
                        列出一个类的所有方法.
                    
                        可选项:
                            -v, --verbose      详细输出完整类名
                    
                    示例:
                        clist -v java.lang.String
                        clist com.android.server.am.ActivityManagerService
                    
                        (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "csearch" -> String.format(
                    Locale.getDefault(),
                    """
                    语法: csearch <subcmd> <pattern>
                    
                    搜索类, 方法, 字段或注解.
                    
                    子命令:
                        class <pattern>                - 搜索类名
                        method <pattern>               - 搜索方法名
                        field <pattern>                - 搜索字段名
                        annotation <pattern>           - 搜索注解
                    
                    选项:
                        pattern - 搜索模式, 支持通配符(*)
                    
                    示例:
                        csearch class *Activity
                        csearch method onCreate
                        csearch field m*
                        csearch annotation Override
                    
                    注意:
                        - 搜索在已加载的类中进行
                        - 支持通配符*匹配任意字符
                        - 搜索结果包含完整类名和成员信息
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "cconstructor" -> String.format("""
                    语法: cconstructor <class_name> [params...]
                    
                    创建类的实例.
                    提供参数的格式: Type:value (e.g. Integer:114514)
                    
                    示例:
                        cconstructor java.lang.Integer Integer:"123"
                        cconstructor java.lang.String String:"hello"
                        cconstructor java.util.ArrayList Integer:"10"
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
            default -> String.format(
                    Locale.getDefault(),
                    """
                    语法: class <subcmd> [args...]
                    
                    查看类的详细信息, 包括继承关系, 接口, 构造函数等.
                    (输入 class <subcmd> 查看子命令帮助)
                    
                    注意:
                        - 所有操作默认支持访问私有成员 (使用 setAccessible(true))
                        - 使用 --super 选项访问父类成员
                        - 使用 --interfaces 选项访问接口成员
                        - 方法参数必须使用 Type:value 格式
                    
                    子命令:
                        info [options] <class_name>                    - 查看类的详细信息
                        graph <class_name>                             - 生成类继承图
                        analyze [options] <class_name>                 - 分析类的字段和方法
                        list [options] <class_name>                    - 列出一个类的所有方法
                        invoke [options] <class_name> <method_name> [params...]  - 调用类中的方法
                        field [options] <class_name> [field_name]      - 查看或操作字段
                        constructor <class_name> [params...]           - 创建类的实例
                        search <subcmd> <pattern>                      - 搜索类, 方法, 字段或注解
                        reflect <class> <type> <name> [options]       - 使用反射访问和操作类的私有成员
                    
                    info 选项:
                        -v, --verbose       显示详细信息
                        -i, --interfaces    显示实现的接口
                        -c, --constructors  显示构造函数
                        -s, --super         显示父类信息
                        -m, --modifiers     显示修饰符信息
                        -a, --all           显示所有信息 (默认)
                    
                    analyze 选项:
                        -v, --verbose     显示详细信息
                        -f, --fields      只显示字段
                        -m, --methods     只显示方法
                        -a, --all         显示所有信息 (默认)
                    
                    list 选项:
                        -v, --verbose    详细输出完整类名
                    
                    invoke 选项:
                        --super           访问父类成员
                        --interfaces      访问接口成员
                    
                    invoke所需的参数格式: Type:value (e.g. Integer:114514, 目前只支持简单类型)
                    
                    field 选项:
                        -g, --get <值>    获取字段值
                        -s, --set <值>    设置字段值
                        -v, --value       显示字段值
                        -t, --type        显示字段类型
                        -m, --modifiers   显示修饰符
                        -a, --all         显示所有信息 (默认)
                        --super           访问父类成员
                        --interfaces      访问接口成员
                    
                    constructor 参数格式: Type:value (e.g. Integer:114514, 目前只支持简单类型)
                    
                    search 子命令:
                        class <pattern>                - 搜索类名
                        method <pattern>               - 搜索方法名
                        field <pattern>                - 搜索字段名
                        annotation <pattern>           - 搜索注解
                    
                    reflect 子命令:
                        语法: reflect <class> <type> <name> [options]
                    
                        使用统一的反射接口访问和操作类的私有成员。
                    
                        类型 (type参数) 说明:
                            field        - 获取/设置字段值
                            method       - 调用方法
                            constructor  - 创建实例
                            static       - 访问静态成员
                    
                        选项:
                            -v, --value <value>      设置字段值
                            -p, --params <args>      方法参数（空格分隔）
                            -s, --super             访问父类成员
                            -i, --interfaces         访问接口成员
                            -r, --raw                原始输出（不格式化）
                    
                        示例:
                            class reflect java.lang.System field out
                            class reflect java.lang.Integer method parseInt -p "String:\\"114514\\""
                            class reflect java.lang.String constructor -p "Integer:1919810"
                            class reflect java.lang.System static out
                    
                    快捷命令:
                        cinfo       - 等同于 class info
                        cgraph      - 等同于 class graph
                        canalyze    - 等同于 class analyze
                        clist       - 等同于 class list
                        cinvoke     - 等同于 class invoke
                        cfield      - 等同于 class field
                        cconstructor - 等同于 class constructor
                        csearch     - 等同于 class search
                        creflect    - 等同于 class reflect
                    
                    示例:
                        class info java.lang.String
                        class info -i java.util.ArrayList
                        class info -c android.view.View
                        class graph java.util.ArrayList
                        class analyze java.lang.String
                        class analyze -f com.android.server.am.ActivityManagerService
                        class analyze -a java.util.ArrayList
                        class list -v java.lang.String
                        class list com.android.server.am.ActivityManagerService
                        class invoke java.lang.Integer parseInt String:"123"
                        class invoke android.app.ActivityThread currentActivityThread
                        class field java.lang.String
                        class field -g java.lang.System out
                        class field -s java.lang.System out "test"
                        class field -v com.example.MyClass myField
                        class constructor java.lang.Integer Integer:114514
                        class constructor java.lang.String String:"hello"
                        class constructor java.util.ArrayList Integer:1 Integer:2 Integer:3
                        class search class *Activity
                        class search method onCreate
                        class search field m*
                        class search annotation Override
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
        };
    }

    @Override
    public void runMain(CommandExecutor.CmdExecContext context) {
        String cmdName = context.cmdName();
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        
        logger.debug("执行class命令，参数: " + Arrays.toString(args));
        logger.debug("目标包: " + targetPackage + ", 类加载器: " + classLoader);

        String subCommand;
        String[] subArgs;

        if (cmdName.equals("class")) {
            if (args.length < 1) {
                logger.warn("参数不足，需要至少1个参数");
                context.println(getHelpText(), Colors.WHITE);
                return;
            }
            subCommand = args[0];
            subArgs = Arrays.copyOfRange(args, 1, args.length);
        } else {
            subCommand = getSubCommandFromAlias(cmdName);
            subArgs = args;
        }

        try {
            ClassCommand command = ClassCommandRegistry.getCommand(subCommand);
            if (command == null) {
                handleLegacyCommand(cmdName, context);
                return;
            }

            ClassCommandContext cmdContext = new ClassCommandContext(
                subArgs, classLoader, targetPackage, context, logger
            );
            
            command.execute(cmdContext);
        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "class " + subCommand, 
                e, 
                context,
                "执行class命令失败"
            );
        }
    }

    private String getSubCommandFromAlias(String alias) {
        return switch (alias) {
            case "cinfo" -> "info";
            case "cgraph" -> "graph";
            case "canalyze" -> "analyze";
            case "clist" -> "list";
            case "cinvoke" -> "invoke";
            case "cfield" -> "field";
            case "csearch" -> "search";
            case "cconstructor" -> "constructor";
            case "creflect" -> "reflect";
            default -> alias;
        };
    }

    private void handleLegacyCommand(String cmdName, CommandExecutor.CmdExecContext context) {
        context.println("命令 '" + cmdName + "' 还未迁移到新架构或者根本没有这个命令", Colors.RED);
        context.println(getHelpText(), Colors.WHITE);
    }
}
