package com.justnothing.testmodule.command.functions.classcmd;

import com.justnothing.testmodule.command.base.RegisterCommand;
import com.justnothing.testmodule.command.base.SupportsRequests;
import com.justnothing.testmodule.command.base.RegisterParser;
import static com.justnothing.testmodule.constants.CommandServer.CMD_CLASS_VER;

import com.justnothing.testmodule.command.base.CommandRequest;
import com.justnothing.testmodule.command.base.MainCommand;
import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.classcmd.parser.ClassCommandParser;
import com.justnothing.testmodule.command.functions.classcmd.request.AnalyzeClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassGraphRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassHierarchyRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ClassInfoRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.GetFieldValueRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeConstructorRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.InvokeMethodRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.MethodListRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.ReflectClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.SearchClassRequest;
import com.justnothing.testmodule.command.functions.classcmd.request.SetFieldValueRequest;
import com.justnothing.testmodule.command.output.Colors;
import com.justnothing.testmodule.command.utils.CommandExceptionHandler;

import java.util.Arrays;
import java.util.Locale;

@RegisterCommand("class")
@SupportsRequests({
    ClassInfoRequest.class,
    ClassGraphRequest.class,
    ClassHierarchyRequest.class,
    AnalyzeClassRequest.class,
    GetFieldValueRequest.class,
    InvokeConstructorRequest.class,
    InvokeMethodRequest.class,
    MethodListRequest.class,
    ReflectClassRequest.class,
    SearchClassRequest.class,
    SetFieldValueRequest.class
})
@RegisterParser(ClassCommandParser.class)
public class ClassMain extends MainCommand<ClassCommandRequest, ClassCommandResult> {



    public ClassMain() {
        super("class", ClassCommandResult.class);
    }

    public ClassMain(String commandName) {
        super(commandName, ClassCommandResult.class);
    }


    @Override
    public String getHelpText() {
        return String.format(
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
    }


    @Override
    public ClassCommandResult runMain(CommandExecutor.CmdExecContext<CommandRequest> context) throws Exception {
        String cmdName = context.cmdName();
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        
        logger.debug("执行class命令，参数: " + Arrays.toString(args));
        logger.debug("目标包: " + targetPackage + ", 类加载器: " + classLoader);

        String subCommand;

        if (args.length < 1) {
            logger.warn("参数不足，需要至少1个参数");
            context.println(getHelpText(), Colors.WHITE);
            return createErrorResult("参数不足，需要指定子命令");
        }
        subCommand = args[0];


        try {
            ClassCommand<? extends ClassCommandRequest, ? extends ClassCommandResult>
                    command = ClassCommandRegistry.getCommand(subCommand);
            if (command == null) {
                handleLegacyCommand(cmdName, context);
                return createErrorResult("未知子命令: " + subCommand);
            }

            return command.execute(context);
        } catch (Exception e) {
            CommandExceptionHandler.handleException(
                "class " + subCommand, 
                e, 
                context,
                "执行class命令失败"
            );
            return createErrorResult("执行class命令失败: " + e.getMessage());
        }
    }

    private void handleLegacyCommand(String cmdName, CommandExecutor.CmdExecContext<?> context) {
        context.println("命令 '" + cmdName + "' 还未迁移到新架构或者根本没有这个命令", Colors.RED);
        context.println(getHelpText(), Colors.WHITE);
    }
}
