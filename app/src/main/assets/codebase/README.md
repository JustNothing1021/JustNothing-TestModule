# Hook 示例脚本

本目录包含各种 Hook 监控示例脚本，用于演示如何使用 hook 命令监控 Android 系统中的各种事件。

## 使用方法

所有脚本默认位于：`/data/local/tmp/methods/scripts/`

基本语法：
```
hook add <类名> <方法名> [sig <签名>] [before|after|replace] (code <代码> | codebase <文件名>)
```

## 示例脚本列表

### 1. activity_launch_monitor.java
监控 Activity 的启动事件。

**使用方法：**
```
hook add android.app.ActivityThread performLaunchActivity after codebase activity_launch_monitor.java
```

**监控内容：**
- Activity 类名
- 包名
- 组件名
- 任务ID
- 参数信息

---

### 2. toast_monitor.java
监控 Toast 消息的创建。

**使用方法：**
```
hook add android.widget.Toast makeText before codebase toast_monitor.java
```

**监控内容：**
- Context 类型
- 消息内容
- 显示时长
- 调用位置

---

### 3. intent_monitor.java
监控 Intent 的创建。

**使用方法：**
```
hook add android.content.Intent "<init>" before codebase intent_monitor.java
```

**监控内容：**
- 参数数量和类型
- Action
- 其他参数
- 调用位置

---

### 4. view_click_monitor.java
监控 View 的点击事件。

**使用方法：**
```
hook add android.view.View performClick before codebase view_click_monitor.java
```

**监控内容：**
- View 类型
- View ID
- 内容描述
- 文本内容（如果是 TextView）
- 调用位置

---

### 5. service_create_monitor.java
监控 Service 的创建。

**使用方法：**
```
hook add android.app.ActivityThread handleCreateService before codebase service_create_monitor.java
```

**监控内容：**
- ServiceInfo 类型
- 包名
- Service 名称
- 进程名

---

### 6. broadcast_monitor.java
监控 BroadcastReceiver 的接收。

**使用方法：**
```
hook add android.app.ActivityThread handleReceiver before codebase broadcast_monitor.java
```

**监控内容：**
- Action
- Extras 数据
- 组件名
- 调用位置

---

### 7. network_monitor.java
监控基于 java.net.URL 的网络请求。

**使用方法：**
```
hook add java.net.URL openConnection before codebase network_monitor.java
```

**监控内容：**
- URL 地址
- 调用位置

---

### 8. sharedpreferences_monitor.java
监控 SharedPreferences 的访问。

**使用方法：**
```
hook add android.app.ContextImpl getSharedPreferences before codebase sharedpreferences_monitor.java
```

**监控内容：**
- SharedPreferences 名称
- 包名
- 调用位置

---

### 9. modify_return_value.java
演示如何修改方法的返回值。

**使用方法：**
```
hook add java.lang.System currentTimeMillis replace codebase modify_return_value.java
```

**功能：**
- 将 System.currentTimeMillis() 的返回值修改为固定值（2025-01-01 00:00:00）
- 用于测试时间相关的功能

## 内置函数

所有脚本都可以使用以下内置函数：

- `getParam()` - 获取当前 Hook 的参数（MethodHookParam）
- `getPhase()` - 获取当前 Hook 的阶段（before/after/replace）
- `getHookId()` - 获取当前 Hook 的 ID
- `setReturnValue(Object value)` - 设置返回值（仅在 replace 阶段有效）
- `setThrowable(Throwable throwable)` - 抛出异常
- `getReturnValue()` - 获取返回值
- `println(String s)` - 打印信息并换行

## 注意事项

1. 这些脚本会监控系统级别的事件，可能会产生大量输出
2. 建议在测试环境中使用，避免在生产环境使用
3. 某些 Hook 可能会影响系统性能
4. 使用 `hook remove <id>` 命令可以移除不需要的 Hook
5. 使用 `hook disable <id>` 命令可以临时禁用 Hook

## 示例工作流

1. 添加 Hook：
   ```
   hook add android.app.ActivityThread performLaunchActivity after codebase activity_launch_monitor.java
   ```

2. 查看所有 Hook：
   ```
   hook list
   ```

3. 查看 Hook 输出：
   ```
   hook output <hook_id>
   ```

4. 移除 Hook：
   ```
   hook remove <hook_id>
   ```

## 高级用法

### 组合多个阶段

可以在同一个 Hook 中使用多个阶段：

```
hook add com.example.MyClass myMethod before code 'println("before");' after code 'println("after");'
```

### 使用脚本名称作为 codebase

如果脚本已经在 scripts 目录中，可以直接使用脚本名称：

```
hook add android.app.ActivityThread performLaunchActivity after codebase activity_launch_monitor.java
```

### 修改方法参数

在 before 阶段可以修改方法参数：

```
hook add com.example.MyClass myMethod before code 'param.args[0] = "new_value";'
```

### 阻止方法执行

在 replace 阶段可以阻止原始方法执行：

```
hook add com.example.MyClass myMethod replace code 'setReturnValue(null);'
```

## 故障排除

如果 Hook 不生效，请检查：

1. 类名和方法名是否正确
2. 方法签名是否匹配（使用 `sig` 参数指定）
3. Hook 是否已启用（使用 `hook enable <id>`）
4. 查看日志输出以获取更多信息

## 更多信息

使用 `hook` 命令查看完整的帮助信息：
```
hook
```