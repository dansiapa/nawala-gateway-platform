# ============================================================
# Nawala - Server Setup via SSH
# Usage: powershell -ExecutionPolicy Bypass -File setup-server.ps1
# ============================================================

$SERVER = "3.229.68.221"
$USER = "ubuntu"
$SSH_KEY_PUB = Get-Content "$env:USERPROFILE\.ssh\nawala_deploy.pub"

Write-Host ""
Write-Host "NAWALA - Server Setup" -ForegroundColor Cyan
Write-Host "Server: $USER@$SERVER"
Write-Host "You will be prompted for SSH password." -ForegroundColor Yellow
Write-Host ""

$REMOTE_SCRIPT = @"
#!/bin/bash
set -e
export DEBIAN_FRONTEND=noninteractive

# Add deploy key
mkdir -p ~/.ssh && chmod 700 ~/.ssh
echo "$SSH_KEY_PUB" >> ~/.ssh/authorized_keys
chmod 600 ~/.ssh/authorized_keys
echo "[OK] SSH deploy key added"

# Install dependencies
sudo apt-get update -qq
sudo apt-get install -y -qq openjdk-17-jdk mysql-server redis-server nginx
sudo systemctl enable mysql redis-server nginx
sudo systemctl start mysql redis-server nginx

# Setup MySQL
sudo mysql -e "CREATE DATABASE IF NOT EXISTS nawala_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
sudo mysql -e "CREATE USER IF NOT EXISTS 'nawala'@'localhost' IDENTIFIED BY 'Nawala@DB2026!';"
sudo mysql -e "GRANT ALL PRIVILEGES ON nawala_db.* TO 'nawala'@'localhost';"
sudo mysql -e "FLUSH PRIVILEGES;"
echo "[OK] MySQL configured"

# Directories
sudo mkdir -p /opt/nawala/{logs/platform,logs/gateway,config,backup}
sudo chown -R ubuntu:ubuntu /opt/nawala
"@

$REMOTE_SCRIPT += @"

# Application config
cat > /opt/nawala/config/application.properties << 'EOF'
spring.datasource.url=jdbc:mysql://localhost:3306/nawala_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
spring.datasource.username=nawala
spring.datasource.password=Nawala@DB2026!
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.data.redis.host=localhost
spring.data.redis.port=6379
server.port=8080
server.servlet.session.timeout=30m
EOF
echo "[OK] Application config"

# Nginx
sudo tee /etc/nginx/sites-available/nawala > /dev/null << 'EOF'
upstream platform { server 127.0.0.1:8080; }
upstream gateway { server 127.0.0.1:9090; }
server {
    listen 80;
    server_name _;
    client_max_body_size 50M;
    location / {
        proxy_pass http://platform;
        proxy_set_header Host \`$host;
        proxy_set_header X-Real-IP \`$remote_addr;
        proxy_set_header X-Forwarded-For \`$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \`$scheme;
    }
    location /gw/ {
        rewrite ^/gw/(.*) /\`$1 break;
        proxy_pass http://gateway;
        proxy_set_header Host \`$host;
        proxy_set_header X-Real-IP \`$remote_addr;
        proxy_set_header X-Forwarded-For \`$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \`$scheme;
        proxy_read_timeout 300s;
    }
}
EOF
sudo ln -sf /etc/nginx/sites-available/nawala /etc/nginx/sites-enabled/nawala
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl restart nginx
echo "[OK] Nginx configured"
"@

$REMOTE_SCRIPT += @"

# Systemd services
sudo tee /etc/systemd/system/nawala-platform.service > /dev/null << 'EOF'
[Unit]
Description=Nawala Platform
After=mysql.service redis-server.service
[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/nawala
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/nawala/platform.war --spring.config.additional-location=file:/opt/nawala/config/
Restart=on-failure
RestartSec=10
[Install]
WantedBy=multi-user.target
EOF

sudo tee /etc/systemd/system/nawala-gateway.service > /dev/null << 'EOF'
[Unit]
Description=Nawala Gateway
After=nawala-platform.service
[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/nawala
ExecStart=/usr/bin/java -Xms256m -Xmx512m -jar /opt/nawala/gateway.war --spring.config.additional-location=file:/opt/nawala/config/ --server.port=9090
Restart=on-failure
RestartSec=10
[Install]
WantedBy=multi-user.target
EOF

sudo systemctl daemon-reload
echo ""
echo "SERVER READY!"
echo "Java:  \`$(java -version 2>&1 | head -1)"
echo "MySQL: \`$(sudo systemctl is-active mysql)"
echo "Redis: \`$(sudo systemctl is-active redis-server)"
echo "Nginx: \`$(sudo systemctl is-active nginx)"
echo "Push to deploy/production to deploy!"
"@

# Replace placeholder with actual key
$REMOTE_SCRIPT = $REMOTE_SCRIPT.Replace('$SSH_KEY_PUB', $SSH_KEY_PUB)

# Save script temporarily
$TMP_SCRIPT = "$env:TEMP\nawala_setup.sh"
$REMOTE_SCRIPT | Set-Content -Path $TMP_SCRIPT -Encoding UTF8

Write-Host "Connecting to $USER@$SERVER..." -ForegroundColor Green
Write-Host "Enter password when prompted:" -ForegroundColor Yellow
Write-Host ""

# Copy and execute
scp -o StrictHostKeyChecking=no $TMP_SCRIPT "${USER}@${SERVER}:/tmp/nawala_setup.sh"
ssh -o StrictHostKeyChecking=no "${USER}@${SERVER}" "chmod +x /tmp/nawala_setup.sh && /tmp/nawala_setup.sh"

# Cleanup
Remove-Item $TMP_SCRIPT -Force

Write-Host "" 
Write-Host "DONE! Server ready for deployment." -ForegroundColor Green
Write-Host "Next: git push origin deploy/production" -ForegroundColor Cyan
