cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

project(sandbox_test)

add_library(
    sandbox_test
    SHARED
    sandbox_test.cpp
)

target_include_directories(
    sandbox_test
    PRIVATE
    ${ANDROID_NDK}/sources/android/native_app_glue
)

target_link_libraries(
    sandbox_test
    log
)

# seccomp 过滤器：三条规则（进程创建 / 线程创建 / 执行命令）彼此独立，
# 由 Java 侧按 SandboxConfig 逐位传入。
project(sandbox_seccomp)

add_library(
    sandbox_seccomp
    SHARED
    seccomp_jni.cpp
)

target_include_directories(
    sandbox_seccomp
    PRIVATE
    ${ANDROID_NDK}/sources/android/native_app_glue
)

target_link_libraries(
    sandbox_seccomp
    log
)
