// Activity创建监控Hook - After阶段
// 使用方法：hook add android.app.ActivityThread performLaunchActivity after codebase activity_launch_monitor.java
// 脚本默认位于：/data/local/tmp/methods/scripts/
//
// 注意：这个脚本会监视所有Activity的启动，包括应用内和其他应用的Activity

auto param = getParam();
auto phase = getPhase();
auto hookId = getHookId();

println("[" + hookId + "][" + phase + "] Activity 启动");

// 获取 Activity 实例
auto result = param.result;
if (result != null) {
    // performLaunchActivity 的返回值是 Activity 实例
    auto activity = result;

    // 获取类名
    auto activityClass = activity.getClass();
    println("  Activity 类名: " + activityClass.toString());

    // 获取包名
    auto packageName = activity.getPackageName();
    println("  包名: " + packageName);

    // 获取组件名
    auto componentName = activity.getComponentName();
    if (componentName != null) {
        println("  组件名: " + componentName.toShortString());
    }

    // 获取任务ID
    auto taskId = activity.getTaskId();
    println("  任务ID: " + taskId);
} else {
    println("  Activity 启动失败");
}

// 打印参数信息（如果有）
if (param.args.length > 0) {
    println("  参数数量: " + param.args.length);
    for (auto i = 0; i < param.args.length; i++) {
        if (param.args[i] != null) {
            auto argClass = param.args[i].getClass();
            println("    参数[" + i + "]: " + argClass.toString());
        }
    }
}