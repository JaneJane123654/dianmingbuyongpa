@echo off
REM ========================================
REM 学生课堂挂机程序 - 打包脚本
REM ========================================

echo [1/3] 清理旧构建...
call mvn clean

echo.
echo [2/3] 编译并打包...
call mvn package -DskipTests

echo.
echo [3/3] 打包完成!
echo.
echo 可执行文件位置:
echo   target\classroom-assistant-3.0.0-all.jar
echo.
echo 运行命令:
echo   java -jar target\classroom-assistant-3.0.0-all.jar
echo.
pause
