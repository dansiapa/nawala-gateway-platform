#!/bin/bash
# ============================================================
# Nawala Gateway Platform - Stop Services
# ============================================================

DEPLOY_PATH="/opt/nawala"

echo "Stopping Nawala services..."

# --- Stop Platform ---
if [ -f "$DEPLOY_PATH/platform.pid" ]; then
    PID=$(cat "$DEPLOY_PATH/platform.pid")
    if kill -0 "$PID" 2>/dev/null; then
        echo "Stopping Platform (PID: $PID)..."
        kill "$PID"
        sleep 5
        # Force kill if still running
        kill -0 "$PID" 2>/dev/null && kill -9 "$PID"
    fi
    rm -f "$DEPLOY_PATH/platform.pid"
    echo "Platform stopped."
else
    echo "Platform not running (no PID file)."
fi

# --- Stop Gateway ---
if [ -f "$DEPLOY_PATH/gateway.pid" ]; then
    PID=$(cat "$DEPLOY_PATH/gateway.pid")
    if kill -0 "$PID" 2>/dev/null; then
        echo "Stopping Gateway (PID: $PID)..."
        kill "$PID"
        sleep 5
        kill -0 "$PID" 2>/dev/null && kill -9 "$PID"
    fi
    rm -f "$DEPLOY_PATH/gateway.pid"
    echo "Gateway stopped."
else
    echo "Gateway not running (no PID file)."
fi

echo "All services stopped."
