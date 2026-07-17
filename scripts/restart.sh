#!/bin/bash
# ============================================================
# Nawala Gateway Platform - Restart Services
# ============================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Restarting Nawala services..."
"$SCRIPT_DIR/stop.sh"
sleep 3
"$SCRIPT_DIR/start.sh"
