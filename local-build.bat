@echo off
setlocal

set "JAVA_HOME=D:\java-17-openjdk-17.0.17.0.10-1.win.jdk.x86_64\java-17-openjdk-17.0.17.0.10-1.win.jdk.x86_64"
set "PATH=%JAVA_HOME%\bin;%PATH%"
set "MAVEN_HOME=D:\nawala\.mvn\maven-3.9.6"
set "PATH=%MAVEN_HOME%\bin;%PATH%"

echo ========================================
echo   NAWALA - Local Build
echo ========================================
echo.
echo JAVA_HOME: %JAVA_HOME%
echo.

cd /d D:\nawala

echo [1/2] Building Platform...
call mvn.cmd clean package -DskipTests -pl platform -am
if errorlevel 1 (
    echo ERROR: Platform build failed!
    pause
    exit /b 1
)

echo.
echo [2/2] Building Gateway...
call mvn.cmd clean package -DskipTests -pl gateway -am
if errorlevel 1 (
    echo ERROR: Gateway build failed!
    pause
    exit /b 1
)

echo.
echo ========================================
echo   BUILD SUCCESS!
echo ========================================
echo   Platform JAR: platform\target\nawala-platform-1.0.0.jar
echo   Gateway JAR:  gateway\target\nawala-gateway-1.0.0.jar
echo ========================================
pause
