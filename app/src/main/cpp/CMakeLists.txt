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
