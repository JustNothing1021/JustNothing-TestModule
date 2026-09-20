package com.justnothing.testmodule.command.functions.classcmd;

import java.util.Map;

/**
 * class 命令族的 CLI 文案（id 常量 + 中英对照）。
 *
 * <p>命名与 id 规范见 {@link com.justnothing.testmodule.command.framework.i18n.CliTexts}。
 * 本类由 {@code CliTexts} 的静态块登记，新增条目只需在这里加常量 + 两行 put。</p>
 *
 * <p>英文允许缺失（只 put 中文），缺失时英文环境回落显示中文 —— 所以翻译可以一条一条补。</p>
 */
public final class ClassTexts {

    // ==================== @Cmd（主命令）====================
    public static final String CMD_CLASS_DESC = "cmd.class.desc";

    // ==================== @CmdRoutes.Route（命令列表里那一行）====================
    public static final String ROUTE_CLASS_INFO_DESC = "route.class.info.desc";
    public static final String ROUTE_CLASS_GRAPH_DESC = "route.class.graph.desc";
    public static final String ROUTE_CLASS_ANALYZE_DESC = "route.class.analyze.desc";
    public static final String ROUTE_CLASS_LIST_DESC = "route.class.list.desc";
    public static final String ROUTE_CLASS_INVOKE_DESC = "route.class.invoke.desc";
    public static final String ROUTE_CLASS_FIELD_DESC = "route.class.field.desc";
    public static final String ROUTE_CLASS_CONSTRUCTOR_DESC = "route.class.constructor.desc";
    public static final String ROUTE_CLASS_REFLECT_DESC = "route.class.reflect.desc";
    public static final String ROUTE_CLASS_HIERARCHY_DESC = "route.class.hierarchy.desc";

    // ==================== @SubCommandInfo（帮助正文）====================
    public static final String SUB_CLASS_INFO_DESC = "sub.class.info.desc";
    public static final String SUB_CLASS_INFO_OPTIONS = "sub.class.info.options";
    public static final String SUB_CLASS_GRAPH_DESC = "sub.class.graph.desc";
    public static final String SUB_CLASS_GRAPH_OPTIONS = "sub.class.graph.options";
    public static final String SUB_CLASS_ANALYZE_DESC = "sub.class.analyze.desc";
    public static final String SUB_CLASS_ANALYZE_OPTIONS = "sub.class.analyze.options";
    public static final String SUB_CLASS_LIST_DESC = "sub.class.list.desc";
    public static final String SUB_CLASS_LIST_OPTIONS = "sub.class.list.options";
    public static final String SUB_CLASS_INVOKE_DESC = "sub.class.invoke.desc";
    public static final String SUB_CLASS_INVOKE_OPTIONS = "sub.class.invoke.options";
    public static final String SUB_CLASS_FIELD_DESC = "sub.class.field.desc";
    public static final String SUB_CLASS_FIELD_OPTIONS = "sub.class.field.options";
    public static final String SUB_CLASS_CONSTRUCTOR_DESC = "sub.class.constructor.desc";
    public static final String SUB_CLASS_CONSTRUCTOR_USAGE = "sub.class.constructor.usage";
    public static final String SUB_CLASS_CONSTRUCTOR_OPTIONS = "sub.class.constructor.options";
    public static final String SUB_CLASS_REFLECT_DESC = "sub.class.reflect.desc";
    public static final String SUB_CLASS_REFLECT_OPTIONS = "sub.class.reflect.options";

    // ==================== @CmdParam（参数说明）====================
    // --- class info ---
    public static final String PARAM_CLASS_INFO_CLASS_DESC = "param.class.info.class.desc";
    public static final String PARAM_CLASS_INFO_VERBOSE_DESC = "param.class.info.verbose.desc";
    public static final String PARAM_CLASS_INFO_INTERFACES_DESC = "param.class.info.interfaces.desc";
    public static final String PARAM_CLASS_INFO_CONSTRUCTORS_DESC = "param.class.info.constructors.desc";
    public static final String PARAM_CLASS_INFO_SUPER_DESC = "param.class.info.super.desc";
    public static final String PARAM_CLASS_INFO_MODIFIERS_DESC = "param.class.info.modifiers.desc";
    public static final String PARAM_CLASS_INFO_ALL_DESC = "param.class.info.all.desc";

    // --- class graph ---
    public static final String PARAM_CLASS_GRAPH_CLASS_DESC = "param.class.graph.class.desc";
    public static final String PARAM_CLASS_GRAPH_NO_SUBCLASSES_DESC = "param.class.graph.no-subclasses.desc";
    public static final String PARAM_CLASS_GRAPH_NO_INTERFACES_DESC = "param.class.graph.no-interfaces.desc";
    public static final String PARAM_CLASS_GRAPH_COMPACT_DESC = "param.class.graph.compact.desc";
    public static final String PARAM_CLASS_GRAPH_DEPTH_DESC = "param.class.graph.depth.desc";

    // --- class analyze ---
    public static final String PARAM_CLASS_ANALYZE_CLASS_DESC = "param.class.analyze.class.desc";
    public static final String PARAM_CLASS_ANALYZE_FIELDS_DESC = "param.class.analyze.fields.desc";
    public static final String PARAM_CLASS_ANALYZE_METHODS_DESC = "param.class.analyze.methods.desc";
    public static final String PARAM_CLASS_ANALYZE_CONSTRUCTORS_DESC = "param.class.analyze.constructors.desc";
    public static final String PARAM_CLASS_ANALYZE_INTERFACES_DESC = "param.class.analyze.interfaces.desc";
    public static final String PARAM_CLASS_ANALYZE_SUPER_DESC = "param.class.analyze.super.desc";
    public static final String PARAM_CLASS_ANALYZE_MODIFIERS_DESC = "param.class.analyze.modifiers.desc";
    public static final String PARAM_CLASS_ANALYZE_ALL_DESC = "param.class.analyze.all.desc";
    public static final String PARAM_CLASS_ANALYZE_VERBOSE_DESC = "param.class.analyze.verbose.desc";
    public static final String PARAM_CLASS_ANALYZE_HIERARCHY_DESC = "param.class.analyze.hierarchy.desc";
    public static final String PARAM_CLASS_ANALYZE_STATS_DESC = "param.class.analyze.stats.desc";
    public static final String PARAM_CLASS_ANALYZE_RAW_DESC = "param.class.analyze.raw.desc";

    // --- class list ---
    public static final String PARAM_CLASS_LIST_CLASS_DESC = "param.class.list.class.desc";
    public static final String PARAM_CLASS_LIST_VERBOSE_DESC = "param.class.list.verbose.desc";

    // --- class invoke ---
    public static final String PARAM_CLASS_INVOKE_CLASS_DESC = "param.class.invoke.class.desc";
    public static final String PARAM_CLASS_INVOKE_METHOD_DESC = "param.class.invoke.method.desc";
    public static final String PARAM_CLASS_INVOKE_STATIC_DESC = "param.class.invoke.static.desc";
    public static final String PARAM_CLASS_INVOKE_FREE_DESC = "param.class.invoke.free.desc";
    public static final String PARAM_CLASS_INVOKE_SUPER_DESC = "param.class.invoke.super.desc";
    public static final String PARAM_CLASS_INVOKE_INTERFACES_DESC = "param.class.invoke.interfaces.desc";
    public static final String PARAM_CLASS_INVOKE_INSTANCE_DESC = "param.class.invoke.instance.desc";

    // --- class field ---
    public static final String PARAM_CLASS_FIELD_CLASS_DESC = "param.class.field.class.desc";
    public static final String PARAM_CLASS_FIELD_INSTANCE_DESC = "param.class.field.instance.desc";
    public static final String PARAM_CLASS_FIELD_VALUE_DESC = "param.class.field.value.desc";
    public static final String PARAM_CLASS_FIELD_TYPE_DESC = "param.class.field.type.desc";
    public static final String PARAM_CLASS_FIELD_MODIFIERS_DESC = "param.class.field.modifiers.desc";
    public static final String PARAM_CLASS_FIELD_ALL_DESC = "param.class.field.all.desc";
    public static final String PARAM_CLASS_FIELD_SUPER_DESC = "param.class.field.super.desc";
    public static final String PARAM_CLASS_FIELD_INTERFACES_DESC = "param.class.field.interfaces.desc";
    public static final String PARAM_CLASS_FIELD_STATIC_DESC = "param.class.field.static.desc";
    public static final String PARAM_CLASS_FIELD_GET_DESC = "param.class.field.get.desc";
    public static final String PARAM_CLASS_FIELD_GET_TARGET_DESC = "param.class.field.get-target.desc";
    public static final String PARAM_CLASS_FIELD_SET_DESC = "param.class.field.set.desc";
    public static final String PARAM_CLASS_FIELD_SET_TARGET_DESC = "param.class.field.set-target.desc";
    public static final String PARAM_CLASS_FIELD_SET_VALUE_DESC = "param.class.field.set-value.desc";

    // --- class constructor ---
    public static final String PARAM_CLASS_CONSTRUCTOR_CLASS_DESC = "param.class.constructor.class.desc";
    public static final String PARAM_CLASS_CONSTRUCTOR_FREE_DESC = "param.class.constructor.free.desc";

    // --- class reflect ---
    public static final String PARAM_CLASS_REFLECT_CLASS_DESC = "param.class.reflect.class.desc";
    public static final String PARAM_CLASS_REFLECT_OPERATION_DESC = "param.class.reflect.operation.desc";
    public static final String PARAM_CLASS_REFLECT_MEMBER_DESC = "param.class.reflect.member.desc";
    public static final String PARAM_CLASS_REFLECT_SUPER_DESC = "param.class.reflect.super.desc";
    public static final String PARAM_CLASS_REFLECT_INTERFACES_DESC = "param.class.reflect.interfaces.desc";
    public static final String PARAM_CLASS_REFLECT_RAW_DESC = "param.class.reflect.raw.desc";

    // --- class hierarchy ---
    public static final String PARAM_CLASS_HIERARCHY_CLASS_DESC = "param.class.hierarchy.class.desc";

    private ClassTexts() {
    }

    /**
     * 由 {@code CliTexts} 的静态块调用。必须是 public —— 它在另一个包里。
     * 命名上刻意带 register 而不是「构造时自己注册」：登记动作集中在 CliTexts 一处，
     * 「哪些族登记了」才看得全，漏登记也能被守卫测试发现。
     */
    public static void register(Map<String, String> zh, Map<String, String> en) {
        zh.put(CMD_CLASS_DESC, "查看类的详细信息, 包括继承关系, 接口, 构造函数等");
        en.put(CMD_CLASS_DESC, "Inspect class details: super classes, interfaces, constructors and more");

        zh.put(ROUTE_CLASS_INFO_DESC, "查看类的详细信息");
        en.put(ROUTE_CLASS_INFO_DESC, "Show detailed information about a class");

        zh.put(ROUTE_CLASS_GRAPH_DESC, "生成类继承图");
        en.put(ROUTE_CLASS_GRAPH_DESC, "Generate a class inheritance graph");

        zh.put(ROUTE_CLASS_ANALYZE_DESC, "分析类的字段和方法");
        en.put(ROUTE_CLASS_ANALYZE_DESC, "Analyze the fields and methods of a class");

        zh.put(ROUTE_CLASS_LIST_DESC, "列出一个类的所有方法");
        en.put(ROUTE_CLASS_LIST_DESC, "List all methods of a class");

        zh.put(ROUTE_CLASS_INVOKE_DESC, "调用类中的方法");
        en.put(ROUTE_CLASS_INVOKE_DESC, "Invoke a method of a class");

        zh.put(ROUTE_CLASS_FIELD_DESC, "查看或操作字段");
        en.put(ROUTE_CLASS_FIELD_DESC, "Inspect or modify fields");

        zh.put(ROUTE_CLASS_CONSTRUCTOR_DESC, "创建类的实例");
        en.put(ROUTE_CLASS_CONSTRUCTOR_DESC, "Create an instance of a class");

        zh.put(ROUTE_CLASS_REFLECT_DESC, "使用反射访问和操作类的私有成员");
        en.put(ROUTE_CLASS_REFLECT_DESC, "Access and modify private members through reflection");

        zh.put(ROUTE_CLASS_HIERARCHY_DESC, "查看类的继承层次结构");
        en.put(ROUTE_CLASS_HIERARCHY_DESC, "Show the class inheritance hierarchy");

        zh.put(SUB_CLASS_INFO_DESC, "查看类的详细信息, 包括字段, 方法, 构造函数, 接口等.");
        en.put(SUB_CLASS_INFO_DESC, "Show detailed information about a class: fields, methods, constructors, interfaces and more.");

        // optionsDesc 是多行文本块，整块一个 id（含缩进，逐字节照抄原注解里的内容）
        zh.put(SUB_CLASS_INFO_OPTIONS, """
                选项:
                  -v, --verbose       显示详细信息
                  -i, --interfaces    显示实现的接口
                  -c, --constructors  显示构造函数
                  -s, --super         显示父类信息
                  -m, --modifiers     显示修饰符信息
                  -a, --all           显示所有信息 (默认)
                """);
        en.put(SUB_CLASS_INFO_OPTIONS, """
                Options:
                  -v, --verbose       Show detailed information
                  -i, --interfaces    Show implemented interfaces
                  -c, --constructors  Show constructors
                  -s, --super         Show super class information
                  -m, --modifiers     Show modifier information
                  -a, --all           Show all information (default)
                """);

        zh.put(SUB_CLASS_GRAPH_DESC, "生成类的继承关系图.");
        en.put(SUB_CLASS_GRAPH_DESC, "Generate a class inheritance graph.");

        zh.put(SUB_CLASS_GRAPH_OPTIONS, """
                选项:
                    --no-subclasses    不显示子类
                    --no-interfaces    不显示接口
                    --compact          紧凑模式输出
                    --depth <N>       最大遍历深度 (默认10)
                """);
        en.put(SUB_CLASS_GRAPH_OPTIONS, """
                Options:
                    --no-subclasses    Hide subclasses
                    --no-interfaces    Hide interfaces
                    --compact          Compact output
                    --depth <N>        Maximum traversal depth (default 10)
                """);

        zh.put(SUB_CLASS_ANALYZE_DESC, "深度分析类的结构, 生成详细的字段和方法报告, 比如继承和实现性之类的");
        en.put(SUB_CLASS_ANALYZE_DESC, "Analyze a class in depth and produce a detailed report on its fields and methods, covering inheritance and implementations");

        zh.put(SUB_CLASS_ANALYZE_OPTIONS, """
                选项:
                    -v, --verbose        显示详细信息
                    -f, --fields         只显示字段
                    -m, --methods        只显示方法
                    -c, --constructors   显示构造函数
                    -i, --interfaces     显示实现的接口
                    -s, --super          显示父类信息
                    --modifiers          显示修饰符信息
                    --hierarchy          显示继承层次
                    --stats              显示统计信息
                    --raw                原始输出 (JSON格式)
                    -a, --all            显示所有信息 (默认)
                """);
        en.put(SUB_CLASS_ANALYZE_OPTIONS, """
                Options:
                    -v, --verbose        Show detailed information
                    -f, --fields         Show fields only
                    -m, --methods        Show methods only
                    -c, --constructors   Show constructors
                    -i, --interfaces     Show implemented interfaces
                    -s, --super          Show super class information
                    --modifiers          Show modifier information
                    --hierarchy          Show the inheritance hierarchy
                    --stats              Show statistics
                    --raw                Raw output (JSON)
                    -a, --all            Show all information (default)
                """);

        zh.put(SUB_CLASS_LIST_DESC, "列出一个类的所有方法，支持按修饰符、返回类型等筛选");
        en.put(SUB_CLASS_LIST_DESC, "List all methods of a class, filterable by modifier, return value and more");

        zh.put(SUB_CLASS_LIST_OPTIONS, """
                选项:
                  -v, --verbose       显示详细信息（参数、异常等）
                """);
        en.put(SUB_CLASS_LIST_OPTIONS, """
                Options:
                  -v, --verbose       Show detailed information (parameters, exceptions, ...)
                """);

        zh.put(SUB_CLASS_INVOKE_DESC, "调用类的静态方法或创建实例后调用实例方法");
        en.put(SUB_CLASS_INVOKE_DESC, "Invoke a static method, or create an instance and invoke an instance method on it");

        zh.put(SUB_CLASS_INVOKE_OPTIONS, """
                参数支持表达式语法，可以直接写值或使用类型提示。

                参数格式:
                    - 直接表达式: 123, "hello", true, null
                    - 带类型提示: int:123, String:"hello", boolean:true

                表达式支持:
                    - 字面量: 123, 3.14, "text", true, null
                    - 算术运算: 1 + 2, 10 * 5
                    - 字符串拼接: "Hello " + "World"
                    - 方法调用: Math.abs(-5)
                    - 字段访问: SomeClass.FIELD
                    - 对象创建: new ArrayList()
                    - 三元运算: x > 0 ? x : -x

                选项:
                    --super       查找父类方法
                    --interfaces  查找接口方法
                    -s            调用静态方法
                    -f, --free    自由模式（跳过类型推断）
                """);
        en.put(SUB_CLASS_INVOKE_OPTIONS, """
                Parameters accept expression syntax: write the value directly, or prefix it with a type hint.

                Parameter formats:
                    - Plain expression: 123, "hello", true, null
                    - With type hint: int:123, String:"hello", boolean:true

                Expressions support:
                    - Literals: 123, 3.14, "text", true, null
                    - Arithmetic: 1 + 2, 10 * 5
                    - String concatenation: "Hello " + "World"
                    - Method calls: Math.abs(-5)
                    - Field access: SomeClass.FIELD
                    - Object creation: new ArrayList()
                    - Ternary: x > 0 ? x : -x

                Options:
                    --super       Look for the method in super classes
                    --interfaces  Look for the method in interfaces
                    -s            Invoke a static method
                    -f, --free    Free mode (skip type inference)
                """);

        zh.put(SUB_CLASS_FIELD_DESC, "查看或修改类的字段值，支持静态字段和实例字段");
        en.put(SUB_CLASS_FIELD_DESC, "Read or write a class field; both static and instance fields are supported");

        zh.put(SUB_CLASS_FIELD_OPTIONS, """
                操作符:
                    get, --get, -g         获取字段值 (需提供字段名)
                    set, --set, -s         设置字段值 (格式: set <field> <value>)

                目标选项:
                    --class               目标类名 (位置参数 position=1)
                    --instance, -i        目标实例表达式 (用于非静态字段)

                显示选项:
                    -v, --value           显示字段值
                    -t, --type            显示字段类型
                    -m, --modifiers       显示修饰符
                    -a, --all             显示所有信息 (默认)

                访问控制:
                    --super               访问父类字段
                    --interfaces          访问接口字段
                    --static-only         仅静态字段
                """);
        en.put(SUB_CLASS_FIELD_OPTIONS, """
                Operators:
                    get, --get, -g         Read a field value (the field name is required)
                    set, --set, -s         Write a field value (format: set <field> <value>)

                Target options:
                    --class               Target class name (positional parameter position=1)
                    --instance, -i        Target instance expression (for instance fields)

                Display options:
                    -v, --value           Show the field value
                    -t, --type            Show the field type
                    -m, --modifiers       Show modifiers
                    -a, --all             Show all information (default)

                Access control:
                    --super               Access fields declared in super classes
                    --interfaces          Access fields declared in interfaces
                    --static-only         Static fields only
                """);

        zh.put(SUB_CLASS_CONSTRUCTOR_DESC, "创建类的实例, 调用构造函数并返回结果");
        en.put(SUB_CLASS_CONSTRUCTOR_DESC, "Create an instance of a class by calling a constructor and return the result");

        zh.put(SUB_CLASS_CONSTRUCTOR_USAGE, "class constructor [选项] <class_name> [args...]");
        en.put(SUB_CLASS_CONSTRUCTOR_USAGE, "class constructor [options] <class_name> [args...]");

        zh.put(SUB_CLASS_CONSTRUCTOR_OPTIONS, """
                参数支持表达式语法，可以直接写值或使用类型提示。

                参数格式:
                    - 直接表达式: 123, "hello", true, null
                    - 带类型提示: int:123, String:"hello", boolean:true

                表达式支持:
                    - 字面量: 114514, 3.14, "text", true, null
                    - 算术运算: 1 + 2, 10 * 5
                    - 字符串拼接: "Hello " + "World"
                    - 方法调用: Math.abs(-5)
                    - 字段访问: SomeClass.FIELD
                    - 对象创建: new ArrayList()

                选项:
                    -f, --free      自由模式（跳过类型推断）
                """);
        en.put(SUB_CLASS_CONSTRUCTOR_OPTIONS, """
                Parameters accept expression syntax: write the value directly, or prefix it with a type hint.

                Parameter formats:
                    - Plain expression: 123, "hello", true, null
                    - With type hint: int:123, String:"hello", boolean:true

                Expressions support:
                    - Literals: 114514, 3.14, "text", true, null
                    - Arithmetic: 1 + 2, 10 * 5
                    - String concatenation: "Hello " + "World"
                    - Method calls: Math.abs(-5)
                    - Field access: SomeClass.FIELD
                    - Object creation: new ArrayList()

                Options:
                    -f, --free      Free mode (skip type inference)
                """);

        zh.put(SUB_CLASS_REFLECT_DESC, "使用统一的反射接口访问和操作类的私有成员.");
        en.put(SUB_CLASS_REFLECT_DESC, "Access and modify private members of a class through one unified reflection interface.");

        zh.put(SUB_CLASS_REFLECT_OPTIONS, """
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
                """);
        en.put(SUB_CLASS_REFLECT_OPTIONS, """
                Type (the type parameter):
                    field        - Read/write a field value
                    method       - Invoke a method
                    constructor  - Create an instance
                    static       - Access a static member

                Options:
                    -v, --value <value>      Set the field value
                    -p, --params <args>      Method parameters (space separated)
                    -s, --super             Access members declared in super classes
                    -i, --interfaces         Access members declared in interfaces
                    -r, --raw                Raw output (no formatting)
                """);

        zh.put(PARAM_CLASS_INFO_CLASS_DESC, "类名");
        en.put(PARAM_CLASS_INFO_CLASS_DESC, "Class name");

        zh.put(PARAM_CLASS_INFO_VERBOSE_DESC, "显示详细信息");
        en.put(PARAM_CLASS_INFO_VERBOSE_DESC, "Show detailed information");

        zh.put(PARAM_CLASS_INFO_INTERFACES_DESC, "显示实现的接口");
        en.put(PARAM_CLASS_INFO_INTERFACES_DESC, "Show implemented interfaces");

        zh.put(PARAM_CLASS_INFO_CONSTRUCTORS_DESC, "显示构造函数");
        en.put(PARAM_CLASS_INFO_CONSTRUCTORS_DESC, "Show constructors");

        zh.put(PARAM_CLASS_INFO_SUPER_DESC, "显示父类信息");
        en.put(PARAM_CLASS_INFO_SUPER_DESC, "Show super class information");

        zh.put(PARAM_CLASS_INFO_MODIFIERS_DESC, "显示修饰符信息");
        en.put(PARAM_CLASS_INFO_MODIFIERS_DESC, "Show modifier information");

        zh.put(PARAM_CLASS_INFO_ALL_DESC, "显示所有信息 (默认)");
        en.put(PARAM_CLASS_INFO_ALL_DESC, "Show all information (default)");

        zh.put(PARAM_CLASS_GRAPH_CLASS_DESC, "类名");
        en.put(PARAM_CLASS_GRAPH_CLASS_DESC, "Class name");

        zh.put(PARAM_CLASS_GRAPH_NO_SUBCLASSES_DESC, "隐藏子类");
        en.put(PARAM_CLASS_GRAPH_NO_SUBCLASSES_DESC, "Hide subclasses");

        zh.put(PARAM_CLASS_GRAPH_NO_INTERFACES_DESC, "隐藏接口");
        en.put(PARAM_CLASS_GRAPH_NO_INTERFACES_DESC, "Hide interfaces");

        zh.put(PARAM_CLASS_GRAPH_COMPACT_DESC, "紧凑模式");
        en.put(PARAM_CLASS_GRAPH_COMPACT_DESC, "Compact output");

        zh.put(PARAM_CLASS_GRAPH_DEPTH_DESC, "最大深度 (支持: --depth=10 或 --depth 10)");
        en.put(PARAM_CLASS_GRAPH_DEPTH_DESC, "Maximum depth (accepts --depth=10 as well as --depth 10)");

        zh.put(PARAM_CLASS_ANALYZE_CLASS_DESC, "类名");
        en.put(PARAM_CLASS_ANALYZE_CLASS_DESC, "Class name");

        zh.put(PARAM_CLASS_ANALYZE_FIELDS_DESC, "显示字段");
        en.put(PARAM_CLASS_ANALYZE_FIELDS_DESC, "Show fields");

        zh.put(PARAM_CLASS_ANALYZE_METHODS_DESC, "显示方法");
        en.put(PARAM_CLASS_ANALYZE_METHODS_DESC, "Show methods");

        zh.put(PARAM_CLASS_ANALYZE_CONSTRUCTORS_DESC, "显示构造函数");
        en.put(PARAM_CLASS_ANALYZE_CONSTRUCTORS_DESC, "Show constructors");

        zh.put(PARAM_CLASS_ANALYZE_INTERFACES_DESC, "显示实现的接口");
        en.put(PARAM_CLASS_ANALYZE_INTERFACES_DESC, "Show implemented interfaces");

        zh.put(PARAM_CLASS_ANALYZE_SUPER_DESC, "显示父类信息");
        en.put(PARAM_CLASS_ANALYZE_SUPER_DESC, "Show super class information");

        zh.put(PARAM_CLASS_ANALYZE_MODIFIERS_DESC, "显示修饰符信息");
        en.put(PARAM_CLASS_ANALYZE_MODIFIERS_DESC, "Show modifier information");

        zh.put(PARAM_CLASS_ANALYZE_ALL_DESC, "显示所有信息 (默认)");
        en.put(PARAM_CLASS_ANALYZE_ALL_DESC, "Show all information (default)");

        zh.put(PARAM_CLASS_ANALYZE_VERBOSE_DESC, "显示详细信息");
        en.put(PARAM_CLASS_ANALYZE_VERBOSE_DESC, "Show detailed information");

        zh.put(PARAM_CLASS_ANALYZE_HIERARCHY_DESC, "显示继承层次");
        en.put(PARAM_CLASS_ANALYZE_HIERARCHY_DESC, "Show the inheritance hierarchy");

        zh.put(PARAM_CLASS_ANALYZE_STATS_DESC, "显示统计信息");
        en.put(PARAM_CLASS_ANALYZE_STATS_DESC, "Show statistics");

        zh.put(PARAM_CLASS_ANALYZE_RAW_DESC, "原始输出格式");
        en.put(PARAM_CLASS_ANALYZE_RAW_DESC, "Raw output format");

        zh.put(PARAM_CLASS_LIST_CLASS_DESC, "类名");
        en.put(PARAM_CLASS_LIST_CLASS_DESC, "Class name");

        zh.put(PARAM_CLASS_LIST_VERBOSE_DESC, "显示详细信息（参数、异常等）");
        en.put(PARAM_CLASS_LIST_VERBOSE_DESC, "Show detailed information (parameters, exceptions, ...)");

        zh.put(PARAM_CLASS_INVOKE_CLASS_DESC, "类名");
        en.put(PARAM_CLASS_INVOKE_CLASS_DESC, "Class name");

        zh.put(PARAM_CLASS_INVOKE_METHOD_DESC, "方法名");
        en.put(PARAM_CLASS_INVOKE_METHOD_DESC, "Method name");

        zh.put(PARAM_CLASS_INVOKE_STATIC_DESC, "静态方法");
        en.put(PARAM_CLASS_INVOKE_STATIC_DESC, "Static method");

        zh.put(PARAM_CLASS_INVOKE_FREE_DESC, "自由模式");
        en.put(PARAM_CLASS_INVOKE_FREE_DESC, "Free mode");

        zh.put(PARAM_CLASS_INVOKE_SUPER_DESC, "访问父类成员");
        en.put(PARAM_CLASS_INVOKE_SUPER_DESC, "Access members declared in super classes");

        zh.put(PARAM_CLASS_INVOKE_INTERFACES_DESC, "访问接口成员");
        en.put(PARAM_CLASS_INVOKE_INTERFACES_DESC, "Access members declared in interfaces");

        zh.put(PARAM_CLASS_INVOKE_INSTANCE_DESC, "实例表达式（用于非静态方法的实例）");
        en.put(PARAM_CLASS_INVOKE_INSTANCE_DESC, "Instance expression (the receiver of an instance method)");

        zh.put(PARAM_CLASS_FIELD_CLASS_DESC, "目标类名");
        en.put(PARAM_CLASS_FIELD_CLASS_DESC, "Target class name");

        zh.put(PARAM_CLASS_FIELD_INSTANCE_DESC, "目标实例表达式 (用于操作非静态字段, 支持代码表达式)");
        en.put(PARAM_CLASS_FIELD_INSTANCE_DESC, "Target instance expression (for instance fields, accepts code expressions)");

        zh.put(PARAM_CLASS_FIELD_VALUE_DESC, "显示字段值");
        en.put(PARAM_CLASS_FIELD_VALUE_DESC, "Show the field value");

        zh.put(PARAM_CLASS_FIELD_TYPE_DESC, "显示字段类型");
        en.put(PARAM_CLASS_FIELD_TYPE_DESC, "Show the field type");

        zh.put(PARAM_CLASS_FIELD_MODIFIERS_DESC, "显示修饰符");
        en.put(PARAM_CLASS_FIELD_MODIFIERS_DESC, "Show modifiers");

        zh.put(PARAM_CLASS_FIELD_ALL_DESC, "显示所有信息 (默认)");
        en.put(PARAM_CLASS_FIELD_ALL_DESC, "Show all information (default)");

        zh.put(PARAM_CLASS_FIELD_SUPER_DESC, "访问父类字段");
        en.put(PARAM_CLASS_FIELD_SUPER_DESC, "Access fields declared in super classes");

        zh.put(PARAM_CLASS_FIELD_INTERFACES_DESC, "访问接口字段");
        en.put(PARAM_CLASS_FIELD_INTERFACES_DESC, "Access fields declared in interfaces");

        zh.put(PARAM_CLASS_FIELD_STATIC_DESC, "仅静态字段");
        en.put(PARAM_CLASS_FIELD_STATIC_DESC, "Static fields only");

        zh.put(PARAM_CLASS_FIELD_GET_DESC, "获取字段值 (支持: --get name / get name / -g name)");
        en.put(PARAM_CLASS_FIELD_GET_DESC, "Read a field value (accepts --get name / get name / -g name)");

        zh.put(PARAM_CLASS_FIELD_GET_TARGET_DESC, "要获取的字段名");
        en.put(PARAM_CLASS_FIELD_GET_TARGET_DESC, "Name of the field to read");

        zh.put(PARAM_CLASS_FIELD_SET_DESC, "设置字段值 (支持: --set name val / set name val / -s name val)");
        en.put(PARAM_CLASS_FIELD_SET_DESC, "Write a field value (accepts --set name val / set name val / -s name val)");

        zh.put(PARAM_CLASS_FIELD_SET_TARGET_DESC, "要设置的字段名");
        en.put(PARAM_CLASS_FIELD_SET_TARGET_DESC, "Name of the field to write");

        zh.put(PARAM_CLASS_FIELD_SET_VALUE_DESC, "要设置的值");
        en.put(PARAM_CLASS_FIELD_SET_VALUE_DESC, "Value to write");

        zh.put(PARAM_CLASS_CONSTRUCTOR_CLASS_DESC, "类名");
        en.put(PARAM_CLASS_CONSTRUCTOR_CLASS_DESC, "Class name");

        zh.put(PARAM_CLASS_CONSTRUCTOR_FREE_DESC, "自由模式（使用表达式语法）");
        en.put(PARAM_CLASS_CONSTRUCTOR_FREE_DESC, "Free mode (uses expression syntax)");

        zh.put(PARAM_CLASS_REFLECT_CLASS_DESC, "类名");
        en.put(PARAM_CLASS_REFLECT_CLASS_DESC, "Class name");

        zh.put(PARAM_CLASS_REFLECT_OPERATION_DESC, "操作类型");
        en.put(PARAM_CLASS_REFLECT_OPERATION_DESC, "Operation type");

        zh.put(PARAM_CLASS_REFLECT_MEMBER_DESC, "成员名称");
        en.put(PARAM_CLASS_REFLECT_MEMBER_DESC, "Member name");

        zh.put(PARAM_CLASS_REFLECT_SUPER_DESC, "访问父类成员");
        en.put(PARAM_CLASS_REFLECT_SUPER_DESC, "Access members declared in super classes");

        zh.put(PARAM_CLASS_REFLECT_INTERFACES_DESC, "访问接口成员");
        en.put(PARAM_CLASS_REFLECT_INTERFACES_DESC, "Access members declared in interfaces");

        zh.put(PARAM_CLASS_REFLECT_RAW_DESC, "原始输出格式");
        en.put(PARAM_CLASS_REFLECT_RAW_DESC, "Raw output format");

        zh.put(PARAM_CLASS_HIERARCHY_CLASS_DESC, "类名");
        en.put(PARAM_CLASS_HIERARCHY_CLASS_DESC, "Class name");
    }
}
