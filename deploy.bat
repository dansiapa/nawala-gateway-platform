@echo off
REM ============================================================
REM Nawala - Build & Deploy Script
REM Jalankan dari: D:\nawala
REM ============================================================

set SERVER=3.229.68.221
set USER=ubuntu
set KEY=D:\Users\Rangga.Putra\.ssh\gacoan-ai.pem
set DEPLOY_PATH=/opt/nawala
set PROJECT_DIR=D:\nawala
set JDK_DIR=D:\java-17-openjdk-17.0.17.0.10-1.win.jdk.x86_64\java-17-openjdk-17.0.17.0.10-1.win.jdk.x86_64

echo.
echo ========================================
echo  NAWALA - Build ^& Deploy
echo  Server: %USER%@%SERVER%
echo ========================================
echo.

set "JAVA_HOME=%JDK_DIR%"
set "PATH=%JAVA_HOME%\bin;%PATH%"
echo Using Java: %JAVA_HOME%
java -version 2>&1 | findstr /i "version"
echo.

REM ============================================================
REM STEP 1: Build Project
REM ============================================================
echo [1/4] Building project...
cd /d %PROJECT_DIR%
call mvnw.cmd clean package -DskipTests -B
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo Build OK!
echo.

REM ============================================================
REM STEP 2: Stop Services di Server
REM ============================================================
echo [2/4] Stopping services on server...
ssh -i "%KEY%" -o StrictHostKeyChecking=no %USER%@%SERVER% "sudo systemctl stop nawala-gateway 2>/dev/null; sudo systemctl stop nawala-platform 2>/dev/null; echo 'Services stopped'"
echo.

REM ============================================================
REM STEP 3: Upload JAR files
REM ============================================================
echo [3/4] Uploading Platform JAR (~53MB)...
scp -i "%KEY%" -o StrictHostKeyChecking=no %PROJECT_DIR%\platform\target\nawala-platform-1.0.0.jar %USER%@%SERVER%:%DEPLOY_PATH%/platform.jar
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Upload platform failed!
    pause
    exit /b 1
)

echo [3/4] Uploading Gateway JAR (~46MB)...
scp -i "%KEY%" -o StrictHostKeyChecking=no %PROJECT_DIR%\gateway\target\nawala-gateway-1.0.0.jar %USER%@%SERVER%:%DEPLOY_PATH%/gateway.jar
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Upload gateway failed!
    pause
    exit /b 1
)
echo Upload OK!
echo.

REM ============================================================
REM STEP 4: Start Services
REM ============================================================
echo [4/4] Starting services...
ssh -i "%KEY%" -o StrictHostKeyChecking=no %USER%@%SERVER% "sudo systemctl start nawala-platform && echo 'Platform started' && sleep 5 && sudo systemctl start nawala-gateway && echo 'Gateway started'"
echo.

REM ============================================================
REM Health Check
REM ============================================================
echo ========================================
echo  Waiting 30s for startup...
echo ========================================
timeout /t 30 /nobreak >nul

ssh -i "%KEY%" -o StrictHostKeyChecking=no %USER%@%SERVER% "echo '--- Service Status ---'; echo 'Platform: '$(sudo systemctl is-active nawala-platform); echo 'Gateway:  '$(sudo systemctl is-active nawala-gateway); echo 'MySQL:    '$(sudo systemctl is-active mysql); echo 'Redis:    '$(sudo systemctl is-active redis-server); echo 'Nginx:    '$(sudo systemctl is-active nginx); echo ''; curl -sf http://localhost:8080/login > /dev/null 2>&1 && echo 'Platform HTTP: OK' || echo 'Platform HTTP: STARTING...'; curl -sf http://localhost:9090/actuator/health > /dev/null 2>&1 && echo 'Gateway HTTP: OK' || echo 'Gateway HTTP: STARTING...'"

echo.
echo ========================================
echo  DEPLOY COMPLETE!
echo  http://%SERVER%       (Nginx)
echo  http://%SERVER%:8080  (Platform)
echo  http://%SERVER%:9090  (Gateway)
echo ========================================
echo.
pause
