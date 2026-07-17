@echo off
REM ============================================================
REM Nawala Gateway Platform - One-Click Setup Script
REM ============================================================
REM 
REM REQUIREMENTS:
REM   1. Regenerate PAT di: https://github.com/settings/tokens
REM      Scope yang dibutuhkan: repo (full), admin:repo_hook
REM   2. SSH key sudah ada (~/.ssh/id_rsa atau buat baru)
REM
REM USAGE:
REM   setup-all.bat
REM ============================================================

echo.
echo ==========================================
echo   NAWALA GATEWAY - COMPLETE SETUP
echo ==========================================
echo.

REM --- Config (EDIT THESE) ---
set SERVER_HOST=3.229.68.221
set SERVER_USER=ubuntu
set REPO=dansiapa/nawala-gateway-platform
set DB_PASSWORD=Nawala@DB2026!
set GH_CLI=D:\gh\bin\gh.exe

REM --- Generate Keys ---
echo [1/4] Generating encryption keys...
powershell -Command "$enc=[Convert]::ToBase64String((1..32|%%{Get-Random -Max 256})-as[byte[]]); Set-Content env_enc.tmp $enc"
powershell -Command "$jwt=[Convert]::ToBase64String((1..64|%%{Get-Random -Max 256})-as[byte[]]); Set-Content env_jwt.tmp $jwt"
powershell -Command "$pay=[Convert]::ToBase64String((1..32|%%{Get-Random -Max 256})-as[byte[]]); Set-Content env_pay.tmp $pay"
powershell -Command "$int=-join((1..32)|%%{'{0:x2}'-f(Get-Random -Max 256)}); Set-Content env_int.tmp $int"

set /p NAWALA_ENCRYPTION_KEY=<env_enc.tmp
set /p NAWALA_JWT_SECRET=<env_jwt.tmp
set /p NAWALA_PAYLOAD_KEY=<env_pay.tmp
set /p NAWALA_INTERNAL_SECRET=<env_int.tmp
del env_*.tmp

echo    Encryption Key: %NAWALA_ENCRYPTION_KEY%
echo    JWT Secret: %NAWALA_JWT_SECRET:~0,20%...
echo    Payload Key: %NAWALA_PAYLOAD_KEY%
echo    Internal Secret: %NAWALA_INTERNAL_SECRET:~0,20%...

REM --- Set GitHub Secrets ---
echo.
echo [2/4] Setting GitHub Secrets...
%GH_CLI% secret set SERVER_HOST --body "%SERVER_HOST%" --repo %REPO%
%GH_CLI% secret set SERVER_USER --body "%SERVER_USER%" --repo %REPO%
%GH_CLI% secret set DB_PASSWORD --body "%DB_PASSWORD%" --repo %REPO%
%GH_CLI% secret set NAWALA_ENCRYPTION_KEY --body "%NAWALA_ENCRYPTION_KEY%" --repo %REPO%
%GH_CLI% secret set NAWALA_JWT_SECRET --body "%NAWALA_JWT_SECRET%" --repo %REPO%
%GH_CLI% secret set NAWALA_PAYLOAD_KEY --body "%NAWALA_PAYLOAD_KEY%" --repo %REPO%
%GH_CLI% secret set NAWALA_INTERNAL_SECRET --body "%NAWALA_INTERNAL_SECRET%" --repo %REPO%

REM --- SSH Key ---
echo.
echo [3/4] Setting SSH key...
if exist "%USERPROFILE%\.ssh\nawala_deploy" (
    %GH_CLI% secret set SSH_PRIVATE_KEY --body-file "%USERPROFILE%\.ssh\nawala_deploy" --repo %REPO%
    echo    SSH key set from ~/.ssh/nawala_deploy
) else (
    echo    [!] SSH key not found. Generating...
    ssh-keygen -t ed25519 -C "nawala-deploy" -f "%USERPROFILE%\.ssh\nawala_deploy" -N ""
    %GH_CLI% secret set SSH_PRIVATE_KEY --body-file "%USERPROFILE%\.ssh\nawala_deploy" --repo %REPO%
    echo.
    echo    === IMPORTANT ===
    echo    Copy this public key to your server:
    echo.
    type "%USERPROFILE%\.ssh\nawala_deploy.pub"
    echo.
    echo    Run on server: echo "PUBLIC_KEY" ^>^> ~/.ssh/authorized_keys
)

REM --- Create deploy branch ---
echo.
echo [4/4] Creating deploy/production branch...
cd /d D:\nawala
git checkout -b deploy/production 2>nul || git checkout deploy/production
git push origin deploy/production 2>nul

echo.
echo ==========================================
echo   SETUP COMPLETE!
echo ==========================================
echo.
echo   Secrets set on: github.com/%REPO%
echo   Deploy branch:  deploy/production
echo.
echo   NEXT STEPS:
echo   1. Copy SSH public key to server (if new)
echo   2. Push to deploy/production to trigger deployment
echo   3. Or go to GitHub Actions and manually run
echo.
pause
