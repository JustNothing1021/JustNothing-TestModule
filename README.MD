# JustNothing TestModule

JustNothing1021 (真的啥也不是啊) 的测试模块。

~~终于会写MD了!!!~~

## 这是个啥东西？

如果你是一个新手安卓开发者，想必你也会有下面的一些疑问：

 - 我的代码是怎么跑起来的？
 - 系统到底在我背后干些什么？
 - 系统到底是怎么操作我写的源码的？

如果对此抱有好奇的话，这个模块或许能帮到你！

### 概述

这个模块是我在研究小棺材 (小天才电话手表) 时的副产物之一。

含有AIGC（人工智能生成的内容）。

它是一个**Xposed模块**。

它同时有用户界面和命令行界面。

具有的功能:
 - 解除小棺材手表的系统检测 (当然，现在只是次要功能了)
 - 分析系统类的构成
 - 监测和分析系统行为
 - 整花活，比如炸掉系统（？）
 - 其他各种奇奇怪怪的玩法

简单示例 (**命令行界面**, 这玩意的GUI很丑):

```text
> adb shell

# methods help
Xposed Method CLI (Command Line Interface) Server端 v0.3.4
作者: JustNothing1021, DeepSeek 和 GLM-4.7

一个用来调试安卓开发千奇百怪的诡异问题的Xp模块/命令行工具.

命令语法: methods [options] <command> [args...]

可用命令:
  help                              - 显示所有命令的帮助或特定命令的帮助
  watch                             - 监控字段或方法的变化
  trace                             - 跟踪方法调用链
  profile                           - 性能分析
...

# methods sinteractive
====== 脚本交互执行模式 ======
输入 'exit' 或 'quit' 退出

>>> System.out.println("Never gonna " + new String[] {"give you up", "say goodbye"} [randomInt(0, 1)]);
Never gonna say goodbye
>>> analyze(com.android.server.am.ActivityManagerService);
=== Object Analysis ===
String: class com.android.server.am.ActivityManagerService
Class Name: java.lang.Class
Simple Name: Class
Package: java.lang
Is Array: false
Is Interface: false
Is Annotation: false
Is Enum: false
Is Primitive: false

=== Fields ===
  private transient int java.lang.Class.accessFlags = -2146959359
  private transient int java.lang.Class.classFlags = 0
  private transient ClassLoader java.lang.Class.classLoader = dalvik.system.PathClassLoader[DexPathList[[zip file "/system/framework/services.jar", zip file "/system/framework/ethernet-service.jar", zip file "/system/framework/wifi-service.jar", zip file "/system/framework/com.android.location.provider.jar"],nativeLibraryDirectories=[/system/lib, /vendor/lib, /system/lib, /vendor/lib]]]
  private transient int java.lang.Class.classSize = 3075
...
>>> exit
Say goodbye~~~
交互式模式已退出
```

相信你会感兴趣的！

### 面向对象

像我这样又菜又爱玩的好奇宝宝。

## 使用教程

### 如果你想马上就用上

上[Release](https://github.com/JustNothing1021/JustNothing-TestModule/releases)下个最新版本!

具体方法:

(1) 安装应用apk
 - 下载最新release中的build-release-vx.x.x.apk (虽然大概率其实是debug)
 - 如果你使用的是安卓设备下载, 直接安装就好!
 - 其他情况, 使用ADB安装:
```
adb install -r build-release-vx.x.x.apk
```

(2) 在Xposed管理器中激活这个Xposed模块

(3) 安装配套的Magisk模块
 - 下载最新release中的magisk-module.zip (如果没的话, 可以在项目里找找)
 - 如果你使用的是安卓设备下载, 直接用Magisk管理器安装就好!
 - 其他情况, 使用ADB安装:
```text
adb push magisk-module.zip /data/local/tmp
magisk --install-module /data/local/tmp/magisk-module.zip
```

(4) 如果你不用Magisk管理root (?)
 - 把magisk-module.zip中的system文件夹提取出来，将其与设备中的system合并
 - 再将zip中`common/binary`下符合ABI的可执行文件命名为`methods_client`复制在`system/framework`下即可!
 - 然后，更改一下可执行文件的权限
```text
chmod 0775 /system/bin/methods                 # Shell客户端文件
chmod 0775 /system/bin/methods-daemon          # 守护进程监视器
chmod 0775 /system/bin/methods_client          # C++客户端文件
chmod 0775 /system/bin/methods-concurrent-test # debug用，并发测试

# 顺便把数据目录也更改一下
mkdir /data/local/tmp/methods 
chmod -R 0775 /data/local/tmp/methods 
```

### 如果你想自己编译

用 `./gradlew :app:assembleDebug` 进行构建。

生成的文件位置:
 - APK: `app/build/outputs/apk/debug/app-debug.apk`
 - Magisk模块: `app/build/outputs/magisk-module.zip`

~~上Android Studio吧，懒人必备~~

(困了，下面让AI跟你们讲讲吧)

## 项目结构

```
TestModule/
├── app/                           # Android应用模块
│   ├── src/
│   │   ├── main/
│   │   │   ├── assets/            # 资源文件
│   │   │   │   ├── arm64-v8a/      # ARM64架构的C++客户端
│   │   │   │   ├── armeabi-v7a/  # ARM32架构的C++客户端
│   │   │   │   ├── x86/           # x86架构的C++客户端
│   │   │   │   ├── x86_64/        # x86_64架构的C++客户端
│   │   │   │   ├── codebase/       # 示例代码
│   │   │   │   └── ...           # 其他资源文件
│   │   │   ├── cpp/             # C++客户端源码
│   │   │   │   ├── CMakeLists.txt
│   │   │   │   └── methods_client.cpp
│   │   │   ├── java/            # Java源码
│   │   │   │   └── com/justnothing/
│   │   │   │       ├── methodsclient/      # 客户端代码
│   │   │   │       │   ├── executor/         # 命令执行器
│   │   │   │       │   ├── monitor/          # 监控器
│   │   │   │       │   └── StreamClient.java
│   │   │   │       ├── testmodule/       # Xposed模块代码
│   │   │   │       │   ├── command/         # 命令处理
│   │   │   │       │   ├── constants/       # 常量定义
│   │   │   │       │   ├── hooks/           # Xposed钩子
│   │   │   │       │   ├── service/         # 服务端代码
│   │   │   │       │   ├── ui/              # 用户界面
│   │   │   │       │   └── utils/           # 工具类
│   │   │   │       └── xtchttplib/       # HTTP库
│   │   │   └── res/             # Android资源
│   │   │       ├── anim/            # 动画
│   │   │       ├── drawable/        # 图片
│   │   │       ├── layout/          # 布局
│   │   │       ├── mipmap-*/       # 图标
│   │   │       ├── values/          # 值
│   │   │       └── xml/            # 配置
│   │   ├── libs/                 # 依赖库
│   │   │   ├── NCW-Logger-1.0.3-hotfix.jar
│   │   │   └── XposedBridgeApi.jar
│   │   └── build.gradle         # Gradle构建配置
├── magisk/                       # Magisk模块文件
│   ├── install.sh               # 安装脚本
│   └── scripts/                # Shell脚本
│       ├── methods              # 主脚本
│       ├── methods-daemon       # 守护进程
│       └── methods-concurrent-test # 并发测试
├── docs/                        # 文档
│   ├── magisk-module-template/ # Magisk模块模板
│   ├── CONCURRENT_HANDLING.md  # 并发处理文档
│   ├── HYBRID_SOLUTION.md      # 混合方案文档
│   ├── PROJECT_REFACTOR_SUMMARY.md  # 项目重构总结
│   └── PROJECT_STRUCTURE.md     # 项目结构文档
├── gradle/                      # Gradle配置
│   └── wrapper/
├── .github/                     # GitHub配置
│   └── workflows/
│       └── release.yml
├── .idea/                       # Android Studio配置
├── .vscode/                     # VS Code配置
├── .gitignore                    # Git忽略文件
├── README.MD                     # 项目说明
├── build.gradle                   # 根Gradle配置
├── gradle.properties              # Gradle属性
├── gradlew                      # Gradle包装器(Unix)
├── gradlew.bat                  # Gradle包装器(Windows)
└── settings.gradle                # Gradle设置
```

## 技术栈

### Android
 - **Xposed Framework** - 系统级钩子框架
 - **Magisk** - Root权限管理
 - **NDK** - C++原生开发
 - **Gradle** - 构建系统

### 后端
 - **Java** - Xposed模块和服务端
 - **C++** - 原生客户端
 - **Socket通信** - 客户端和服务端通信
 - **Binder IPC** - Android进程间通信

### 前端
 - **Java** - Android UI
 - **ViewBinding** - 视图绑定
 - **Material Design** - 材料设计

## 开发指南

### 环境要求
 - Android Studio Hedgehog | 2023.1.1 或更高
 - JDK 21 或更高
 - Android NDK r29.0.14206865
 - Gradle 8.14.3
 - Magisk v19.0 或更高
 - Xposed v80 或更高

### 构建步骤

1. 克隆仓库
```bash
git clone https://github.com/JustNothing1021/JustNothing-TestModule.git
cd JustNothing-TestModule
```

2. 构建APK
```bash
./gradlew assembleDebug
```

3. 构建Magisk模块
```bash
./gradlew :app:createMagiskZip
```

4. 安装
 - 将 `app/build/outputs/apk/debug/app-debug.apk` 安装到设备
 - 将 `app/build/outputs/magisk-module.zip` 通过Magisk安装

## 功能说明

### 命令行命令

#### 基础命令
 - `help` - 显示帮助信息
 - `version` - 显示版本信息
 - `exit` - 退出客户端

(更多命令见 methods help)

### Xposed Hook 框架

#### Package Hook
 - 在应用启动时加载
 - 拦截方法调用
 - 修改返回值
 - 访问私有字段

#### Zygote Hook
 - 在系统早期加载执行

## 架构说明

### 客户端-服务端架构

很抽象对吧

~~这个图是我自己画的，不是AI，如何呢~~

```

  输入/输出
┌─────────┐
│  客户端  │
│ (Shell) │
└──────▲──┘     ┌─────────┐
       │        │  app    │
命令行  ├────────▶ process │ --> 目前主要的瓶颈
       │        └──────▲──┘     破坏输入且开销大
       │               │ 
┌──────▼──┐     ┌──────▼──┐ 
│  客户端  │     │  客户端  │
│  (C++)  │     │  (Java) │
└──────▲──┘     └──────▲──┘ 
       ├───────────────┘
       │ Socket
       │
┌──────▼────────┐
│    服务端      │
│    (Java)     │
│   (Binder)    │
│ (注入android)  │
│               │
│  ┌────────┐   │
│  │ Xposed │   │
│  │ Hook   │   │
│  └───▲────┘   │
│      │        │
│   ┌──▼───┐    │
│   │ 系统  │    │
│   │ API  │    │
│   └──────┘    │
└───────────────┘
```

### 并发处理

使用 `ThreadLocal` 实现线程安全的命令执行器，每个线程独立的状态管理。

使用Java守护进程 + C++轻量级客户端的混合架构。


## 贡献指南

欢迎提交 Issue 和 Pull Request！（其实根本不会有人）

### 代码规范
 - 哈？还有这一说？

### 提交规范
 - feat, feature: 新功能
 - fix, bugfix: 修复bug
 - docs, document: 文档更新
 - style: 代码格式调整
 - refactor: 重构
 - test: 测试相关
 - chore: 构建/工具相关
 - i18n: 国际化支持 (加翻译啥的)

## 许可证

没有采用许可证（？）

## 致谢

感谢以下项目，库和开发者的支持：
 - [Xposed Framework](https://github.com/rovo89/Xposed)
 - [Magisk](https://github.com/topjohnwu/Magisk)
 - [DeepSeek](https://www.deepseek.com)
 - [GLM-4.7](https://www.zhipuai.cn)
 - [NoneColdWind](https://github.com/NoneColdWind)

## 联系方式

- 作者: JustNothing1021
- GitHub: https://github.com/JustNothing1021
- Email: ljy0909112022@163.com

## 更新日志

### v0.4.3
README.MD!!!!!
修复大量解释器问题

## 常见问题

### Q: 为什么有三个客户端？
A: Shell处理终端，Java通过app_process执行，体验好，C++处理轻量命令。

## 相关链接

- [Xposed Framework文档](https://github.com/rovo89/Xposed/wiki)
- [Magisk文档](https://topjohnwu.github.io/Magisk/)
- [Android开发者文档](https://developer.android.com/)
