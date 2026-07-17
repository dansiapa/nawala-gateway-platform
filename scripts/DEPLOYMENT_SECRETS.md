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
# │ SERVER_HOST             │ Your server IP (e.g. 3.229.68.221)           │
# │ SERVER_USER             │ SSH username (e.g. ubuntu)                   │
# │ SSH_PRIVATE_KEY         │ Full SSH private key (id_rsa content)        │
# │ DB_PASSWORD             │ MySQL password for nawala user                │
# │ NAWALA_ENCRYPTION_KEY   │ AES-256 key (openssl rand -base64 32)        │
# │ NAWALA_JWT_SECRET       │ JWT signing secret (openssl rand -base64 64) │
# │ NAWALA_PAYLOAD_KEY      │ Payload encryption key (openssl rand -base64 32) │
# │ NAWALA_INTERNAL_SECRET  │ Internal API secret (openssl rand -hex 32)   │
# └─────────────────────────┴──────────────────────────────────────────────┘
#
# ============================================================
# How to Generate SSH Key (if you don't have one):
# ============================================================
#
#   ssh-keygen -t ed25519 -C "nawala-deploy" -f ~/.ssh/nawala_deploy
#
# Then:
#   1. Copy private key content → paste as SSH_PRIVATE_KEY secret
#      cat ~/.ssh/nawala_deploy
#
#   2. Add public key to server authorized_keys:
#      ssh-copy-id -i ~/.ssh/nawala_deploy.pub ubuntu@YOUR_SERVER
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
