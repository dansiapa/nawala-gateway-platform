# ============================================================
# Nawala - Quick Deploy (skip server setup, just build & deploy)
# Usage: powershell -ExecutionPolicy Bypass -File scripts/deploy-quick.ps1
# ============================================================

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
& "$ScriptDir\deploy-full.ps1" -SkipSetup
