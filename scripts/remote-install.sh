#!/bin/bash
# ============================================================
# Nawala Gateway Platform - Remote Server Installer
# ============================================================
# SSH into your server and run this script:
#   curl -sSL https://raw.githubusercontent.com/dansiapa/nawala-gateway-platform/master/scripts/remote-install.sh | sudo bash
# ============================================================

set -e

echo "========================================="
echo " NAWALA GATEWAY - Server Installation"
echo "========================================="

export DEBIAN_FRONTEND=noninteractive
DEPLOY_PATH="/opt/nawala"
DB_NAME="nawala_db"
DB_USER="nawala"
DB_PASS="Nawala@DB2026!"

# --- Update System ---
echo "[1/7] Updating system..."
apt-get update -qq && apt-get upgrade -y -qq

# --- Install Java 17 ---
echo "[2/7] Installing Java 17..."
apt-get install -y -qq openjdk-17-jdk
java -version

# --- Install MySQL 8 ---
echo "[3/7] Installing MySQL 8..."
apt-get install -y -qq mysql-server
systemctl enable mysql && systemctl start mysql

mysql -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';"
mysql -e "GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';"
mysql -e "FLUSH PRIVILEGES;"
echo "   Database: ${DB_NAME} ✓"

# --- Install Redis ---
echo "[4/7] Installing Redis..."
apt-get install -y -qq redis-server
systemctl enable redis-server && systemctl start redis-server
redis-cli ping

# --- Install Nginx ---
echo "[5/7] Installing Nginx..."
apt-get install -y -qq nginx
systemctl enable nginx

cat > /etc/nginx/sites-available/nawala << 'NGINX'
upstream platform {
    server 127.0.0.1:8080;
}
upstream gateway {
    server 127.0.0.1:9090;
}
server {
    listen 80;
    server_name _;
    client_max_body_size 50M;

    location / {
        proxy_pass http://platform;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /gw/ {
        rewrite ^/gw/(.*) /$1 break;
        proxy_pass http://gateway;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
    }
}
NGINX

ln -sf /etc/nginx/sites-available/nawala /etc/nginx/sites-enabled/nawala
rm -f /etc/nginx/sites-enabled/default
nginx -t && systemctl restart nginx

# --- Setup Directories ---
echo "[6/7] Setting up directories..."
mkdir -p ${DEPLOY_PATH}/{logs/platform,logs/gateway,config,backup}

# --- Create application config ---
echo "[7/7] Creating application config..."
cat > ${DEPLOY_PATH}/config/application.properties << EOF
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/${DB_NAME}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# Encryption (will be overridden by env vars from GitHub Actions)
nawala.encryption.key=\${NAWALA_ENCRYPTION_KEY:defaultkey123456789012345678901234}
nawala.gateway.jwt.secret=\${NAWALA_JWT_SECRET:defaultjwtsecret}
nawala.gateway.payload-key=\${NAWALA_PAYLOAD_KEY:defaultpayloadkey}
nawala.gateway.internal-secret=\${NAWALA_INTERNAL_SECRET:defaultinternalsecret}

# Server
server.port=8080
server.servlet.session.timeout=30m
EOF

# --- Create systemd services ---
cat > /etc/systemd/system/nawala-platform.service << EOF
[Unit]
Description=Nawala Platform Console
After=mysql.service redis-server.service
Wants=mysql.service redis-server.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=${DEPLOY_PATH}
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar ${DEPLOY_PATH}/platform.war --spring.config.additional-location=file:${DEPLOY_PATH}/config/
Restart=on-failure
RestartSec=10
StandardOutput=append:${DEPLOY_PATH}/logs/platform/stdout.log
StandardError=append:${DEPLOY_PATH}/logs/platform/stderr.log

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/systemd/system/nawala-gateway.service << EOF
[Unit]
Description=Nawala API Gateway
After=nawala-platform.service
Wants=nawala-platform.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=${DEPLOY_PATH}
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar ${DEPLOY_PATH}/gateway.war --spring.config.additional-location=file:${DEPLOY_PATH}/config/ --server.port=9090
Restart=on-failure
RestartSec=10
StandardOutput=append:${DEPLOY_PATH}/logs/gateway/stdout.log
StandardError=append:${DEPLOY_PATH}/logs/gateway/stderr.log

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
chown -R ubuntu:ubuntu ${DEPLOY_PATH}

echo ""
echo "========================================="
echo " INSTALLATION COMPLETE!"
echo "========================================="
echo ""
echo " Java:    $(java -version 2>&1 | head -1)"
echo " MySQL:   $(systemctl is-active mysql)"
echo " Redis:   $(systemctl is-active redis-server)"
echo " Nginx:   $(systemctl is-active nginx)"
echo ""
echo " Database: ${DB_NAME}"
echo " DB User:  ${DB_USER}"
echo " Config:   ${DEPLOY_PATH}/config/application.properties"
echo ""
echo " Services:"
echo "   sudo systemctl start nawala-platform"
echo "   sudo systemctl start nawala-gateway"
echo ""
echo " URLs (after WAR deploy):"
echo "   Platform: http://$(hostname -I | awk '{print $1}'):8080"
echo "   Gateway:  http://$(hostname -I | awk '{print $1}'):9090"
echo "   Nginx:    http://$(hostname -I | awk '{print $1}'):80"
echo ""
