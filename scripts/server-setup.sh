#!/bin/bash
# ============================================================
# Nawala Gateway Platform - Server Setup Script
# Run once on fresh server (idempotent - safe to re-run)
# ============================================================

set -e

echo "========================================="
echo " Nawala Server Setup"
echo "========================================="

export DEBIAN_FRONTEND=noninteractive

# --- System Update ---
echo "[1/6] Updating system packages..."
sudo apt-get update -qq
sudo apt-get upgrade -y -qq

# --- Java 17 ---
echo "[2/6] Installing Java 17..."
if ! java -version 2>&1 | grep -q "17"; then
    sudo apt-get install -y -qq openjdk-17-jdk
fi
echo "Java: $(java -version 2>&1 | head -1)"

# --- MySQL 8.0 ---
echo "[3/6] Installing MySQL 8.0..."
if ! command -v mysql &> /dev/null; then
    sudo apt-get install -y -qq mysql-server
    sudo systemctl enable mysql
    sudo systemctl start mysql
    
    # Secure MySQL & create nawala database
    sudo mysql -e "CREATE DATABASE IF NOT EXISTS nawala_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
    sudo mysql -e "CREATE USER IF NOT EXISTS 'nawala'@'localhost' IDENTIFIED BY '$(cat /opt/nawala/.db_password 2>/dev/null || echo "CHANGE_ME")';"
    sudo mysql -e "GRANT ALL PRIVILEGES ON nawala_db.* TO 'nawala'@'localhost';"
    sudo mysql -e "FLUSH PRIVILEGES;"
fi
echo "MySQL: $(mysql --version)"

# --- Redis 7 ---
echo "[4/6] Installing Redis..."
if ! command -v redis-server &> /dev/null; then
    sudo apt-get install -y -qq redis-server
    sudo systemctl enable redis-server
    sudo systemctl start redis-server
fi
echo "Redis: $(redis-server --version)"

# --- Nginx (Reverse Proxy) ---
echo "[5/6] Installing Nginx..."
if ! command -v nginx &> /dev/null; then
    sudo apt-get install -y -qq nginx
    sudo systemctl enable nginx
fi

# Configure Nginx
sudo tee /etc/nginx/sites-available/nawala > /dev/null << 'NGINX'
upstream platform {
    server 127.0.0.1:8080;
}

upstream gateway {
    server 127.0.0.1:9090;
}

server {
    listen 80;
    server_name _;

    # Platform Console
    location / {
        proxy_pass http://platform;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Gateway API
    location /gw/ {
        rewrite ^/gw/(.*) /$1 break;
        proxy_pass http://gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_connect_timeout 75s;
    }
}
NGINX

sudo ln -sf /etc/nginx/sites-available/nawala /etc/nginx/sites-enabled/nawala
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl restart nginx

# --- Directory Structure ---
echo "[6/6] Setting up directories..."
sudo mkdir -p /opt/nawala/{logs/platform,logs/gateway,config,backup}
sudo chown -R $USER:$USER /opt/nawala

echo ""
echo "========================================="
echo " Server setup complete!"
echo "========================================="
echo ""
echo " Java 17:   $(java -version 2>&1 | head -1)"
echo " MySQL:     $(sudo systemctl is-active mysql)"
echo " Redis:     $(sudo systemctl is-active redis-server)"
echo " Nginx:     $(sudo systemctl is-active nginx)"
echo ""
