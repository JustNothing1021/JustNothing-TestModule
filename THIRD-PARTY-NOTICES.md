# 第三方组件与许可证

本项目自身代码采用 [MIT License](LICENSE)。本文件列出所用第三方组件及其许可证。

> 许可证按各上游项目自己的声明整理，能直接从本地产物（jar 内的 `LICENSE`、Gradle 缓存里的 POM）读到的都实测过，见文末「怎么核的」。
> 版权归各自作者所有，许可证原文以各自项目仓库为准。

## 随 APK 分发

### Apache-2.0

许可证副本见 [licenses/Apache-2.0.txt](licenses/Apache-2.0.txt)。

- `androidx.appcompat:appcompat` 1.7.1
- `androidx.constraintlayout:constraintlayout` 2.2.1
- `com.google.android.material:material` 1.13.0
- `com.google.code.gson:gson` 2.14.0
- `com.caverock:androidsvg-aar` 1.4
- `com.github.PhilJay:MPAndroidChart` v3.1.0
- `de.femtopedia.dex2jar:dex-translator` 2.4.38
- `org.apache-extras.beanshell:bsh` 2.0b6
- `net.java.dev.jna:jna` 5.18.1 —— 双许可（`Apache-2.0 OR LGPL-2.1-or-later`），本项目选 Apache-2.0

### BSD-3-Clause

- `org.ow2.asm:asm` 9.9.1
- `com.android.tools:r8` 8.5.35
- `org.jline:jline` 3.30.13

### BSD-2-Clause

- `org.commonmark:commonmark` 0.24.0
- `org.commonmark:commonmark-ext-gfm-tables` 0.24.0
- `org.commonmark:commonmark-ext-gfm-strikethrough` 0.24.0

### Mulan PSL v2

- `cn.hutool:hutool-all` 5.8.44

### MIT

- `org.benf:cfr` 0.152

## 仓库内自带的 jar（`app/libs/`）

| 文件 | 内容 | 许可证 |
| --- | --- | --- |
| `engine_new.jar` | `com.justnothing.engine`，本项目的脚本引擎 | MIT（作者自有代码） |
| `RichConsole.jar` | `com.justnothing.richconsole`，本项目的控制台渲染库 | MIT（作者自有代码） |
| `NCW-Logger-1.0.3-hotfix3.jar` | `cn.ncw.logger`，朋友的作品 | **无许可证**，见下 |

## 仓库内自带的二进制与内置源码

| 路径 | 说明 | 许可证 |
| --- | --- | --- |
| `third_party/vdexextractor/` | dex/vdex 提取工具，编译后作为 assets 打进 APK | Apache-2.0 |
| `app/src/main/jniLibs/*/libjlinenative.so` | JLine3 原生库的编译产物（源码是作者本地保留的 JLine3 副本，做过修改） | BSD-3-Clause |
| `app/src/main/jniLibs/*/libsandbox_test.so`、`libsandbox_seccomp.so` | 本项目自有 C++（`app/src/main/cpp/`） | MIT（同本项目） |

## 仅编译期使用，不随 APK 分发

- `de.robv.android.xposed:api` 82 —— Apache-2.0

## 仅测试使用

- `junit:junit` 4.13.2 —— EPL-1.0
- `androidx.test.ext:junit` 1.3.0 —— Apache-2.0
- `androidx.test.espresso:espresso-core` 3.7.0 —— Apache-2.0

## 补充

`cn.ncw.logger` 没附任何许可证。默认情况下他人无权复制、修改、分发。作者本人已经明确同意本项目使用。
