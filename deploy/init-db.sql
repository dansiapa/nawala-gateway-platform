-- Nawala Gateway Platform Database Initialization
-- This script runs automatically on first Docker startup

-- Extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(500) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(500),
    phone VARCHAR(500),
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    enabled BOOLEAN DEFAULT true,
    can_manage_routes BOOLEAN DEFAULT false,
    can_manage_keys BOOLEAN DEFAULT false,
    can_view_analytics BOOLEAN DEFAULT false,
    can_manage_users BOOLEAN DEFAULT false,
    can_manage_waf BOOLEAN DEFAULT false,
    theme_preference VARCHAR(20) DEFAULT 'system',
    created_at TIMESTAMP DEFAULT NOW(),
    last_login_at TIMESTAMP
);

-- Privileges table
CREATE TABLE IF NOT EXISTS privileges (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

-- User privileges mapping
CREATE TABLE IF NOT EXISTS user_privileges (
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    privilege_id BIGINT REFERENCES privileges(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, privilege_id)
);

-- API Routes
CREATE TABLE IF NOT EXISTS api_routes (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    active BOOLEAN DEFAULT true,
    auth_required BOOLEAN DEFAULT false,
    rate_limit_enabled BOOLEAN DEFAULT false,
    rate_limit_per_minute INT DEFAULT 60,
    timeout_seconds INT DEFAULT 30,
    payload_encryption BOOLEAN DEFAULT false,
    load_balanced BOOLEAN DEFAULT false,
    lb_strategy VARCHAR(20) DEFAULT 'ROUND_ROBIN',
    priority INT DEFAULT 0,
    versioned BOOLEAN DEFAULT false,
    default_version VARCHAR(20),
    request_count BIGINT DEFAULT 0,
    error_count BIGINT DEFAULT 0,
    avg_latency_ms BIGINT DEFAULT 0,
    created_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Route targets for load balancing
CREATE TABLE IF NOT EXISTS route_targets (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT REFERENCES api_routes(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    weight INT DEFAULT 1,
    healthy BOOLEAN DEFAULT true,
    health_check_url VARCHAR(500),
    last_health_check TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- API Keys
CREATE TABLE IF NOT EXISTS api_keys (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    key_prefix VARCHAR(20) NOT NULL,
    key_hash VARCHAR(255) NOT NULL UNIQUE,
    owner_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    active BOOLEAN DEFAULT true,
    revoked BOOLEAN DEFAULT false,
    rate_limit_per_minute INT DEFAULT 60,
    expires_at TIMESTAMP,
    last_used_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW()
);

-- OAuth Clients
CREATE TABLE IF NOT EXISTS oauth2_clients (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    client_id VARCHAR(100) NOT NULL UNIQUE,
    client_secret_hash VARCHAR(255) NOT NULL,
    redirect_uri VARCHAR(500),
    scopes VARCHAR(500) DEFAULT 'read',
    owner_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- OAuth Tokens
CREATE TABLE IF NOT EXISTS oauth2_tokens (
    id BIGSERIAL PRIMARY KEY,
    client_id BIGINT REFERENCES oauth2_clients(id) ON DELETE CASCADE,
    access_token VARCHAR(500) NOT NULL UNIQUE,
    refresh_token VARCHAR(500),
    scopes VARCHAR(500),
    expires_at TIMESTAMP NOT NULL,
    revoked BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

-- WAF Rules
CREATE TABLE IF NOT EXISTS waf_rules (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    rule_type VARCHAR(50) NOT NULL,
    pattern TEXT,
    action VARCHAR(20) DEFAULT 'BLOCK',
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Webhooks
CREATE TABLE IF NOT EXISTS webhooks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    target_url VARCHAR(500) NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    secret VARCHAR(255),
    active BOOLEAN DEFAULT true,
    last_triggered_at TIMESTAMP,
    last_status VARCHAR(20),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Plugins
CREATE TABLE IF NOT EXISTS plugins (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    hook_type VARCHAR(50) NOT NULL,
    script TEXT,
    config TEXT,
    active BOOLEAN DEFAULT true,
    priority INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW()
);

-- API Mocks
CREATE TABLE IF NOT EXISTS api_mocks (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    method VARCHAR(10) NOT NULL,
    path VARCHAR(500) NOT NULL UNIQUE,
    status_code INT DEFAULT 200,
    content_type VARCHAR(100) DEFAULT 'application/json',
    response_body TEXT,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Service Registry
CREATE TABLE IF NOT EXISTS service_registry (
    id BIGSERIAL PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL,
    url VARCHAR(500) NOT NULL,
    metadata TEXT,
    healthy BOOLEAN DEFAULT true,
    last_heartbeat TIMESTAMP DEFAULT NOW(),
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(service_name, url)
);

-- Request Logs
CREATE TABLE IF NOT EXISTS request_logs (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT REFERENCES api_routes(id) ON DELETE SET NULL,
    method VARCHAR(10),
    path VARCHAR(500),
    status_code INT,
    latency_ms BIGINT,
    client_ip VARCHAR(50),
    api_key_id BIGINT REFERENCES api_keys(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

-- System Settings (for setup wizard)
CREATE TABLE IF NOT EXISTS system_settings (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value VARCHAR(2000),
    description VARCHAR(500),
    encrypted BOOLEAN DEFAULT false,
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_api_routes_path ON api_routes(path);
CREATE INDEX IF NOT EXISTS idx_api_routes_active ON api_routes(active);
CREATE INDEX IF NOT EXISTS idx_api_keys_hash ON api_keys(key_hash);
CREATE INDEX IF NOT EXISTS idx_oauth2_tokens_access ON oauth2_tokens(access_token);
CREATE INDEX IF NOT EXISTS idx_request_logs_created ON request_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_request_logs_route ON request_logs(route_id);
CREATE INDEX IF NOT EXISTS idx_service_registry_name ON service_registry(service_name);

-- Insert default privileges
INSERT INTO privileges (name, description) VALUES
    ('ROUTES_VIEW', 'View API routes'),
    ('ROUTES_MANAGE', 'Create, update, delete routes'),
    ('KEYS_VIEW', 'View API keys'),
    ('KEYS_MANAGE', 'Create, revoke API keys'),
    ('USERS_VIEW', 'View users'),
    ('USERS_MANAGE', 'Manage users and roles'),
    ('WAF_VIEW', 'View WAF settings'),
    ('WAF_MANAGE', 'Manage WAF rules'),
    ('ANALYTICS_VIEW', 'View analytics'),
    ('SETTINGS_MANAGE', 'Manage system settings')
ON CONFLICT (name) DO NOTHING;

-- NOTE: No default admin user - will be created via Setup Wizard on first run

-- Insert initial system settings (setup.completed = false triggers Setup Wizard)
INSERT INTO system_settings (setting_key, setting_value) VALUES
    ('setup.completed', 'false'),
    ('platform.name', 'Nawala Gateway'),
    ('platform.version', '1.0.0')
ON CONFLICT (setting_key) DO NOTHING;

-- Additional tables for JPA entities

-- Activity Logs
CREATE TABLE IF NOT EXISTS activity_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    action VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    details TEXT,
    ip_address VARCHAR(50),
    user_agent VARCHAR(500),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Anomaly Events
CREATE TABLE IF NOT EXISTS anomaly_events (
    id BIGSERIAL PRIMARY KEY,
    event_type VARCHAR(50) NOT NULL,
    source_ip VARCHAR(50),
    path VARCHAR(500),
    description TEXT,
    severity VARCHAR(20) DEFAULT 'MEDIUM',
    resolved BOOLEAN DEFAULT false,
    created_at TIMESTAMP DEFAULT NOW()
);

-- API Analytics
CREATE TABLE IF NOT EXISTS api_analytics (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT,
    api_key_prefix VARCHAR(20),
    source_ip VARCHAR(50),
    method VARCHAR(10),
    path VARCHAR(500),
    status_code INT,
    latency_ms BIGINT,
    request_size BIGINT,
    response_size BIGINT,
    recorded_at TIMESTAMP DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_analytics_route_time ON api_analytics(route_id, recorded_at);
CREATE INDEX IF NOT EXISTS idx_analytics_api_key ON api_analytics(api_key_prefix, recorded_at);

-- API Docs
CREATE TABLE IF NOT EXISTS api_docs (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT REFERENCES api_routes(id) ON DELETE CASCADE,
    title VARCHAR(200),
    description TEXT,
    request_example TEXT,
    response_example TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- Audit Log
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT,
    username VARCHAR(50),
    action VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50),
    entity_id BIGINT,
    old_value TEXT,
    new_value TEXT,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT NOW()
);

-- Rate Limit Tiers
CREATE TABLE IF NOT EXISTS rate_limit_tiers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    requests_per_minute INT DEFAULT 60,
    requests_per_hour INT DEFAULT 1000,
    requests_per_day INT DEFAULT 10000,
    burst_size INT DEFAULT 10,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Route Transformations
CREATE TABLE IF NOT EXISTS route_transformations (
    id BIGSERIAL PRIMARY KEY,
    route_id BIGINT REFERENCES api_routes(id) ON DELETE CASCADE,
    transformation_type VARCHAR(50) NOT NULL,
    source_field VARCHAR(200),
    target_field VARCHAR(200),
    transformation_value TEXT,
    apply_on VARCHAR(20) DEFAULT 'REQUEST',
    priority INT DEFAULT 0,
    active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT NOW()
);

-- Webhook Deliveries
CREATE TABLE IF NOT EXISTS webhook_deliveries (
    id BIGSERIAL PRIMARY KEY,
    webhook_id BIGINT REFERENCES webhooks(id) ON DELETE CASCADE,
    event_type VARCHAR(50),
    payload TEXT,
    response_status INT,
    response_body TEXT,
    success BOOLEAN DEFAULT false,
    attempt_count INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW()
);

