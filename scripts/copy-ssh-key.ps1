# ============================================================
# Nawala - Copy SSH Deploy Key to Server
# Run once: powershell -ExecutionPolicy Bypass -File scripts/copy-ssh-key.ps1
# After this, SSH will work without password (for CI/CD)
# ============================================================

$SERVER = "3.229.68.221"
$USER = "ubuntu"
$KEY_FILE = "$env:USERPROFILE\.ssh\nawala_deploy.pub"

if (-not (Test-Path $KEY_FILE)) {
    Write-Host "ERROR: SSH public key not found at $KEY_FILE" -ForegroundColor Red
    Write-Host "Generate one with: ssh-keygen -t ed25519 -C 'nawala-deploy' -f $env:USERPROFILE\.ssh\nawala_deploy"
    exit 1
}

$PUB_KEY = Get-Content $KEY_FILE -Raw
$PUB_KEY = $PUB_KEY.Trim()

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host " Nawala - Copy SSH Key to Server"
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Server : $USER@$SERVER"
Write-Host "Key    : $KEY_FILE"
Write-Host ""
Write-Host "You will be prompted for SSH password." -ForegroundColor Yellow
Write-Host ""

$CMD = "mkdir -p ~/.ssh && chmod 700 ~/.ssh && echo '$PUB_KEY' >> ~/.ssh/authorized_keys && sort -u ~/.ssh/authorized_keys -o ~/.ssh/authorized_keys && chmod 600 ~/.ssh/authorized_keys && echo 'SSH key added successfully!'"

ssh -o StrictHostKeyChecking=no "${USER}@${SERVER}" $CMD

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "SUCCESS! SSH key copied." -ForegroundColor Green
    Write-Host ""
    Write-Host "Test with:" -ForegroundColor Cyan
    Write-Host "  ssh -i $env:USERPROFILE\.ssh\nawala_deploy $USER@$SERVER 'echo OK'"
    Write-Host ""
} else {
    Write-Host ""
    Write-Host "FAILED to copy SSH key." -ForegroundColor Red
    exit 1
}
