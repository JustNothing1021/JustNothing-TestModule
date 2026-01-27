// Intent监控Hook - Before阶段
// 使用方法：hook add android.content.Intent <init> before codebase intent_monitor.java
// 脚本默认位于：/data/local/tmp/methods/scripts/
//
// 注意：这个脚本会监视所有Intent的创建

auto param = getParam();
auto phase = getPhase();
auto hookId = getHookId();

println("[" + hookId + "][" + phase + "] Intent 被创建");

// 打印构造函数参数
if (param.args.length > 0) {
    println("  参数数量: " + param.args.length);

    // 尝试获取Action
    if (param.args[0] != null) {
        auto argClass = param.args[0].getClass();
        auto className = argClass.toString();
        if (className.equals("class java.lang.String")) {
            println("  Action: " + param.args[0]);
        }
    }

    // 尝试获取其他参数
    for (auto i = 1; i < param.args.length; i++) {
        if (param.args[i] != null) {
            auto argClass = param.args[i].getClass();
            println("  参数[" + i + "]: " + argClass.toString() + " = " + param.args[i]);
        }
    }
}

// 获取调用栈信息
try {
    auto ex = new java.lang.Exception();
    auto stackTrace = ex.getStackTrace();
    if (stackTrace != null && stackTrace.length > 2) {
        auto caller = stackTrace[2];
        println("  调用位置: " + caller.getClassName() + "." + caller.getMethodName());
    }
} catch (auto e) {
    println("  无法获取调用栈");
}