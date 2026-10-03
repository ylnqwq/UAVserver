@echo off
title 天枢·低空智能运营管控平台 - 单 Jar 启动 (端口 8082)
setlocal

set "PORT=8082"
set "ROOT=%~dp0"
set "JAR=%ROOT%uavserver-all.jar"
set "LOGDIR=%ROOT%logs"

if not exist "%JAR%" goto nojar
if not exist "%LOGDIR%" md "%LOGDIR%"

echo ==============================================================
echo   天枢·低空智能运营管控平台   单 Jar 启动
echo --------------------------------------------------------------
echo   访问地址 : http://localhost:%PORT%
echo   接口前缀 : http://localhost:%PORT%/api
echo   启动日志 : 直接输出在本窗口
echo   停止服务 : Ctrl + C，或双击 stop-uavserver-8082.bat
echo ==============================================================
echo.

for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":%PORT%" ^| findstr LISTENING') do taskkill /PID %%P /F >nul 2>&1

echo [信息] 正在启动，约 10-20 秒；出现 Started UAVServer 即代表成功。
echo.

java -Dfile.encoding=UTF-8 -jar "%JAR%" --server.port=%PORT%

echo.
echo [信息] 服务已退出。
pause
exit /b 0

:nojar
echo [错误] 找不到 %JAR%
echo.
echo 请先打包（项目根目录执行）：
echo    mvn -f backend\pom.xml clean package -DskipTests
echo    copy backend\target\uavserver-1.0.0.jar uavserver-all.jar
echo.
pause
exit /b 1