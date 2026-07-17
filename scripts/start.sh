#!/bin/bash
# ============================================================
# Nawala Gateway Platform - Start Services
# ============================================================

set -e

DEPLOY_PATH="/opt/nawala"
LOG_PATH="$DEPLOY_PATH/logs"

echo "Starting Nawala services..."

# --- Start Platform (Management Console) ---
echo "[1/2] Starting Platform (port 8080)..."
nohup java -jar "$DEPLOY_PATH/platform.war" \
    --spring.config.additional-location="file:$DEPLOY_PATH/config/" \
    --logging.file.path="$LOG_PATH/platform" \
    --server.port=8080 \
    > "$LOG_PATH/platform/stdout.log" 2>&1 &

PLATFORM_PID=$!
echo "$PLATFORM_PID" > "$DEPLOY_PATH/platform.pid"
echo "Platform started (PID: $PLATFORM_PID)"

# --- Start Gateway ---
echo "[2/2] Starting Gateway (port 9090)..."
nohup java -jar "$DEPLOY_PATH/gateway.war" \
    --spring.config.additional-location="file:$DEPLOY_PATH/config/" \
    --logging.file.path="$LOG_PATH/gateway" \
    --server.port=9090 \
    > "$LOG_PATH/gateway/stdout.log" 2>&1 &

GATEWAY_PID=$!
echo "$GATEWAY_PID" > "$DEPLOY_PATH/gateway.pid"
echo "Gateway started (PID: $GATEWAY_PID)"

echo ""
echo "========================================="
echo " Nawala is starting up..."
echo " Platform: http://0.0.0.0:8080"
echo " Gateway:  http://0.0.0.0:9090"
echo "========================================="
echo ""
echo "Check logs:"
echo "  tail -f $LOG_PATH/platform/stdout.log"
echo "  tail -f $LOG_PATH/gateway/stdout.log"
