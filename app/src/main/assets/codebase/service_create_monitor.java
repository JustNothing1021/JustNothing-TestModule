// Service启动监控Hook - Before阶段
// 使用方法：hook add android.app.ActivityThread handleCreateService before codebase service_create_monitor.java
// 脚本默认位于：/data/local/tmp/methods/scripts/
//
// 注意：这个脚本会监视所有Service的启动

auto param = getParam();
auto phase = getPhase();
auto hookId = getHookId();

println("[" + hookId + "][" + phase + "] Service 被创建");

// 获取 ServiceInfo
if (param.args.length > 0) {
    auto serviceInfo = param.args[0];
    if (serviceInfo != null) {
        auto serviceInfoClass = serviceInfo.getClass();
        println("  ServiceInfo 类型: " + serviceInfoClass.toString());

        // 获取包名
        auto packageName = serviceInfo.applicationInfo.packageName;
        println("  包名: " + packageName);

        // 获取Service名称
        auto name = serviceInfo.name;
        println("  Service 名称: " + name);

        // 获取进程名
        auto processName = serviceInfo.processName;
        println("  进程名: " + processName);
    }
}

// 获取调用栈信息
auto stackTrace = new java.lang.Exception().getStackTrace();
if (stackTrace != null && stackTrace.length > 2) {
    auto caller = stackTrace[2];
    println("  调用位置: " + caller.getClassName() + "." + caller.getMethodName() + " (行 " + caller.getLineNumber() + ")");
}