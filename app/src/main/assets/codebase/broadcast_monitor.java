// BroadcastReceiver监控Hook - Before阶段
// 使用方法：hook add android.app.ActivityThread handleReceiver before codebase broadcast_monitor.java
// 脚本默认位于：/data/local/tmp/methods/scripts/
//
// 注意：这个脚本会监视所有BroadcastReceiver的接收

auto param = getParam();
auto phase = getPhase();
auto hookId = getHookId();

println("[" + hookId + "][" + phase + "] BroadcastReceiver 被触发");

// 获取 Intent
if (param.args.length > 1) {
    auto intent = param.args[1];
    if (intent != null) {
        // 获取Action
        auto action = intent.getAction();
        println("  Action: " + action);

        // 获取Extra
        auto extras = intent.getExtras();
        if (extras != null && extras.size() > 0) {
            println("  Extras 数量: " + extras.size());
            auto keySet = extras.keySet();
            for (auto key : keySet) {
                auto value = extras.get(key);
                println("    " + key + " = " + value);
            }
        }

        // 获取组件名
        auto component = intent.getComponent();
        if (component != null) {
            println("  组件名: " + component.toShortString());
        }
    }
}

// 获取调用栈信息
auto stackTrace = new java.lang.Exception().getStackTrace();
if (stackTrace != null && stackTrace.length > 2) {
    auto caller = stackTrace[2];
    println("  调用位置: " + caller.getClassName() + "." + caller.getMethodName() + " (行 " + caller.getLineNumber() + ")");
}