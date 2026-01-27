// SharedPreferences监控Hook - Before阶段
// 使用方法：hook add android.app.ContextImpl getSharedPreferences before codebase sharedpreferences_monitor.java
// 脚本默认位于：/data/local/tmp/methods/scripts/
//
// 注意：这个脚本会监视所有SharedPreferences的访问

auto param = getParam();
auto phase = getPhase();
auto hookId = getHookId();

println("[" + hookId + "][" + phase + "] SharedPreferences 被访问");

// 获取 SharedPreferences 名称
if (param.args.length > 0) {
    auto name = param.args[0];
    if (name != null) {
        println("  名称: " + name);
    }
}

// 获取 Context
auto thisObject = param.thisObject;
if (thisObject != null) {
    auto packageName = thisObject.getPackageName();
    println("  包名: " + packageName);
}

// 获取调用栈信息
auto stackTrace = new java.lang.Exception().getStackTrace();
if (stackTrace != null && stackTrace.length > 2) {
    auto caller = stackTrace[2];
    println("  调用位置: " + caller.getClassName() + "." + caller.getMethodName() + " (行 " + caller.getLineNumber() + ")");
}