# JustNothing TestModule

JustNothing1021 (真的啥也不是啊) 的测试模块。

~~终于会写MD了!!!~~

## 这是个啥东西？

> 我觉得一个相当贴切的项目描述: 一个高中生和朋友一起, 从零开始, 边学边做, 把所有觉得酷的东西都塞进去的"技术游乐场".

如果你是一个新手安卓开发者，想必你也会有下面的一些疑问：

- 我的代码是怎么跑起来的？
- 系统到底在我背后干些什么？
- 系统到底是怎么操作我写的源码的？

如果对此抱有好奇的话，这个模块或许能帮到你！

### 概述

这个模块是我在研究小棺材 (小天才电话手表) 时的副产物之一。

它是一个**Xposed模块**，同时有用户界面（但是很烂）和命令行界面。

含有AIGC（人工智能生成的内容）。

目前已经**移除了**对电话手表的所有 Hook 层面的特殊适配。如果有需要的话，可以安装自定义 HookPack（详情见 [HookPackLoader](app/src/main/java/com/justnothing/testmodule/hooks/HookPackLoader.java)）。

HookPack 严格意义上来说**并不在本项目的 MIT 许可范围内**。可以找作者要，但请不要把它大量分发，可能会造成侵权。毕竟这玩意主要还是一个系统分析工具来的。

具有的功能:

- 分析系统类的构成
- 监测和分析系统行为
- 整花活，比如炸掉系统（？）
- 其他各种奇奇怪怪的玩法

简单示例 (**命令行界面**, 这玩意的GUI很丑):

```text
> adb shell
# methods -r
启动 Java REPL 模式...
╔══════════════════════════════════════════════════╗
║   JustNothing Methods REPL - Interactive Shell   ║
╠══════════════════════════════════════════════════╣
║  Tab     补全命令/参数                           ║
║  ↑↓      历史记录                                ║
║  Ctrl+R  搜索历史                                ║
║  exit    退出                                    ║
╚══════════════════════════════════════════════════╝

methods> help
Xposed Method CLI (Command Line Interface) Server端 v0.4.0
作者: JustNothing1021, DeepSeek 和 GLM-4.7

一个用来调试安卓开发千奇百怪的诡异问题的Xp模块/命令行工具.

命令语法: methods [options] <command> [args...]

可用命令:
  help                               - 获取命令帮助信息
  watch                              - 监控字段或方法的变化, 非阻塞执行.
  trace                              - 跟踪方法调用链，生成调用树
  ...

methods> sinteractive
====== 脚本交互执行模式 =====
输入 'exit' 或 'quit' 退出 (你不会闲到拿这俩做变量名, 对吧)
输入 ':multi' 进入多行模式, ':eval' 执行, ':clear' 清空
输入 'setPrintAST(true)' 开启 AST 打印
>>> System.out.println("Hello World!");
Hello World!
>>> exit
Say goodbye~~~
交互式模式已退出

methods> class info com.android.server.am.ActivityManagerService
类名: com.android.server.am.ActivityManagerService
修饰符: public

父类: android.app.IActivityManager$Stub

实现的接口 (2个):
  - com.android.server.Watchdog$Monitor
  - com.android.internal.os.BatteryStatsImpl$BatteryCallback

构造函数 (2个):
  public ActivityManagerService(Context)
  public ActivityManagerService(Injector)

字段 (410个):
  [package-private] final int GL_ES_VERSION
  private int crashCounts
  private boolean fwMonitorSwitch
  private boolean isFirstPerformance
  [package-private] final ArrayList mActiveInstrumentation
...
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

```bash
adb install -r build-release-vx.x.x.apk
```

(2) 在Xposed管理器中激活这个Xposed模块

(3) 安装配套的Magisk模块

- 下载最新release中的magisk-module.zip (如果没的话, 可以在项目里找找)
- 如果你使用的是安卓设备下载, 直接用Magisk管理器安装就好!
- 其他情况, 使用ADB安装:

```bash
adb push magisk-module.zip /data/local/tmp
magisk --install-module /data/local/tmp/magisk-module.zip
```

(4) 如果你不用Magisk管理root (?)

- 把magisk-module.zip中的system文件夹提取出来，将其与设备中的system合并
- 然后，更改一下可执行文件的权限

```bash
chmod 0775 /system/bin/methods                 # Shell客户端文件

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

## 技术栈

### Android

- **Xposed Framework** - 系统Hook框架
- **Magisk** - Root权限管理
- **NDK** - C++原生开发
- **Gradle** - 构建系统

### 后端

- **Java** - Xposed模块和服务端
- **Socket通信** - 客户端和服务端通信
- **Binder IPC** - Android进程间通信

### 前端

- **Java** - Android UI

## 开发指南

### 环境要求

- JDK 8 (运行时) / JDK 17 (编译器) 或更高
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

1. 构建APK

```bash
./gradlew assembleDebug
```

1. 构建Magisk模块

```bash
./gradlew :app:createMagiskZip
```

1. 安装

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

对了，这个MD只在Github上效果好

```text

  输入/输出
┌─────────┐
│  客户端 │
│ (Shell) │
└──────▲──┘     ┌─────────┐
       │        │  app    │
命令行 ├────────> process │ --> 目前主要的瓶颈
       │        └──────▲──┘     破坏输入且开销大
       │               │ 
       │       ┌──────▼──┐ 
       │       │  客户端 │
       │       │  (Java) │
       │       └──────▲──┘ 
       ├───────────────┘
       │ Socket
       │
┌──────▼────────┐
│    服务端     │
│    (Java)     │
│   (Binder)    │
│ (注入android) │
│               │
│  ┌────────┐   │
│  │ Xposed │   │
│  │ Hook   │   │
│  └───▲────┘   │
│      │        │
│   ┌──▼───┐    │
│   │ 系统 │    │
│   │ API  │    │
│   └──────┘    │
└───────────────┘
```

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

本项目采用 [MIT License](LICENSE)（Copyright (c) 2026 JustNothing1021）。

用到的第三方组件及其许可证见 [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md)。

## 致谢

感谢以下项目，库和开发者的支持：

- [Xposed Framework](https://github.com/rovo89/Xposed)
- [Magisk](https://github.com/topjohnwu/Magisk)
- [DeepSeek](https://www.deepseek.com)
- [GLM-4.7](https://www.zhipuai.cn)
- [GLM-5](https://www.zhipuai.cn)
- [NoneColdWind](https://github.com/NoneColdWind)

## 联系方式

- 作者: JustNothing1021
- GitHub: <https://github.com/JustNothing1021>
- Email: <ljy0909112022@163.com>

## 更新日志

### v0.5.0 的 TODO

- 给所有的 CLI 命令都添加 GUI 实现
- 完善权限控制的机制 (包括进程创建之类的)
- 支持一下脚本引擎的嵌套类

### v0.4.9

- 整个项目的大重构, 从命令基类到通信协议都重写了一遍
- 复活了 bytecode 命令, 可以直接反编译源码! (但仅限某些设备)
- 更好的终端的呈现和交互体验!
- 一个新的 jank 命令

### v0.4.8

- 增加了 agent 命令, 可以把服务注入到别的应用里面了, 不仅仅是 system_server
- 部分 CLI 命令的用户界面 (Finally!!!)
- 增加了解析器的 using 语法
- 完善了权限机制的控制

### v0.4.7

- 重构了所有模块的命令行子命令，都添上了颜色
- 增加了一个权限沙盒机制，后续会添加给命令
- 完善了 Cythava++ (?) 的匿名类语法
- 使用能工智人给所有代码 lint 了一遍
- 把一堆 Thread.sleep 换成异步
- 增加了一个神秘的免责声明

### v0.4.6

- 让AI用解释器的语言写了简单的文本程序，可以用script run玩玩
- 给交互式协议添加了颜色的显示
- 给脚本执行器单开了一个项目
- 稍稍改进了一些代码的风格

### v0.4.5

- 给脚本执行器翻新了
- 给所有用签名解析的添加varargs支持
- 给脚本执行器加了一堆乱七八糟的语法（目前还没文档，后面慢慢补吧）
- 稍微优化了一下代码的观感
- 让代码执行器用起来便捷一些

### v0.4.4

- 重构了class命令，现在结构更清晰了
- 让诸如cinvoke/cconstructor等传参依赖调用的命令也能适配可变参数
- 修复了一大堆解释器的问题，添加了方法语法（比如System.out::println）
- 修复了一直以来CmdUtils执行root命令超时的问题
- 修复了工作流不能用的问题（脑溢血方法）
- 让readme在github上面看起来美观一些

### v0.4.3

README.MD!!!!!

修复大量解释器问题

## 常见问题

### Q: 为什么有两个客户端？

A: Shell 处理终端，Java 通过 app_process 执行，体验好。

## 相关链接

- [Xposed Framework文档](https://github.com/rovo89/Xposed/wiki)
- [Magisk文档](https://topjohnwu.github.io/Magisk/)
- [Android开发者文档](https://developer.android.com/)
