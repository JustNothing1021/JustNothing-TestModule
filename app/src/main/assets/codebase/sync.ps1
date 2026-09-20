# Codebase 脚本同步工具
# 将脚本推送到设备，无需清空缓存

param(
    [switch]$Push,      # 推送到设备
    [switch]$Pull,      # 从设备拉取
    [switch]$List,      # 列出设备上的脚本
    [string]$Device     # 指定设备ID（多设备时使用）
)

$ErrorActionPreference = "Stop"
$Host.UI.RawUI.WindowTitle = "Codebase 脚本同步工具"

$CODEBASE_DIR = $PSScriptRoot
$DEVICE_DIR = "/data/local/tmp/methods/scripts"

function Write-Header {
    Write-Host ""
    Write-Host "╔════════════════════════════════════════════════════════════╗" -ForegroundColor Cyan
    Write-Host "║            Codebase 脚本同步工具                           ║" -ForegroundColor Cyan
    Write-Host "╚════════════════════════════════════════════════════════════╝" -ForegroundColor Cyan
    Write-Host ""
}

function Test-AdbConnection {
    Write-Host "正在检查 adb 连接..." -ForegroundColor Yellow
    
    $devices = adb devices 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] adb 未安装或不在 PATH 中！" -ForegroundColor Red
        return $false
    }
    
    $deviceList = $devices | Select-String -Pattern "device$" | ForEach-Object { 
        ($_ -split "\s+")[0] 
    }
    
    if ($deviceList.Count -eq 0) {
        Write-Host "[错误] 未检测到设备连接！" -ForegroundColor Red
        Write-Host "请确保:" -ForegroundColor Yellow
        Write-Host "  1. 设备已通过 USB 连接"
        Write-Host "  2. 已开启 USB 调试"
        Write-Host "  3. 已授权此电脑调试"
        return $false
    }
    
    if ($deviceList.Count -gt 1 -and -not $Device) {
        Write-Host "[警告] 检测到多个设备：" -ForegroundColor Yellow
        for ($i = 0; $i -lt $deviceList.Count; $i++) {
            Write-Host "  $($i+1). $($deviceList[$i])"
        }
        Write-Host ""
        Write-Host "请使用 -Device 参数指定设备ID" -ForegroundColor Yellow
        return $false
    }
    
    Write-Host "[OK] 设备已连接" -ForegroundColor Green
    return $true
}

function Invoke-Push {
    Write-Host "源目录: $CODEBASE_DIR" -ForegroundColor Gray
    Write-Host "目标目录: $DEVICE_DIR" -ForegroundColor Gray
    Write-Host ""
    
    Write-Host "正在创建目标目录..." -ForegroundColor Yellow
    adb shell "mkdir -p $DEVICE_DIR" 2>$null | Out-Null
    
    Write-Host ""
    Write-Host "正在同步脚本文件..." -ForegroundColor Yellow
    Write-Host "────────────────────────────────────────────────────────" -ForegroundColor DarkGray
    
    $files = Get-ChildItem -Path $CODEBASE_DIR -File | Where-Object { 
        $_.Name -notlike "*.bat" -and $_.Name -notlike "*.ps1" 
    }
    
    $success = 0
    $failed = 0
    
    foreach ($file in $files) {
        Write-Host "推送: $($file.Name)" -NoNewline
        
        $result = adb push $file.FullName "$DEVICE_DIR/$($file.Name)" 2>&1
        
        if ($LASTEXITCODE -eq 0) {
            Write-Host " [成功]" -ForegroundColor Green
            $success++
        } else {
            Write-Host " [失败]" -ForegroundColor Red
            $failed++
        }
    }
    
    Write-Host "────────────────────────────────────────────────────────" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "同步完成！成功: $success, 失败: $failed" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "现在可以在设备上运行:" -ForegroundColor Yellow
    Write-Host "  script manage"
    Write-Host "  manage> run <脚本名>"
}

function Invoke-Pull {
    Write-Host "从设备拉取脚本到本地..." -ForegroundColor Yellow
    Write-Host "源目录: $DEVICE_DIR" -ForegroundColor Gray
    Write-Host "目标目录: $CODEBASE_DIR" -ForegroundColor Gray
    Write-Host ""
    
    $result = adb shell "ls $DEVICE_DIR" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] 无法访问设备目录" -ForegroundColor Red
        return
    }
    
    $files = $result -split "`n" | Where-Object { $_.Trim() }
    
    Write-Host "────────────────────────────────────────────────────────" -ForegroundColor DarkGray
    
    $success = 0
    foreach ($file in $files) {
        $fileName = $file.Trim()
        if ($fileName) {
            Write-Host "拉取: $fileName" -NoNewline
            adb pull "$DEVICE_DIR/$fileName" "$CODEBASE_DIR/$fileName" 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) {
                Write-Host " [成功]" -ForegroundColor Green
                $success++
            } else {
                Write-Host " [失败]" -ForegroundColor Red
            }
        }
    }
    
    Write-Host "────────────────────────────────────────────────────────" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "拉取完成！共 $success 个文件" -ForegroundColor Cyan
}

function Invoke-List {
    Write-Host "设备上的脚本列表:" -ForegroundColor Yellow
    Write-Host "目录: $DEVICE_DIR" -ForegroundColor Gray
    Write-Host ""
    
    $result = adb shell "ls -la $DEVICE_DIR" 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Host "[错误] 无法访问设备目录" -ForegroundColor Red
        return
    }
    
    Write-Host $result
}

# 主程序
Write-Header

if (-not (Test-AdbConnection)) {
    Read-Host "按回车键退出"
    exit 1
}

if ($List) {
    Invoke-List
} elseif ($Pull) {
    Invoke-Pull
} else {
    # 默认执行推送
    Invoke-Push
}

Write-Host ""
Read-Host "按回车键退出"
