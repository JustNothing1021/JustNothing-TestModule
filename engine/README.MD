懒得写md了，直接复制吧（

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
