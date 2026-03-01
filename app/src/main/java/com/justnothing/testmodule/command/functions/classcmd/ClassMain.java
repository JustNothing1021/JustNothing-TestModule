package com.justnothing.testmodule.command.functions.classcmd;

import static com.justnothing.testmodule.constants.CommandServer.CMD_CLASS_VER;

import android.util.Log;

import com.justnothing.testmodule.command.CommandExecutor;
import com.justnothing.testmodule.command.functions.CommandBase;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import de.robv.android.xposed.XposedHelpers;

public class ClassMain extends CommandBase {

    private final String commandName;

    public ClassMain() {
        this("class");
    }

    public ClassMain(String commandName) {
        super("ClassExecutor");
        this.commandName = commandName;
    }

    @Override
    public String getHelpText() {
        return switch (commandName) {
            case "cinfo" -> String.format("""
                    语法: cinfo [options] <class_name>
                    
                    查看类的详细信息.
                    
                    选项:
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
            case "cgraph" -> String.format("""
                    语法: cgraph <class_name>
                    
                    生成类继承图.
                    
                    示例:
                        cgraph java.util.ArrayList
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "canalyze" -> String.format("""
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
            case "cinvoke" -> String.format("""
                    语法: cinvoke <class> <method> [params...]
                    
                    调用某个类中的单一方法.
                    提供参数的格式: Type:value (e.g. Integer:114514)
                    
                    示例:
                        cinvoke java.lang.Integer parseInt String:"123"
                        cinvoke android.app.ActivityThread currentActivityThread
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "cfield" -> String.format("""
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
            case "clist" -> String.format("""
                        语法: clist [options] <class>
                    
                        列出一个类的所有方法.
                    
                        可选项:
                            -vb, --verbose      详细输出完整类名
                    
                    示例:
                        clist -vb java.lang.String
                        clist com.android.server.am.ActivityManagerService
                    
                        (Submodule class %s)
                    """, CMD_CLASS_VER);
            case "csearch" -> String.format("""
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
            default -> String.format("""
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
                    
                    info 选项:
                        -i, --interfaces    显示实现的接口
                        -c, --constructors  显示构造函数
                        -s, --super         显示父类信息
                        -m, --modifiers     显示修饰符信息
                        -a, --all           显示所有信息 (默认)
                    
                    analyze 选项:
                        -f, --fields      只显示字段
                        -m, --methods     只显示方法
                        -a, --all         显示所有信息 (默认)
                    
                    list 选项:
                        -vb, --verbose    详细输出完整类名
                    
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
                        
                        使用统一的反射接口访问和操作类的私有成员.
                        
                        类型说明:
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
                            class reflect java.lang.Integer method parseInt -p "String:\"123\""
                            class reflect java.lang.String constructor -p "String:\"hello\""
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
                        class list -vb java.lang.String
                        class list com.android.server.am.ActivityManagerService
                        class invoke java.lang.Integer parseInt String:"123"
                        class invoke android.app.ActivityThread currentActivityThread
                        class field java.lang.String
                        class field -g java.lang.System out
                        class field -s java.lang.System out "test"
                        class field -v com.example.MyClass myField
                        class constructor java.lang.Integer Integer:"123"
                        class constructor java.lang.String String:"hello"
                        class constructor java.util.ArrayList Integer:"10"
                        class search class *Activity
                        class search method onCreate
                        class search field m*
                        class search annotation Override
                    
                    (Submodule class %s)
                    """, CMD_CLASS_VER);
        };
    }

    @Override
    public String runMain(CommandExecutor.CmdExecContext context) {
        String cmdName = context.cmdName();
        String[] args = context.args();
        ClassLoader classLoader = context.classLoader();
        String targetPackage = context.targetPackage();
        
        logger.debug("执行class命令，参数: " + java.util.Arrays.toString(args));
        logger.debug("目标包: " + targetPackage + ", 类加载器: " + classLoader);

        switch (cmdName) {
            case "cinfo" -> {
                return handleInfo(args, classLoader, targetPackage);
            }
            case "cgraph" -> {
                return handleGraph(args, classLoader);
            }
            case "canalyze" -> {
                return handleAnalyze(args, classLoader, targetPackage);
            }
            case "clist" -> {
                return handleList(args, classLoader, targetPackage);
            }
            case "cinvoke" -> {
                return handleInvoke(args, classLoader, targetPackage, context);
            }
            case "cfield" -> {
                return handleField(args, classLoader, targetPackage);
            }
            case "csearch" -> {
                return handleSearch(args, classLoader);
            }
            case "cconstructor" -> {
                return handleConstructor(args, classLoader, targetPackage);
            }
            case "creflect" -> {
                return handleReflect(args, classLoader, context);
            }
            case "class" -> {
                if (args.length < 1) {
                    logger.warn("参数不足，需要至少1个参数");
                    return getHelpText();
                }

                String subCommand = args[0];

                try {
                    return switch (subCommand) {
                        case "info" -> handleInfo(args, classLoader, targetPackage);
                        case "graph" -> handleGraph(args, classLoader);
                        case "analyze" -> handleAnalyze(args, classLoader, targetPackage);
                        case "list" -> handleList(args, classLoader, targetPackage);
                        case "invoke" -> handleInvoke(args, classLoader, targetPackage, context);
                        case "field" -> handleField(args, classLoader, targetPackage);
                        case "search" -> handleSearch(args, classLoader);
                        case "constructor" -> handleConstructor(args, classLoader, targetPackage);
                        case "reflect" -> handleReflect(args, classLoader, context);
                        default -> "未知子命令: " + subCommand + "\n" + getHelpText();
                    };
                } catch (Exception e) {
                    logger.error("执行class命令失败", e);
                    return "错误: " + e.getMessage() +
                            "\n堆栈追踪: \n" + Log.getStackTraceString(e);
                }
            }
        }
        
        return getHelpText();
    }

    private String handleInfo(String[] args, ClassLoader classLoader, String targetPackage) {
        if (args.length < 2) {
            logger.warn("参数不足，需要至少2个参数");
            return getHelpText();
        }

        boolean showInterfaces = false;
        boolean showConstructors = false;
        boolean showSuper = false;
        boolean showModifiers = false;
        boolean showAll = true;
        String className = args[args.length - 1];

        for (int i = 1; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "-i", "--interfaces" -> {
                    showInterfaces = true;
                    showAll = false;
                }
                case "-c", "--constructors" -> {
                    showConstructors = true;
                    showAll = false;
                }
                case "-s", "--super" -> {
                    showSuper = true;
                    showAll = false;
                }
                case "-m", "--modifiers" -> {
                    showModifiers = true;
                    showAll = false;
                }
                case "-a", "--all" -> showAll = true;
            }
        }
        
        logger.debug("目标类: " + className + ", 显示全部: " + showAll);

        try {
            Class<?> targetClass;
            try {
                logger.debug("尝试加载类: " + className);
                if (classLoader == null) {
                    logger.debug("使用默认类加载器");
                    targetClass = XposedHelpers.findClass(className, null);
                } else {
                    logger.debug("使用提供的类加载器: " + classLoader);
                    targetClass = XposedHelpers.findClass(className, classLoader);
                }
                logger.info("成功加载类: " + targetClass.getName());
            } catch (Throwable e) {
                logger.error("加载类失败: " + className, e);
                logger.warn("类加载器为: " + targetPackage);
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader : "无") +
                        "\n错误信息: " + e.getMessage() + "\n堆栈追踪: " + Log.getStackTraceString(e);
            }

            StringBuilder sb = new StringBuilder();
            
            sb.append("=== 基本信息 ===\n");
            sb.append("类名: ").append(targetClass.getName()).append("\n");
            sb.append("简单类名: ").append(targetClass.getSimpleName()).append("\n");
            sb.append("包名: ").append(targetClass.getPackage() != null ? targetClass.getPackage().getName() : "无").append("\n");
            
            if (showAll || showModifiers) {
                sb.append("修饰符: ").append(getModifiers(targetClass.getModifiers())).append("\n");
            }
            
            sb.append("是否为数组: ").append(targetClass.isArray()).append("\n");
            sb.append("是否为接口: ").append(targetClass.isInterface()).append("\n");
            sb.append("是否为注解: ").append(targetClass.isAnnotation()).append("\n");
            sb.append("是否为枚举: ").append(targetClass.isEnum()).append("\n");
            sb.append("是否为原始类型: ").append(targetClass.isPrimitive()).append("\n");
            sb.append("是否为抽象类: ").append(Modifier.isAbstract(targetClass.getModifiers())).append("\n");
            sb.append("是否为final类: ").append(Modifier.isFinal(targetClass.getModifiers())).append("\n\n");

            if (showAll || showSuper) {
                sb.append("=== 继承关系 ===\n");
                Class<?> superClass = targetClass.getSuperclass();
                if (superClass != null) {
                    sb.append("父类: ").append(superClass.getName()).append("\n");
                    
                    Class<?> current = superClass;
                    int level = 1;
                    while (current != null) {
                        Class<?> parent = current.getSuperclass();
                        if (parent != null) {
                            sb.append("  ".repeat(level)).append("└─ ").append(parent.getName()).append("\n");
                            current = parent;
                            level++;
                        } else {
                            break;
                        }
                    }
                } else {
                    sb.append("父类: 无\n");
                }
                sb.append("\n");
            }

            if (showAll || showInterfaces) {
                sb.append("=== 实现的接口 ===\n");
                Class<?>[] interfaces = targetClass.getInterfaces();
                if (interfaces.length == 0) {
                    sb.append("无接口\n");
                } else {
                    for (Class<?> _interface : interfaces) {
                        sb.append("  - ").append(_interface.getName()).append("\n");
                    }
                }
                sb.append("接口总数: ").append(interfaces.length).append("\n\n");
            }

            if (showAll || showConstructors) {
                sb.append("=== 构造函数 ===\n");
                Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
                if (constructors.length == 0) {
                    sb.append("无构造函数\n");
                } else {
                    for (Constructor<?> constructor : constructors) {
                        sb.append("  ").append(getConstructorDescriptor(constructor)).append("\n");
                    }
                }
                sb.append("构造函数总数: ").append(constructors.length).append("\n\n");
            }

            if (showAll) {
                sb.append("=== 字段概览 ===\n");
                Field[] fields = targetClass.getDeclaredFields();
                sb.append("字段总数: ").append(fields.length).append("\n");
                int publicFields = 0, privateFields = 0, protectedFields = 0, staticFields = 0;
                for (Field field : fields) {
                    int mod = field.getModifiers();
                    if (Modifier.isPublic(mod)) publicFields++;
                    if (Modifier.isPrivate(mod)) privateFields++;
                    if (Modifier.isProtected(mod)) protectedFields++;
                    if (Modifier.isStatic(mod)) staticFields++;
                }
                sb.append("  public: ").append(publicFields).append("\n");
                sb.append("  private: ").append(privateFields).append("\n");
                sb.append("  protected: ").append(protectedFields).append("\n");
                sb.append("  static: ").append(staticFields).append("\n\n");

                sb.append("=== 方法概览 ===\n");
                Method[] methods = targetClass.getDeclaredMethods();
                sb.append("方法总数: ").append(methods.length).append("\n");
                int publicMethods = 0, privateMethods = 0, protectedMethods = 0, staticMethods = 0;
                for (Method method : methods) {
                    int mod = method.getModifiers();
                    if (Modifier.isPublic(mod)) publicMethods++;
                    if (Modifier.isPrivate(mod)) privateMethods++;
                    if (Modifier.isProtected(mod)) protectedMethods++;
                    if (Modifier.isStatic(mod)) staticMethods++;
                }
                sb.append("  public: ").append(publicMethods).append("\n");
                sb.append("  private: ").append(privateMethods).append("\n");
                sb.append("  protected: ").append(protectedMethods).append("\n");
                sb.append("  static: ").append(staticMethods).append("\n\n");

                sb.append("=== 包信息 ===\n");
                sb.append("包: ").append(targetPackage != null ? targetPackage : "default").append("\n");
                sb.append("类加载器: ").append(classLoader != null ? classLoader.toString() : "无").append("\n");
            }
            
            logger.info("执行成功");
            logger.debug("执行结果:\n" + sb.toString());
            return sb.toString();

        } catch (Exception e) {
            logger.error("执行class info命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String handleGraph(String[] args, ClassLoader classLoader) {
        if (args.length < 2) {
            return "错误: 参数不足\n用法: class graph <class_name>";
        }

        try {
            String className = args[1];
            Class<?> clazz;
            if (classLoader == null) {
                clazz = XposedHelpers.findClass(className, null);
            } else {
                clazz = XposedHelpers.findClass(className, classLoader);
            }
            
            return generateClassInheritanceGraph(clazz);
        } catch (Throwable e) {
            logger.error("生成类继承图失败", e);
            return "错误: 未找到类: " + args[1] + "\n" + e.getMessage();
        }
    }

    private String generateClassInheritanceGraph(Class<?> clazz) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 类继承图 =====\n");
        sb.append("类名: ").append(clazz.getName()).append("\n\n");
        
        List<Class<?>> hierarchy = getClassHierarchy(clazz);
        
        sb.append("继承层次（从顶层父类到当前类）:\n");
        for (int i = 0; i < hierarchy.size(); i++) {
            Class<?> currentClass = hierarchy.get(i);
            
            for (int j = 0; j < i; j++) {
                sb.append("  ");
            }
            sb.append("└─> ").append(currentClass.getSimpleName()).append("\n");
            
            Class<?>[] interfaces = currentClass.getInterfaces();
            if (interfaces.length > 0) {
                for (int j = 0; j < i + 1; j++) {
                    sb.append("  ");
                }
                sb.append("├─ 实现接口: ");
                for (int k = 0; k < interfaces.length; k++) {
                    if (k > 0) {
                        sb.append(", ");
                    }
                    sb.append(interfaces[k].getSimpleName());
                }
                sb.append("\n");
            }
        }
        
        sb.append("\n子类:\n");
        List<Class<?>> subclasses = findSubclasses(clazz);
        if (subclasses.isEmpty()) {
            sb.append("  (无)\n");
        } else {
            for (Class<?> subclass : subclasses) {
                sb.append("  ").append(subclass.getName()).append("\n");
            }
        }
        
        return sb.toString();
    }

    private List<Class<?>> getClassHierarchy(Class<?> clazz) {
        List<Class<?>> hierarchy = new ArrayList<>();
        Class<?> current = clazz;
        
        while (current != null) {
            hierarchy.add(0, current);
            current = current.getSuperclass();
        }
        
        return hierarchy;
    }

    private List<Class<?>> findSubclasses(Class<?> superClass) {
        List<Class<?>> subclasses = new ArrayList<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(superClass.getClassLoader());

            assert classes != null;
            for (Class<?> clazz : classes) {
                if (superClass.isAssignableFrom(clazz) && !clazz.equals(superClass)) {
                    subclasses.add(clazz);
                }
            }
        } catch (Exception e) {
            logger.warn("查找子类失败", e);
        }
        
        return subclasses;
    }

    private String handleAnalyze(String[] args, ClassLoader classLoader, String targetPackage) {
        if (args.length < 2) {
            logger.warn("参数不足，需要至少2个参数");
            return getHelpText();
        }

        boolean showFields = false;
        boolean showMethods = false;
        boolean showAll = true;
        String className = args[args.length - 1];

        for (int i = 1; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "-f", "--fields" -> {
                    showFields = true;
                    showMethods = false;
                    showAll = false;
                }
                case "-m", "--methods" -> {
                    showMethods = true;
                    showFields = false;
                    showAll = false;
                }
                case "-a", "--all" -> {
                    showAll = true;
                    showFields = false;
                    showMethods = false;
                }
            }
        }
        
        logger.debug("目标类: " + className + ", 显示字段: " + showFields + ", 显示方法: " + showMethods + ", 显示全部: " + showAll);

        try {
            Class<?> targetClass;
            try {
                logger.debug("尝试加载类: " + className);
                if (classLoader == null) {
                    logger.debug("使用默认类加载器");
                    targetClass = XposedHelpers.findClass(className, null);
                } else {
                    logger.debug("使用提供的类加载器: " + classLoader);
                    targetClass = XposedHelpers.findClass(className, classLoader);
                }
                logger.info("成功加载类: " + targetClass.getName());
            } catch (Throwable e) {
                logger.error("加载类失败: " + className, e);
                logger.warn("类加载器为: " + targetPackage);
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader : "无") +
                        "\n错误信息: " + e.getMessage() + "\n堆栈追踪: " + Log.getStackTraceString(e);
            }

            StringBuilder sb = new StringBuilder();
            
            if (showAll || showFields) {
                sb.append("=== 字段 ===\n");
                Field[] fields = targetClass.getDeclaredFields();
                if (fields.length == 0) {
                    sb.append("无字段\n");
                } else {
                    for (Field field : fields) {
                        sb.append("  ").append(getFieldDescriptor(field)).append("\n");
                    }
                }
                sb.append("字段总数: ").append(fields.length).append("\n\n");
            }

            if (showAll || showMethods) {
                sb.append("=== 方法 ===\n");
                Method[] methods = targetClass.getDeclaredMethods();
                if (methods.length == 0) {
                    sb.append("无方法\n");
                } else {
                    for (Method method : methods) {
                        sb.append("  ").append(getMethodDescriptor(method)).append("\n");
                    }
                }
                sb.append("方法总数: ").append(methods.length).append("\n\n");
            }

            if (showAll) {
                sb.append("=== 类信息 ===\n");
                sb.append("类名: ").append(targetClass.getName()).append("\n");
                sb.append("简单类名: ").append(targetClass.getSimpleName()).append("\n");
                sb.append("包名: ").append(targetClass.getPackage() != null ? targetClass.getPackage().getName() : "无").append("\n");
                sb.append("是否为数组: ").append(targetClass.isArray()).append("\n");
                sb.append("是否为接口: ").append(targetClass.isInterface()).append("\n");
                sb.append("是否为注解: ").append(targetClass.isAnnotation()).append("\n");
                sb.append("是否为枚举: ").append(targetClass.isEnum()).append("\n");
                sb.append("是否为原始类型: ").append(targetClass.isPrimitive()).append("\n");
                sb.append("是否为抽象类: ").append(Modifier.isAbstract(targetClass.getModifiers())).append("\n\n");

                sb.append("=== 父类 ===\n");
                Class<?> superClass = targetClass.getSuperclass();
                if (superClass != null) {
                    sb.append(superClass.getName()).append("\n");
                } else {
                    sb.append("无父类\n");
                }
                sb.append("\n");

                sb.append("=== 实现的接口 ===\n");
                Class<?>[] interfaces = targetClass.getInterfaces();
                if (interfaces.length == 0) {
                    sb.append("无接口\n");
                } else {
                    for (Class<?> _interface : interfaces) {
                        sb.append("  - ").append(_interface.getName()).append("\n");
                    }
                }
                sb.append("接口总数: ").append(interfaces.length).append("\n\n");

                sb.append("=== 包信息 ===\n");
                sb.append("包: ").append(targetPackage != null ? targetPackage : "default").append("\n");
                sb.append("类加载器: ").append(classLoader != null ? classLoader.toString() : "无").append("\n");
            }
            
            logger.info("执行成功");
            logger.debug("执行结果:\n" + sb.toString());
            return sb.toString();

        } catch (Exception e) {
            logger.error("执行class analyze命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String handleList(String[] args, ClassLoader classLoader, String targetPackage) {
        if (args.length < 1) {
            logger.warn("参数不足，需要至少1个参数");
            return getHelpText();
        }

        boolean verbose = args[0].equals("-vb") || args[0].equals("--verbose");
        if (verbose && args.length < 2) {
            logger.warn("详细模式需要指定类名");
            return getHelpText();
        }
        String className = args[args.length - 1];
        
        logger.debug("目标类名: " + className + ", 详细模式: " + verbose);

        try {
            Class<?> targetClass;
            try {
                logger.debug("尝试加载类: " + className);
                if (classLoader == null) {
                    logger.debug("使用默认类加载器");
                    targetClass = XposedHelpers.findClass(className, null);
                } else {
                    logger.debug("使用提供的类加载器: " + classLoader);
                    targetClass = XposedHelpers.findClass(className, classLoader);
                }
                logger.info("成功加载类: " + targetClass.getName());
            } catch (Throwable e) {
                logger.error("加载类失败: " + className, e);
                logger.warn("类加载器为: " + targetPackage);
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader : "无") +
                        "\n错误信息: " + e.getMessage() + "\n堆栈追踪: " + Log.getStackTraceString(e);
            }

            StringBuilder sb = new StringBuilder();
            sb.append("类名: ").append(className).append("\n");
            sb.append("使用的包: ").append(targetPackage != null ? targetPackage : "default").append("\n");
            sb.append("类加载器: ").append(classLoader != null ? classLoader : "无").append("\n");
            sb.append("\n方法列表:\n");

            logger.debug("开始获取类方法");
            Method[] methods = targetClass.getDeclaredMethods();
            logger.debug("找到 " + methods.length + " 个方法");
            
            Arrays.sort(methods, Comparator.comparing(Method::getName)
                    .thenComparingInt(Method::getParameterCount));

            int staticCount = 0;
            int instanceCount = 0;

            for (Method method : methods) {
                int modifiers = method.getModifiers();
                if (Modifier.isStatic(modifiers)) {
                    staticCount++;
                } else {
                    instanceCount++;
                }
                if (verbose) {
                    sb.append("  ").append(method.toString()).append("\n");
                } else {
                    sb.append("  ").append(getShortMethodDescriptor(method)).append("\n");
                }
            }

            sb.append("\n结果:\n");
            sb.append("  静态方法: ").append(staticCount).append("\n");
            sb.append("  实例方法: ").append(instanceCount).append("\n");
            sb.append("  总计: ").append(methods.length).append("\n");
            
            logger.info("执行成功，找到 " + methods.length + " 个方法 (静态: " + staticCount + ", 实例: " + instanceCount + ")");
            logger.debug("执行结果:\n" + sb.toString());
            return sb.toString();

        } catch (Exception e) {
            logger.error("执行list命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String getModifiers(int modifiers) {
        StringBuilder sb = new StringBuilder();
        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");
        
        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");
        if (Modifier.isNative(modifiers)) sb.append("native ");
        if (Modifier.isAbstract(modifiers)) sb.append("abstract ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isInterface(modifiers)) sb.append("interface ");
        
        return sb.toString().trim();
    }

    private String getConstructorDescriptor(Constructor<?> constructor) {
        StringBuilder sb = new StringBuilder();
        int modifiers = constructor.getModifiers();

        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");

        sb.append(constructor.getName()).append("(");

        Class<?>[] params = constructor.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) sb.append(", ");
        }

        sb.append(")");
        return sb.toString();
    }

    private String getFieldDescriptor(Field field) {
        StringBuilder sb = new StringBuilder();
        int modifiers = field.getModifiers();

        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");

        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isNative(modifiers)) sb.append("native ");
        if (Modifier.isAbstract(modifiers)) sb.append("abstract ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");
        if (Modifier.isInterface(modifiers)) sb.append("interface ");

        sb.append(field.getType().getSimpleName()).append(" ");
        sb.append(field.getName());
        return sb.toString();
    }

    private String getMethodDescriptor(Method method) {
        StringBuilder sb = new StringBuilder();
        int modifiers = method.getModifiers();

        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");

        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isNative(modifiers)) sb.append("native ");
        if (Modifier.isAbstract(modifiers)) sb.append("abstract ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");
        if (Modifier.isInterface(modifiers)) sb.append("interface ");

        sb.append(method.getReturnType().getSimpleName()).append(" ");
        sb.append(method.getName()).append("(");

        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) sb.append(", ");
        }

        sb.append(")");
        return sb.toString();
    }

    private String getShortMethodDescriptor(Method method) {
        StringBuilder sb = new StringBuilder();
        int modifiers = method.getModifiers();

        if (Modifier.isPublic(modifiers)) sb.append("public ");
        else if (Modifier.isPrivate(modifiers)) sb.append("private ");
        else if (Modifier.isProtected(modifiers)) sb.append("protected ");
        else sb.append("[package-private] ");

        if (Modifier.isStatic(modifiers)) sb.append("static ");
        if (Modifier.isFinal(modifiers)) sb.append("final ");
        if (Modifier.isSynchronized(modifiers)) sb.append("synchronized ");
        if (Modifier.isNative(modifiers)) sb.append("native ");

        if (Modifier.isInterface(modifiers)) sb.append("interface ");
        if (Modifier.isVolatile(modifiers)) sb.append("volatile ");
        if (Modifier.isStrict(modifiers)) sb.append("strictfp ");
        if (Modifier.isTransient(modifiers)) sb.append("transient ");

        sb.append(method.getReturnType().getSimpleName()).append(" ");
        sb.append(method.getName()).append("(");

        Class<?>[] params = method.getParameterTypes();
        for (int i = 0; i < params.length; i++) {
            sb.append(params[i].getSimpleName());
            if (i < params.length - 1) sb.append(", ");
        }

        sb.append(")");
        return sb.toString();
    }

    private String handleInvoke(String[] args, ClassLoader classLoader, String targetPackage, CommandExecutor.CmdExecContext context) {
        if (args.length < 2) {
            logger.warn("提供的参数不足");
            return getHelpText();
        }

        boolean accessSuper = false;
        boolean accessInterfaces = false;
        
        String className;
        String methodName;
        
        int argIndex = 0;
        while (argIndex < args.length) {
            String arg = args[argIndex];
            if (arg.equals("--super")) {
                accessSuper = true;
                argIndex++;
            } else if (arg.equals("--interfaces")) {
                accessInterfaces = true;
                argIndex++;
            } else {
                break;
            }
        }
        
        if (argIndex + 2 > args.length) {
            logger.warn("提供的参数不足");
            return getHelpText();
        }
        
        className = args[argIndex];
        methodName = args[argIndex + 1];
        argIndex += 2;

        try {
            Class<?> targetClass;
            try {
                targetClass = XposedHelpers.findClass(className, classLoader);
            } catch (Throwable e) {
                logger.warn("没有找到类" + className);
                return "找不到类: " + className +
                        "\n类加载器: " + (targetPackage != null ? targetPackage : "default") +
                        "\n错误信息: " + e.getMessage() +
                        "\n堆栈追踪: " + Log.getStackTraceString(e);
            }

            List<Object> params = new ArrayList<>();
            List<Class<?>> paramTypes = new ArrayList<>();

            for (int i = argIndex; i < args.length; i++) {
                String paramStr = args[i];
                int colonIndex = paramStr.indexOf(':');
                if (colonIndex <= 0) {
                    logger.warn("参数形式不正确，获取到的: " + paramStr);
                    return "参数形式不正确: " + paramStr +
                            "; 应为Type:value" +
                            "\n\n示例: " +
                            "\n    Integer:123, String:\"hello\", Boolean:true";
                }

                String typeName = paramStr.substring(0, colonIndex);
                String valueExpr = paramStr.substring(colonIndex + 1);

                try {
                    Object parsedValue = com.justnothing.testmodule.command.functions.classcmd.TypeParser.parse(typeName, valueExpr, classLoader);
                    params.add(parsedValue);
                    paramTypes.add(parsedValue.getClass());
                    logger.info("参数" + (params.size() - 1) +
                            ": (" + paramTypes.get(paramTypes.size()-1).getName() +")" +
                            params.get(paramTypes.size()-1).toString());
                } catch (Exception e) {
                    logger.warn("无法解析参数" + paramStr);
                    return "解析参数 " + (i-1) + "失败: " + e.getMessage() +
                            "\n参数: " + paramStr +
                            "\n堆栈追踪: " + Log.getStackTraceString(e);
                }
            }

            Method method = findMethod(targetClass, methodName,
                    paramTypes.toArray(new Class<?>[0]), true, accessSuper, accessInterfaces);

            if (method == null) {
                method = findMethod(targetClass, methodName,
                        paramTypes.toArray(new Class<?>[0]), false, accessSuper, accessInterfaces);

                if (method == null) {
                    logger.warn("没有找到类" + className + "的方法" + methodName);
                    StringBuilder sb = new StringBuilder();
                    sb.append("没有找到方法: ").append(methodName).append("(");
                    for (int i = 0; i < paramTypes.size(); i++) {
                        sb.append(paramTypes.get(i).getSimpleName());
                        if (i < paramTypes.size() - 1) sb.append(", ");
                    }
                    sb.append(")\n");

                    sb.append("目前找到符合名称 '").append(methodName).append("' 的方法有:\n");
                    boolean found = false;
                    for (Method m : targetClass.getDeclaredMethods()) {
                        if (m.getName().contains(methodName)) {
                            sb.append("  ");
                            logger.warn("但是找到了类似的方法" + getMethodDescriptor(m));
                            sb.append(getMethodDescriptor(m));
                            sb.append("\n");
                            found = true;
                        }
                    }
                    if (!found) sb.append("(暂无)");
                    return sb.toString();
                }
            }

            method.setAccessible(true);
            context.output().println("找到了对应方法, 开始调用...");
            Object result;

            if (Modifier.isStatic(method.getModifiers())) {
                result = method.invoke(null, params.toArray());
            } else {
                result = findSingletonInstance(targetClass);
                if (result == null) {
                    try {
                        result = targetClass.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        logger.warn("尝试调用非静态方法" + className + "." + methodName + "时创建实例失败", e);
                        return "非静态方法需要一个示例，在创建实例的时候出现错误: " + e.getMessage() +
                                "\n堆栈追踪: " + Log.getStackTraceString(e);
                    }
                }
                result = method.invoke(result, params.toArray());
            }

            if (result == null) {
                logger.info("调用成功，返回: null");
                return "结果: null";
            } else {
                logger.info("调用成功，返回：(" + result.getClass().getName() + result.toString());
                return "结果: " + result.toString() +
                        "\n类型: " + result.getClass().getName() +
                        "\nHash: " + System.identityHashCode(result);
            }

        } catch (Throwable e) {
            StringBuilder sb = new StringBuilder();
            logger.info("调用失败", e);
            sb.append("调用失败: ").append(e.getMessage()).append("\n");
            Throwable cause = e.getCause();
            if (cause != null && cause != e) {
                sb.append("原因: ").append(cause.getMessage()).append("\n");
            }
            sb.append("堆栈追踪:\n");
            sb.append(Log.getStackTraceString(e));
            return sb.toString();
        }
    }

    private String handleField(String[] args, ClassLoader classLoader, String targetPackage) {
        if (args.length < 1) {
            logger.warn("参数不足，需要至少1个参数");
            return getHelpText();
        }

        boolean getValue = false;
        boolean setValue = false;
        boolean showValue = false;
        boolean showType = false;
        boolean showModifiers = false;
        boolean showAll = true;
        boolean accessSuper = false;
        boolean accessInterfaces = false;
        
        String valueToSet = null;
        String fieldName = null;
        String className = args[args.length - 1];

        for (int i = 0; i < args.length - 1; i++) {
            String arg = args[i];
            switch (arg) {
                case "-g", "--get" -> {
                    getValue = true;
                    showAll = false;
                }
                case "-s", "--set" -> {
                    setValue = true;
                    showAll = false;
                    if (i + 1 < args.length - 1) {
                        valueToSet = args[i + 1];
                        i++;
                    }
                }
                case "-v", "--value" -> {
                    showValue = true;
                    showAll = false;
                }
                case "-t", "--type" -> {
                    showType = true;
                    showAll = false;
                }
                case "-m", "--modifiers" -> {
                    showModifiers = true;
                    showAll = false;
                }
                case "-a", "--all" -> showAll = true;
                case "--super" -> accessSuper = true;
                case "--interfaces" -> accessInterfaces = true;
            }
        }
        
        if ((getValue || setValue || showValue) && args.length < 2) {
            return "错误: 获取/设置字段值需要指定字段名\n" + getHelpText();
        }
        
        if (args.length >= 2) {
            fieldName = args[args.length - 2];
            if (fieldName.startsWith("-")) {
                fieldName = null;
            }
        }
        
        logger.debug("目标类: " + className + ", 字段名: " + fieldName + ", 显示全部: " + showAll);

        try {
            Class<?> targetClass;
            try {
                logger.debug("尝试加载类: " + className);
                if (classLoader == null) {
                    logger.debug("使用默认类加载器");
                    targetClass = XposedHelpers.findClass(className, null);
                } else {
                    logger.debug("使用提供的类加载器: " + classLoader);
                    targetClass = XposedHelpers.findClass(className, classLoader);
                }
                logger.info("成功加载类: " + targetClass.getName());
            } catch (Throwable e) {
                logger.error("加载类失败: " + className, e);
                logger.warn("类加载器为: " + targetPackage);
                return "找不到类: " + className +
                        "\n使用的包: " + (targetPackage != null ? targetPackage : "default") +
                        "\n类加载器: " + (classLoader != null ? classLoader : "无") +
                        "\n错误信息: " + e.getMessage() + "\n堆栈追踪: " + Log.getStackTraceString(e);
            }

            if (fieldName != null) {
                Field field = findField(targetClass, fieldName, accessSuper, accessInterfaces);

                if (field == null) {
                    return "找不到字段: " + fieldName + "\n" + getHelpText();
                }

                field.setAccessible(true);

                if (getValue || setValue) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        if (getValue) {
                            Object value = field.get(null);
                            return "字段值: " + (value != null ? value.toString() : "null");
                        } else if (setValue) {
                            Object value = parseValue(valueToSet, field.getType());
                            field.set(null, value);
                            return "成功设置字段值: " + valueToSet;
                        }
                    } else {
                        return "错误: 无法获取/设置非静态字段，需要提供实例对象";
                    }
                }

                StringBuilder sb = new StringBuilder();
                sb.append("=== 字段信息 ===\n");
                sb.append("字段名: ").append(field.getName()).append("\n");
                sb.append("类型: ").append(field.getType().getName()).append("\n");

                if (showAll || showType) {
                    sb.append("类型: ").append(field.getType().getName()).append("\n");
                }

                if (showAll || showModifiers) {
                    sb.append("修饰符: ").append(getModifiers(field.getModifiers())).append("\n");
                }

                if (showAll || showValue) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        try {
                            Object value = field.get(null);
                            sb.append("值: ").append(value != null ? value.toString() : "null").append("\n");
                        } catch (Exception e) {
                            sb.append("值: 无法获取 (").append(e.getMessage()).append(")\n");
                        }
                    } else {
                        sb.append("值: 非静态字段，需要实例对象\n");
                    }
                }

                if (showAll) {
                    sb.append("声明类: ").append(field.getDeclaringClass().getName()).append("\n");
                    sb.append("是否为final: ").append(Modifier.isFinal(field.getModifiers())).append("\n");
                    sb.append("是否为volatile: ").append(Modifier.isVolatile(field.getModifiers())).append("\n");
                    sb.append("是否为transient: ").append(Modifier.isTransient(field.getModifiers())).append("\n");
                }

                return sb.toString();

            } else {
                Field[] fields = targetClass.getDeclaredFields();
                StringBuilder sb = new StringBuilder();
                
                sb.append("=== 字段列表 ===\n");
                sb.append("类: ").append(targetClass.getName()).append("\n");
                sb.append("字段总数: ").append(fields.length).append("\n\n");
                
                if (fields.length == 0) {
                    sb.append("无字段\n");
                } else {
                    for (Field field : fields) {
                        sb.append("  ").append(getFieldDescriptor(field)).append("\n");
                    }
                }
                
                return sb.toString();
            }

        } catch (Exception e) {
            logger.error("执行field命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private Field findField(Class<?> targetClass, String fieldName, boolean accessSuper, boolean accessInterfaces) {
        Class<?> currentClass = targetClass;
        
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        
        if (accessInterfaces) {
            assert targetClass != null;
            Class<?>[] interfaces = targetClass.getInterfaces();
            for (Class<?> _interface : interfaces) {
                try {
                    return _interface.getDeclaredField(fieldName);
                } catch (NoSuchFieldException ignored) {
                }
            }
        }
        
        return null;
    }

    private Method findMethod(Class<?> clazz, String methodName, Class<?>[] paramTypes, boolean staticOnly, boolean accessSuper, boolean accessInterfaces) {
        Class<?> currentClass = clazz;
        
        while (currentClass != null) {
            try {
                Method method = currentClass.getDeclaredMethod(methodName, paramTypes);
                if (!staticOnly || Modifier.isStatic(method.getModifiers())) {
                    return method;
                }
            } catch (NoSuchMethodException e) {
                for (Method method : currentClass.getDeclaredMethods()) {
                    if (method.getName().equals(methodName)) {
                        if (staticOnly && !Modifier.isStatic(method.getModifiers())) {
                            continue;
                        }

                        if (method.getParameterCount() == paramTypes.length) {
                            Class<?>[] methodParams = method.getParameterTypes();
                            boolean compatible = true;

                            for (int i = 0; i < paramTypes.length; i++) {
                                if (!methodParams[i].isAssignableFrom(paramTypes[i])) {
                                    compatible = false;
                                    break;
                                }
                            }

                            if (compatible) {
                                return method;
                            }
                        }
                    }
                }
            }
            
            if (accessSuper) {
                currentClass = currentClass.getSuperclass();
            } else {
                break;
            }
        }
        
        if (accessInterfaces) {
            assert clazz != null;
            Class<?>[] interfaces = clazz.getInterfaces();
            for (Class<?> _interface : interfaces) {
                try {
                    Method method = _interface.getDeclaredMethod(methodName, paramTypes);
                    if (!staticOnly || Modifier.isStatic(method.getModifiers())) {
                        return method;
                    }
                } catch (NoSuchMethodException e) {
                    for (Method method : _interface.getDeclaredMethods()) {
                        if (method.getName().equals(methodName)) {
                            if (staticOnly && !Modifier.isStatic(method.getModifiers())) {
                                continue;
                            }

                            if (method.getParameterCount() == paramTypes.length) {
                                Class<?>[] methodParams = method.getParameterTypes();
                                boolean compatible = true;

                                for (int i = 0; i < paramTypes.length; i++) {
                                    if (!methodParams[i].isAssignableFrom(paramTypes[i])) {
                                        compatible = false;
                                        break;
                                    }
                                }

                                if (compatible) {
                                    return method;
                                }
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }

    private Object findSingletonInstance(Class<?> clazz) {
        String[] singletonFieldNames = {
                "INSTANCE", "instance", "mInstance", "sInstance",
                "sSingleton", "mSingleton", "gInstance"
        };

        for (String fieldName : singletonFieldNames) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                if (Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    Object instance = field.get(null);
                    if (clazz.isInstance(instance)) {
                        return instance;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private Object parseValue(String value, Class<?> type) {
        if (value == null) return null;
        
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if (type == short.class || type == Short.class) {
            return Short.parseShort(value);
        } else if (type == byte.class || type == Byte.class) {
            return Byte.parseByte(value);
        } else if (type == char.class || type == Character.class) {
            return value.charAt(0);
        } else {
            return value;
        }
    }

    private String handleSearch(String[] args, ClassLoader classLoader) {
        if (args.length < 2) {
            logger.warn("参数不足");
            return getHelpText();
        }

        String subCommand = args[0];
        String pattern = args[1];

        try {
            return switch (subCommand) {
                case "class" -> searchClasses(pattern, classLoader);
                case "method" -> searchMethods(pattern, classLoader);
                case "field" -> searchFields(pattern, classLoader);
                case "annotation" -> searchAnnotations(pattern, classLoader);
                default -> "未知子命令: " + subCommand + "\n" + getHelpText();
            };
        } catch (Exception e) {
            logger.error("执行search命令失败", e);
            return "错误: " + e.getMessage() +
                    "\n堆栈追踪: \n" + Log.getStackTraceString(e);
        }
    }

    private String searchClasses(String pattern, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索类名 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        List<Class<?>> matchedClasses = new ArrayList<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);

            assert classes != null;
            for (Class<?> clazz : classes) {
                String className = clazz.getName();
                if (matchesPattern(className, pattern)) {
                    matchedClasses.add(clazz);
                }
            }
            
        } catch (Exception e) {
            logger.warn("搜索类失败", e);
        }
        
        if (matchedClasses.isEmpty()) {
            sb.append("未找到匹配的类\n");
        } else {
            sb.append("找到 ").append(matchedClasses.size()).append(" 个匹配的类:\n\n");
            for (Class<?> clazz : matchedClasses) {
                sb.append("  ").append(clazz.getName()).append("\n");
            }
        }
        
        return sb.toString();
    }

    private String searchMethods(String pattern, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索方法名 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        Map<String, List<String>> matchedMethods = new LinkedHashMap<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);

            assert classes != null;
            for (Class<?> clazz : classes) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (matchesPattern(method.getName(), pattern)) {
                        String className = clazz.getName();
                        matchedMethods.computeIfAbsent(className, k -> new ArrayList<>())
                                        .add(method.toString());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("搜索方法失败", e);
        }
        
        if (matchedMethods.isEmpty()) {
            sb.append("未找到匹配的方法\n");
        } else {
            int totalCount = matchedMethods.values().stream().mapToInt(List::size).sum();
            sb.append("找到 ").append(totalCount).append(" 个匹配的方法:\n\n");
            
            for (Map.Entry<String, List<String>> entry : matchedMethods.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String method : entry.getValue()) {
                    sb.append("    ").append(method).append("\n");
                }
            }
        }
        
        return sb.toString();
    }

    private String searchFields(String pattern, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索字段名 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        Map<String, List<String>> matchedFields = new LinkedHashMap<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);

            assert classes != null;
            for (Class<?> clazz : classes) {
                for (Field field : clazz.getDeclaredFields()) {
                    if (matchesPattern(field.getName(), pattern)) {
                        String className = clazz.getName();
                        matchedFields.computeIfAbsent(className, k -> new ArrayList<>())
                                      .add(field.getType().getName() + " " + field.getName());
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("搜索字段失败", e);
        }
        
        if (matchedFields.isEmpty()) {
            sb.append("未找到匹配的字段\n");
        } else {
            int totalCount = matchedFields.values().stream().mapToInt(List::size).sum();
            sb.append("找到 ").append(totalCount).append(" 个匹配的字段:\n\n");
            
            for (Map.Entry<String, List<String>> entry : matchedFields.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String field : entry.getValue()) {
                    sb.append("    ").append(field).append("\n");
                }
            }
        }
        
        return sb.toString();
    }

    private String searchAnnotations(String pattern, ClassLoader classLoader) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== 搜索注解 =====\n");
        sb.append("模式: ").append(pattern).append("\n\n");
        
        Map<String, List<String>> matchedAnnotations = new LinkedHashMap<>();
        
        try {
            Field classesField = ClassLoader.class.getDeclaredField("classes");
            classesField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Vector<Class<?>> classes = (Vector<Class<?>>) classesField.get(classLoader);

            assert classes != null;
            for (Class<?> clazz : classes) {
                for (Annotation annotation : clazz.getAnnotations()) {
                    String annotationName = annotation.annotationType().getSimpleName();
                    if (matchesPattern(annotationName, pattern)) {
                        String className = clazz.getName();
                        matchedAnnotations.computeIfAbsent(className, k -> new ArrayList<>())
                                      .add(annotationName);
                    }
                }
                
                for (Method method : clazz.getDeclaredMethods()) {
                    for (Annotation annotation : method.getAnnotations()) {
                        String annotationName = annotation.annotationType().getSimpleName();
                        if (matchesPattern(annotationName, pattern)) {
                            String className = clazz.getName();
                            matchedAnnotations.computeIfAbsent(className, k -> new ArrayList<>())
                                          .add(method.getName() + " -> " + annotationName);
                        }
                    }
                }
            }
            
        } catch (Exception e) {
            logger.warn("搜索注解失败", e);
        }
        
        if (matchedAnnotations.isEmpty()) {
            sb.append("未找到匹配的注解\n");
        } else {
            int totalCount = matchedAnnotations.values().stream().mapToInt(List::size).sum();
            sb.append("找到 ").append(totalCount).append(" 个匹配的注解:\n\n");
            
            for (Map.Entry<String, List<String>> entry : matchedAnnotations.entrySet()) {
                sb.append("  ").append(entry.getKey()).append(":\n");
                for (String annotation : entry.getValue()) {
                    sb.append("    @").append(annotation).append("\n");
                }
            }
        }
        
        return sb.toString();
    }

    private boolean matchesPattern(String text, String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return true;
        }
        
        if (pattern.equals("*")) {
            return true;
        }
        
        String regex = pattern.replace(".", "\\.")
                             .replace("*", ".*");
        
        return text.matches(regex);
    }

    private String handleConstructor(String[] args, ClassLoader classLoader, String targetPackage) {
        if (args.length < 2) {
            logger.warn("参数不足，需要至少2个参数");
            return getHelpText();
        }

        String className = args[1];
        List<Object> params = new ArrayList<>();
        List<Class<?>> paramTypes = new ArrayList<>();

        for (int i = 2; i < args.length; i++) {
            String paramStr = args[i];
            int colonIndex = paramStr.indexOf(':');
            if (colonIndex <= 0) {
                logger.warn("参数形式不正确，获取到的: " + paramStr);
                return "参数形式不正确: " + paramStr +
                        "; 应为Type:value" +
                        "\n\n示例: " +
                        "\n    Integer:123, String:\"hello\", Boolean:true";
            }

            String typeName = paramStr.substring(0, colonIndex);
            String valueExpr = paramStr.substring(colonIndex + 1);

            try {
                Object parsedValue = com.justnothing.testmodule.command.functions.classcmd.TypeParser.parse(typeName, valueExpr, classLoader);
                params.add(parsedValue);
                assert parsedValue != null;
                paramTypes.add(parsedValue.getClass());
                logger.info("参数" + (params.size() - 1) +
                        ": (" + paramTypes.get(paramTypes.size()-1).getName() +")" +
                        params.get(paramTypes.size()-1).toString());
            } catch (Exception e) {
                logger.warn("无法解析参数" + paramStr);
                return "解析参数 " + (i-1) + "失败: " + e.getMessage() +
                        "\n参数: " + paramStr +
                        "\n堆栈追踪: " + Log.getStackTraceString(e);
            }
        }

        try {
            Class<?> targetClass;
            try {
                targetClass = XposedHelpers.findClass(className, classLoader);
            } catch (Throwable e) {
                logger.warn("没有找到类" + className);
                return "找不到类: " + className +
                        "\n类加载器: " + (targetPackage != null ? targetPackage : "default") +
                        "\n错误信息: " + e.getMessage() +
                        "\n堆栈追踪: " + Log.getStackTraceString(e);
            }

            Constructor<?> constructor = findConstructor(targetClass, paramTypes.toArray(new Class<?>[0]));

            if (constructor == null) {
                logger.warn("没有找到类" + className + "的匹配构造函数");
                StringBuilder sb = new StringBuilder();
                sb.append("没有找到匹配的构造函数\n");
                sb.append("参数类型: ");
                for (int i = 0; i < paramTypes.size(); i++) {
                    sb.append(paramTypes.get(i).getSimpleName());
                    if (i < paramTypes.size() - 1) sb.append(", ");
                }
                sb.append("\n\n");
                sb.append("可用的构造函数:\n");
                for (Constructor<?> c : targetClass.getDeclaredConstructors()) {
                    sb.append("  ").append(getConstructorDescriptor(c)).append("\n");
                }
                return sb.toString();
            }

            constructor.setAccessible(true);
            Object instance = constructor.newInstance(params.toArray());

            logger.info("创建实例成功: " + instance);
            return "创建实例成功: " + instance.toString() +
                    "\n类型: " + instance.getClass().getName() +
                    "\nHash: " + System.identityHashCode(instance);

        } catch (Throwable e) {
            StringBuilder sb = new StringBuilder();
            logger.info("创建实例失败", e);
            sb.append("创建实例失败: ").append(e.getMessage()).append("\n");
            Throwable cause = e.getCause();
            if (cause != null && cause != e) {
                sb.append("原因: ").append(cause.getMessage()).append("\n");
            }
            sb.append("堆栈追踪:\n");
            sb.append(Log.getStackTraceString(e));
            return sb.toString();
        }
    }

    private Constructor<?> findConstructor(Class<?> clazz, Class<?>[] paramTypes) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (constructor.getParameterCount() == paramTypes.length) {
                Class<?>[] constructorParams = constructor.getParameterTypes();
                boolean compatible = true;

                for (int i = 0; i < paramTypes.length; i++) {
                    if (!constructorParams[i].isAssignableFrom(paramTypes[i])) {
                        compatible = false;
                        break;
                    }
                }

                if (compatible) {
                    return constructor;
                }
            }
        }
        return null;
    }

    // ========== 从 reflect 模块整合的功能 ==========

    /**
     * 处理统一的反射命令
     */
    private String handleReflect(String[] args, ClassLoader classLoader, CommandExecutor.CmdExecContext context) {
        if (args.length < 4) {
            return "参数不足，需要至少4个参数: class reflect <class> <type> <name> [options]\n" + 
                   getHelpText();
        }

        // args[0] 是 "reflect"，所以实际参数从 args[1] 开始
        String className = args[1];
        String type = args[2];
        String memberName = args[3];
        
        String valueToSet = null;
        String[] methodParams = null;
        boolean accessSuper = false;
        boolean accessInterfaces = false;
        boolean rawOutput = false;

        // 解析选项（从第4个参数开始，因为前3个是 reflect <class> <type> <name>）
        for (int i = 4; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "-v", "--value" -> {
                    if (i + 1 < args.length) {
                        valueToSet = args[++i];
                    }
                }
                case "-p", "--params" -> {
                    if (i + 1 < args.length) {
                        String paramsStr = args[++i];
                        methodParams = parseParams(paramsStr);
                    }
                }
                case "-s", "--super" -> accessSuper = true;
                case "-i", "--interfaces" -> accessInterfaces = true;
                case "-r", "--raw" -> rawOutput = true;
            }
        }
        
        try {
            Class<?> targetClass;
            if (classLoader != null) {
                targetClass = XposedHelpers.findClass(className, classLoader);
            } else {
                targetClass = XposedHelpers.findClass(className, null);
            }
            if (targetClass == null) {
                logger.error("找不到类: " + className);
                return "找不到类: " + className;
            }
        
            return switch (type) {
                case "field" -> handleReflectField(targetClass, memberName, valueToSet, accessSuper, accessInterfaces, rawOutput);
                case "method" -> handleReflectMethod(targetClass, memberName, methodParams, accessSuper, accessInterfaces, rawOutput);
                case "constructor" -> handleReflectConstructor(targetClass, methodParams, rawOutput);
                case "static" -> handleReflectStatic(targetClass, memberName, valueToSet, rawOutput);
                default -> "未知类型: " + type + "\n" + getHelpText();
            };
            
        } catch (Exception e) {
            logger.error("执行reflect命令失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleReflectField(Class<?> targetClass, String fieldName, String valueToSet, 
                                     boolean accessSuper, boolean accessInterfaces, boolean rawOutput) {
        try {
            Field field = findReflectField(targetClass, fieldName, accessSuper, accessInterfaces);
            
            if (field == null) {
                return "找不到字段: " + fieldName;
            }
            
            field.setAccessible(true);
            
            if (valueToSet != null) {
                Object value = parseValue(valueToSet, field.getType());
                if (targetClass.isMemberClass() && Modifier.isStatic(field.getModifiers())) {
                    field.set(null, value);
                } else {
                    field.set(null, value);
                }
                logger.info("设置字段 " + fieldName + " = " + value);
                return "字段 " + fieldName + " 已设置为: " + formatValue(value, rawOutput);
            } else {
                Object value = field.get(null);
                logger.info("获取字段 " + fieldName + " = " + value);
                return "字段 " + fieldName + " = " + formatValue(value, rawOutput);
            }
            
        } catch (Exception e) {
            logger.error("处理字段失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleReflectMethod(Class<?> targetClass, String methodName, String[] params,
                                      boolean accessSuper, boolean accessInterfaces, boolean rawOutput) {
        try {
            Method method = findReflectMethod(targetClass, methodName, params, accessSuper, accessInterfaces);
            
            if (method == null) {
                return "找不到方法: " + methodName;
            }
            
            method.setAccessible(true);
            
            Object result;
            if (Modifier.isStatic(method.getModifiers())) {
                result = method.invoke(null, convertParams(params, method.getParameterTypes()));
            } else {
                return "方法 " + methodName + " 不是静态方法，需要实例对象";
            }
            
            logger.info("调用方法 " + methodName + " = " + result);
            return "方法 " + methodName + " 返回: " + formatValue(result, rawOutput);
            
        } catch (Exception e) {
            logger.error("调用方法失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleReflectConstructor(Class<?> targetClass, String[] params, boolean rawOutput) {
        try {
            Constructor<?> constructor = findReflectConstructor(targetClass, params);
            
            if (constructor == null) {
                return "找不到匹配的构造函数";
            }
            
            constructor.setAccessible(true);
            Object instance = constructor.newInstance(convertParams(params, constructor.getParameterTypes()));
            
            logger.info("创建实例: " + instance);
            return "创建实例: " + formatValue(instance, rawOutput);
            
        } catch (Exception e) {
            logger.error("创建实例失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    private String handleReflectStatic(Class<?> targetClass, String memberName, String valueToSet, boolean rawOutput) {
        try {
            Field field = findReflectField(targetClass, memberName, false, false);
            
            if (field == null) {
                return "找不到静态字段: " + memberName;
            }
            
            if (!Modifier.isStatic(field.getModifiers())) {
                return memberName + " 不是静态字段";
            }
            
            field.setAccessible(true);
            
            if (valueToSet != null) {
                Object value = parseValue(valueToSet, field.getType());
                field.set(null, value);
                logger.info("设置静态字段 " + memberName + " = " + value);
                return "静态字段 " + memberName + " 已设置为: " + formatValue(value, rawOutput);
            } else {
                Object value = field.get(null);
                logger.info("获取静态字段 " + memberName + " = " + value);
                return "静态字段 " + memberName + " = " + formatValue(value, rawOutput);
            }
            
        } catch (Exception e) {
            logger.error("处理静态字段失败", e);
            return "错误: " + e.getMessage() + "\n堆栈: " + getStackTrace(e);
        }
    }

    // ========== 从 reflect 模块整合的工具函数 ==========

    private Field findReflectField(Class<?> targetClass, String fieldName, boolean accessSuper, boolean accessInterfaces) {
        Class<?> currentClass = targetClass;
        
        while (currentClass != null) {
            try {
                return currentClass.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            }
        }
        
        if (accessInterfaces) {
            assert targetClass != null;
            Class<?>[] interfaces = targetClass.getInterfaces();
            for (Class<?> _interface : interfaces) {
                try {
                    return _interface.getDeclaredField(fieldName);
                } catch (NoSuchFieldException e) {
                    continue;
                }
            }
        }
        
        return null;
    }

    private Method findReflectMethod(Class<?> targetClass, String methodName, String[] params, 
                                   boolean accessSuper, boolean accessInterfaces) {
        Class<?> currentClass = targetClass;
        
        while (currentClass != null) {
            Method[] methods = currentClass.getDeclaredMethods();
            for (Method method : methods) {
                if (method.getName().equals(methodName)) {
                    if (params == null || params.length == 0) {
                        if (method.getParameterCount() == 0) {
                            return method;
                        }
                    } else {
                        Class<?>[] paramTypes = method.getParameterTypes();
                        if (paramTypes.length == params.length) {
                            return method;
                        }
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }
        
        if (accessInterfaces) {
            Class<?>[] interfaces = targetClass.getInterfaces();
            for (Class<?> iface : interfaces) {
                Method[] methods = iface.getDeclaredMethods();
                for (Method method : methods) {
                    if (method.getName().equals(methodName)) {
                        if (params == null || params.length == 0) {
                            if (method.getParameterCount() == 0) {
                                return method;
                            }
                        } else {
                            Class<?>[] paramTypes = method.getParameterTypes();
                            if (paramTypes.length == params.length) {
                                return method;
                            }
                        }
                    }
                }
            }
        }
        
        return null;
    }

    private Constructor<?> findReflectConstructor(Class<?> targetClass, String[] params) {
        Constructor<?>[] constructors = targetClass.getDeclaredConstructors();
        
        for (Constructor<?> constructor : constructors) {
            if (params == null || params.length == 0) {
                if (constructor.getParameterCount() == 0) {
                    return constructor;
                }
            } else {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if (paramTypes.length == params.length) {
                    return constructor;
                }
            }
        }
        
        return null;
    }

    private String[] parseParams(String paramsStr) {
        List<String> params = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        
        for (int i = 0; i < paramsStr.length(); i++) {
            char c = paramsStr.charAt(i);
            
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    params.add(current.toString());
                    current = new StringBuilder();
                }
            } else {
                current.append(c);
            }
        }
        
        if (current.length() > 0) {
            params.add(current.toString());
        }
        
        return params.toArray(new String[0]);
    }





    private Object[] convertParams(String[] params, Class<?>[] paramTypes) {
        if (params == null || params.length == 0) {
            return new Object[0];
        }
        
        Object[] result = new Object[params.length];
        for (int i = 0; i < params.length; i++) {
            if (i < paramTypes.length) {
                result[i] = parseValue(params[i], paramTypes[i]);
            } else {
                result[i] = parseValue(params[i], String.class);
            }
        }
        
        return result;
    }

    private String formatValue(Object value, boolean rawOutput) {
        if (value == null) {
            return "null";
        }
        
        if (rawOutput) {
            return value.toString();
        }
        
        if (value.getClass().isArray()) {
            return java.util.Arrays.toString((Object[]) value);
        }
        
        return value.toString();
    }

    private String getStackTrace(Throwable e) {
        StringBuilder sb = new StringBuilder();
        for (StackTraceElement element : e.getStackTrace()) {
            sb.append("    at ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
