#!/bin/bash
# ============================================================
# Nawala Gateway Platform - Remote Server Setup (all-in-one)
# Run on server: sudo bash server-setup-remote.sh
# ============================================================

set -e
export DEBIAN_FRONTEND=noninteractive

DEPLOY_PATH="/opt/nawala"
DB_NAME="nawala_db"
DB_USER="nawala"
DB_PASS="Nawala@DB2026!"

echo "========================================="
echo " NAWALA - Server Setup"
echo "========================================="

# --- System Update ---
echo "[1/7] Updating system..."
apt-get update -qq
apt-get upgrade -y -qq

# --- Java 17 ---
echo "[2/7] Installing Java 17..."
apt-get install -y -qq openjdk-17-jdk curl
echo "Java: $(java -version 2>&1 | head -1)"

# --- MySQL 8 ---
echo "[3/7] Installing MySQL 8..."
apt-get install -y -qq mysql-server
systemctl enable mysql && systemctl start mysql

mysql -e "CREATE DATABASE IF NOT EXISTS ${DB_NAME} CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" 2>/dev/null || true
mysql -e "CREATE USER IF NOT EXISTS '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';" 2>/dev/null || true
mysql -e "ALTER USER '${DB_USER}'@'localhost' IDENTIFIED BY '${DB_PASS}';" 2>/dev/null || true
mysql -e "GRANT ALL PRIVILEGES ON ${DB_NAME}.* TO '${DB_USER}'@'localhost';" 2>/dev/null || true
mysql -e "FLUSH PRIVILEGES;" 2>/dev/null || true
echo "MySQL: $(systemctl is-active mysql)"

# --- Redis ---
echo "[4/7] Installing Redis..."
apt-get install -y -qq redis-server
systemctl enable redis-server && systemctl start redis-server
echo "Redis: $(systemctl is-active redis-server)"

# --- Nginx ---
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
echo "Nginx: $(systemctl is-active nginx)"

# --- Directories ---
echo "[6/7] Setting up directories..."
mkdir -p ${DEPLOY_PATH}/{logs/platform,logs/gateway,config,backup}

# --- Application Config ---
echo "[7/7] Creating application config..."
cat > ${DEPLOY_PATH}/config/application.properties << EOF
# Nawala Production Config
spring.datasource.url=jdbc:mysql://localhost:3306/${DB_NAME}?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect

spring.data.redis.host=localhost
spring.data.redis.port=6379

nawala.encryption.key=ieUfr//mm5I3ZRW39jagiPW9KG8SOiPgvQZULD00LW4=
nawala.gateway.jwt.secret=TmF3YWxhR2F0ZXdheVNlY3JldEtleUZvckpXVFRva2VuU2lnbjEyMzQ=
nawala.gateway.payload-key=ieUfr//mm5I3ZRW39jagiPW9KG8SOiPgvQZULD00LW4=
nawala.gateway.internal-secret=NawalaInternalSecretKey2024!

server.port=8080
server.servlet.session.timeout=30m
server.servlet.session.cookie.http-only=true
EOF

# --- Systemd Services ---
cat > /etc/systemd/system/nawala-platform.service << 'EOF'
[Unit]
Description=Nawala Platform Console
After=mysql.service redis-server.service
Wants=mysql.service redis-server.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/nawala
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/nawala/platform.jar --spring.config.additional-location=file:/opt/nawala/config/ --server.port=8080
Restart=on-failure
RestartSec=10
StandardOutput=append:/opt/nawala/logs/platform/stdout.log
StandardError=append:/opt/nawala/logs/platform/stderr.log

[Install]
WantedBy=multi-user.target
EOF

cat > /etc/systemd/system/nawala-gateway.service << 'EOF'
[Unit]
Description=Nawala API Gateway
After=nawala-platform.service
Wants=nawala-platform.service

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/nawala
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/nawala/gateway.jar --spring.config.additional-location=file:/opt/nawala/config/ --server.port=9090
Restart=on-failure
RestartSec=10
StandardOutput=append:/opt/nawala/logs/gateway/stdout.log
StandardError=append:/opt/nawala/logs/gateway/stderr.log

[Install]
WantedBy=multi-user.target
EOF

systemctl daemon-reload
chown -R ubuntu:ubuntu ${DEPLOY_PATH}

echo ""
echo "========================================="
echo " SERVER SETUP COMPLETE!"
echo "========================================="
echo ""
echo " Java:  $(java -version 2>&1 | head -1)"
echo " MySQL: $(systemctl is-active mysql)"
echo " Redis: $(systemctl is-active redis-server)"
echo " Nginx: $(systemctl is-active nginx)"
echo ""
echo " Database: ${DB_NAME} (user: ${DB_USER})"
echo " Config:   ${DEPLOY_PATH}/config/application.properties"
echo ""
echo " Services ready:"
echo "   sudo systemctl start nawala-platform"
echo "   sudo systemctl start nawala-gateway"
echo ""

