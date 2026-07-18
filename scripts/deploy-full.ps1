# ============================================================
# Nawala Gateway Platform - Full Deployment Script
# ============================================================
# Usage: powershell -ExecutionPolicy Bypass -File scripts/deploy-full.ps1
# ============================================================

param(
    [switch]$SkipSetup,
    [switch]$SkipBuild,
    [switch]$SetupOnly
)

$SERVER = "3.229.68.221"
$USER = "ubuntu"
$DEPLOY_PATH = "/opt/nawala"
$PROJECT_DIR = "D:\nawala"
$SSH_KEY = "$env:USERPROFILE\.ssh\nawala_deploy"
$SSH_OPTS = "-o StrictHostKeyChecking=no -o ConnectTimeout=15"
$USE_KEY = $false

if (Test-Path $SSH_KEY) {
    $testResult = ssh $SSH_OPTS -o BatchMode=yes -i $SSH_KEY "${USER}@${SERVER}" "echo OK" 2>$null
    if ($testResult -eq "OK") {
        $USE_KEY = $true
        Write-Host "Using SSH key authentication" -ForegroundColor Green
    }
}
if (-not $USE_KEY) {
    Write-Host "Will use password authentication." -ForegroundColor Yellow
}

function Invoke-SSH { param([string]$Command)
    if ($USE_KEY) { ssh $SSH_OPTS -i $SSH_KEY "${USER}@${SERVER}" $Command }
    else { ssh $SSH_OPTS "${USER}@${SERVER}" $Command }
}
function Invoke-SCP { param([string]$Source, [string]$Dest)
    if ($USE_KEY) { scp $SSH_OPTS -i $SSH_KEY $Source "${USER}@${SERVER}:${Dest}" }
    else { scp $SSH_OPTS $Source "${USER}@${SERVER}:${Dest}" }
}

Write-Host "`n=== NAWALA GATEWAY - Full Deployment ===" -ForegroundColor Cyan
Write-Host " Server: $USER@$SERVER | Deploy: $DEPLOY_PATH`n"

# ============================================================
# STEP 1: Server Setup
# ============================================================
if (-not $SkipSetup) {
    Write-Host "=== STEP 1: Setting up server ===" -ForegroundColor Yellow

    $TMP = "$env:TEMP\nawala_setup.sh"
    Copy-Item "$PROJECT_DIR\scripts\server-setup-remote.sh" $TMP -Force

    Write-Host "Copying setup script..." -ForegroundColor Cyan
    Invoke-SCP -Source $TMP -Dest "/tmp/nawala_setup.sh"

    Write-Host "Running setup on server..." -ForegroundColor Cyan
    Invoke-SSH -Command "chmod +x /tmp/nawala_setup.sh && sudo /tmp/nawala_setup.sh"

    Remove-Item $TMP -Force -ErrorAction SilentlyContinue

    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Server setup failed!" -ForegroundColor Red
        exit 1
    }
    Write-Host "Server setup completed!`n" -ForegroundColor Green
}

if ($SetupOnly) { Write-Host "SetupOnly done."; exit 0 }

# ============================================================
# STEP 2: Build Project
# ============================================================
if (-not $SkipBuild) {
    Write-Host "=== STEP 2: Building project ===" -ForegroundColor Yellow
    Push-Location $PROJECT_DIR
    & "$PROJECT_DIR\mvnw.cmd" clean package -DskipTests -B
    if ($LASTEXITCODE -ne 0) {
        Write-Host "ERROR: Build failed!" -ForegroundColor Red
        Pop-Location; exit 1
    }
    Pop-Location
    Write-Host "Build successful!`n" -ForegroundColor Green
}

# ============================================================
# STEP 3: Deploy to Server
# ============================================================
Write-Host "=== STEP 3: Deploying ===" -ForegroundColor Yellow

$PLATFORM_JAR = Get-ChildItem "$PROJECT_DIR\platform\target\*.jar" -Exclude "*sources*","*javadoc*" | Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1
$GATEWAY_JAR = Get-ChildItem "$PROJECT_DIR\gateway\target\*.jar" -Exclude "*sources*","*javadoc*" | Where-Object { $_.Name -notmatch "original" } | Select-Object -First 1

if (-not $PLATFORM_JAR) { $PLATFORM_JAR = Get-ChildItem "$PROJECT_DIR\platform\target\*.war" | Select-Object -First 1 }
if (-not $GATEWAY_JAR) { $GATEWAY_JAR = Get-ChildItem "$PROJECT_DIR\gateway\target\*.war" | Select-Object -First 1 }

if (-not $PLATFORM_JAR -or -not $GATEWAY_JAR) {
    Write-Host "ERROR: Artifacts not found! Run without -SkipBuild." -ForegroundColor Red; exit 1
}

Write-Host "Platform: $($PLATFORM_JAR.Name)" -ForegroundColor Cyan
Write-Host "Gateway:  $($GATEWAY_JAR.Name)" -ForegroundColor Cyan

Invoke-SSH -Command "sudo systemctl stop nawala-gateway 2>/dev/null; sudo systemctl stop nawala-platform 2>/dev/null; echo 'Stopped'"

Write-Host "Uploading Platform..." -ForegroundColor Cyan
Invoke-SCP -Source $PLATFORM_JAR.FullName -Dest "$DEPLOY_PATH/platform.jar"
Write-Host "Uploading Gateway..." -ForegroundColor Cyan
Invoke-SCP -Source $GATEWAY_JAR.FullName -Dest "$DEPLOY_PATH/gateway.jar"

Write-Host "Starting services..." -ForegroundColor Cyan
Invoke-SSH -Command "sudo systemctl start nawala-platform; sleep 5; sudo systemctl start nawala-gateway; sudo systemctl enable nawala-platform nawala-gateway 2>/dev/null"

# ============================================================
# STEP 4: Health Check
# ============================================================
Write-Host "`n=== STEP 4: Health check (30s wait) ===" -ForegroundColor Yellow
Start-Sleep -Seconds 30

Invoke-SSH -Command "echo 'Platform: '`$(sudo systemctl is-active nawala-platform); echo 'Gateway: '`$(sudo systemctl is-active nawala-gateway); echo 'MySQL: '`$(sudo systemctl is-active mysql); echo 'Redis: '`$(sudo systemctl is-active redis-server); echo 'Nginx: '`$(sudo systemctl is-active nginx); echo ''; curl -sf http://localhost:8080/login > /dev/null && echo 'Platform HTTP: OK' || echo 'Platform HTTP: STARTING'; curl -sf http://localhost:9090/actuator/health > /dev/null && echo 'Gateway HTTP: OK' || echo 'Gateway HTTP: STARTING'"

Write-Host "`n=== DEPLOYMENT COMPLETE ===" -ForegroundColor Green
Write-Host "  http://$SERVER       (Nginx)"
Write-Host "  http://${SERVER}:8080 (Platform)"
Write-Host "  http://${SERVER}:9090 (Gateway)`n"
