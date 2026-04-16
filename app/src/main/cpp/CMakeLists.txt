cmake_minimum_required(VERSION 3.4.1)

project(methods_client)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

add_executable(
    methods_client
    methods_client.cpp
)

target_link_libraries(
    methods_client
    log
)

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

project(seccomp_notif)

add_library(
    seccomp_notif
    SHARED
    seccomp_user_notif.cpp
)

target_include_directories(
    seccomp_notif
    PRIVATE
    ${ANDROID_NDK}/sources/android/native_app_glue
)

target_link_libraries(
    seccomp_notif
    log
)
