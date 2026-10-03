@echo off
title 停止 天枢·低空智能运营管控平台 (端口 8082)
setlocal

set "PORT=8082"
set "FOUND=0"

for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":%PORT%" ^| findstr LISTENING') do (
    taskkill /PID %%P /F >nul 2>&1
    set "FOUND=1"
)

if "%FOUND%"=="1" (
    echo [完成] 8082 端口的服务已停止。
) else (
    echo [信息] 8082 端口当前没有进程在监听。
)

echo.
pause