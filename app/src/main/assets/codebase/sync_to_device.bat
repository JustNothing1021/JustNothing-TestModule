@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo. ========================================================
echo.                   Codebase 脚本同步工具
echo. ========================================================
echo.

set "CODEBASE_DIR=%~dp0"
set "DEVICE_DIR=/data/local/tmp/methods/scripts"

echo 源目录: %CODEBASE_DIR%
echo 目标目录: %DEVICE_DIR%
echo.

echo 正在检查 adb 连接...
adb get-state >nul 2>&1
if errorlevel 1 (
    echo [错误] 未检测到设备连接！
    echo 请确保:
    echo   1. 设备已通过 USB 连接
    echo   2. 已开启 USB 调试
    echo   3. 已授权此电脑调试
    pause
    exit /b 1
)

echo [OK] 设备已连接
echo.

echo 正在创建目标目录...
adb shell "mkdir -p %DEVICE_DIR%"

echo.
echo 正在同步脚本文件...
echo ────────────────────────────────────────────────────────────

set /a count=0
for %%f in (*) do (
    if not "%%f"=="%~nx0" (
        echo 推送: %%f
        adb push "%%f" "%DEVICE_DIR%/%%f" >nul 2>&1
        if errorlevel 1 (
            echo   [失败] %%f
        ) else (
            echo   [成功] %%f
            set /a count+=1
        )
    )
)

echo ────────────────────────────────────────────────────────────
echo.
echo 同步完成！共推送 !count! 个文件
echo.
echo 现在可以在设备上运行:
echo   script manage
echo   manage^> run ^<脚本名^>
echo.
pause
