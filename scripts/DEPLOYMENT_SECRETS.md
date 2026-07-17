# ============================================================
# GitHub Secrets Required for Deployment
# ============================================================
#
# Go to: GitHub Repo → Settings → Secrets and Variables → Actions
# Add these repository secrets:
#
# ┌─────────────────────────┬──────────────────────────────────────────────┐
# │ Secret Name             │ Description                                  │
# ├─────────────────────────┼──────────────────────────────────────────────┤
# │ SERVER_HOST             │ Your server IP (e.g. 103.xx.xx.xx)           │
# │ SERVER_USER             │ SSH username (e.g. root / ubuntu)            │
# │ SERVER_PASSWORD         │ SSH password for the user                    │
# │ DB_PASSWORD             │ MySQL password for nawala user                │
# │ NAWALA_ENCRYPTION_KEY   │ AES-256 key (openssl rand -base64 32)        │
# │ NAWALA_JWT_SECRET       │ JWT signing secret (openssl rand -base64 64) │
# │ NAWALA_PAYLOAD_KEY      │ Payload encryption key (openssl rand -base64 32) │
# │ NAWALA_INTERNAL_SECRET  │ Internal API secret (openssl rand -hex 32)   │
# └─────────────────────────┴──────────────────────────────────────────────┘
#
# ============================================================
# IMPORTANT: No SSH Key needed!
# ============================================================
#
# This workflow uses sshpass (username + password) instead of SSH keys.
# Just set SERVER_HOST, SERVER_USER, and SERVER_PASSWORD.
#
# ============================================================
# How to Deploy:
# ============================================================
#
#   1. Push to deploy/production branch:
#      git checkout -b deploy/production
#      git push origin deploy/production
#
#   2. Or trigger manually:
#      GitHub → Actions → Deploy Nawala → Run workflow
#
# ============================================================
# Branch Protection (deploy/production):
# ============================================================
#
# This branch should be restricted:
#   - GitHub → Settings → Branches → Add rule
#   - Branch name pattern: deploy/production
#   - ☑ Restrict who can push (only you)
#   - ☑ Do not allow deletions
#   - ☑ Lock branch (no direct pushes except admin)
#
# The branch contains NO secrets — all credentials are in
# GitHub Secrets, injected at runtime only.
#
