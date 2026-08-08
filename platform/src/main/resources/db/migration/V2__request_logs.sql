-- Request logs for analytics
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

CREATE INDEX IF NOT EXISTS idx_request_logs_route_id ON request_logs(route_id);
CREATE INDEX IF NOT EXISTS idx_request_logs_created_at ON request_logs(created_at);
CREATE INDEX IF NOT EXISTS idx_request_logs_status ON request_logs(status_code);

-- Add columns to api_routes for statistics
ALTER TABLE api_routes ADD COLUMN IF NOT EXISTS request_count BIGINT DEFAULT 0;
ALTER TABLE api_routes ADD COLUMN IF NOT EXISTS error_count BIGINT DEFAULT 0;
ALTER TABLE api_routes ADD COLUMN IF NOT EXISTS avg_latency_ms BIGINT DEFAULT 0;

-- Add last_health_check to route_targets
ALTER TABLE route_targets ADD COLUMN IF NOT EXISTS last_health_check TIMESTAMP;
