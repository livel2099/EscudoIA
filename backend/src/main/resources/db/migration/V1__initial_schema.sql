CREATE TABLE users (
  id UUID PRIMARY KEY,
  email VARCHAR(320) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(32) NOT NULL,
  locale VARCHAR(16) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL,
  updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE roles (id UUID PRIMARY KEY, code VARCHAR(64) NOT NULL UNIQUE);
INSERT INTO roles(id,code) VALUES
 ('10000000-0000-0000-0000-000000000001','ROLE_USER'),
 ('10000000-0000-0000-0000-000000000002','ROLE_ADMIN'),
 ('10000000-0000-0000-0000-000000000003','ROLE_SUPER_ADMIN');
CREATE TABLE user_roles (
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  role_code VARCHAR(64) NOT NULL,
  PRIMARY KEY(user_id, role_code)
);
CREATE TABLE refresh_tokens (
  id UUID PRIMARY KEY, user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  token_hash VARCHAR(64) NOT NULL UNIQUE, expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
  revoked_at TIMESTAMP WITH TIME ZONE, device_info VARCHAR(255)
);
CREATE TABLE plans (
  id UUID PRIMARY KEY, code VARCHAR(64) NOT NULL UNIQUE, name VARCHAR(120) NOT NULL, active BOOLEAN NOT NULL,
  limits_json TEXT NOT NULL, features_json TEXT NOT NULL
);
CREATE TABLE plan_prices (
  id UUID PRIMARY KEY, plan_id UUID NOT NULL REFERENCES plans(id), currency VARCHAR(3) NOT NULL,
  amount DECIMAL(14,2) NOT NULL, active_from TIMESTAMP WITH TIME ZONE NOT NULL, active_to TIMESTAMP WITH TIME ZONE
);
INSERT INTO plans(id,code,name,active,limits_json,features_json) VALUES
 ('20000000-0000-0000-0000-000000000001','FREE','Gratis',true,'{"dailyScans":10}','["Resultado básico","Análisis de texto y URL"]'),
 ('20000000-0000-0000-0000-000000000002','SCAN_PREMIUM','Scan Premium',true,'{"scans":1}','["Informe completo","Detalle de indicadores"]'),
 ('20000000-0000-0000-0000-000000000003','PERSONAL','Personal',true,'{"monthlyScans":200}','["Historial completo","Informes premium","Más análisis"]');
INSERT INTO plan_prices(id,plan_id,currency,amount,active_from) VALUES
 ('21000000-0000-0000-0000-000000000001','20000000-0000-0000-0000-000000000001','ARS',0,'2026-01-01T00:00:00Z'),
 ('21000000-0000-0000-0000-000000000002','20000000-0000-0000-0000-000000000002','ARS',2999,'2026-01-01T00:00:00Z'),
 ('21000000-0000-0000-0000-000000000003','20000000-0000-0000-0000-000000000003','ARS',6999,'2026-01-01T00:00:00Z');
CREATE TABLE scans (
  id UUID PRIMARY KEY, user_id UUID REFERENCES users(id) ON DELETE SET NULL, type VARCHAR(24) NOT NULL,
  status VARCHAR(24) NOT NULL, risk_score INTEGER NOT NULL, risk_level VARCHAR(24) NOT NULL,
  classification VARCHAR(40) NOT NULL, confidence DOUBLE PRECISION NOT NULL, sanitized_text TEXT,
  summary VARCHAR(2000) NOT NULL, recommendation VARCHAR(120) NOT NULL, engine_version VARCHAR(80) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_scans_user_created ON scans(user_id, created_at DESC);
CREATE TABLE scan_inputs (id UUID PRIMARY KEY, scan_id UUID NOT NULL REFERENCES scans(id) ON DELETE CASCADE, sanitized_text TEXT, object_key VARCHAR(500), metadata_json TEXT);
CREATE TABLE scan_results (id UUID PRIMARY KEY, scan_id UUID NOT NULL REFERENCES scans(id) ON DELETE CASCADE, summary TEXT, recommendation VARCHAR(120), engine_version VARCHAR(80), created_at TIMESTAMP WITH TIME ZONE NOT NULL);
CREATE TABLE risk_indicators (
  id UUID PRIMARY KEY, scan_id UUID NOT NULL REFERENCES scans(id) ON DELETE CASCADE, type VARCHAR(80) NOT NULL,
  category VARCHAR(80) NOT NULL, score INTEGER NOT NULL, severity VARCHAR(24) NOT NULL, source VARCHAR(40) NOT NULL,
  confidence DOUBLE PRECISION NOT NULL, explanation VARCHAR(1000) NOT NULL
);
CREATE TABLE risk_config (component_code VARCHAR(80) PRIMARY KEY, weight INTEGER NOT NULL, version VARCHAR(80) NOT NULL);
INSERT INTO risk_config(component_code,weight,version) VALUES
 ('URL_REPUTATION',25,'risk-1.0.0'),('DOMAIN_ANALYSIS',20,'risk-1.0.0'),('BRAND_IMPERSONATION',15,'risk-1.0.0'),
 ('SOCIAL_ENGINEERING',15,'risk-1.0.0'),('AI_CLASSIFIER',15,'risk-1.0.0'),('CONTEXT_BEHAVIOR',10,'risk-1.0.0');
CREATE TABLE payments (
  id UUID PRIMARY KEY, user_id UUID REFERENCES users(id) ON DELETE SET NULL, type VARCHAR(40) NOT NULL,
  amount DECIMAL(14,2) NOT NULL, currency VARCHAR(3) NOT NULL, provider_id VARCHAR(255), status VARCHAR(32) NOT NULL,
  provider_event_id VARCHAR(255) UNIQUE, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE TABLE audit_logs (
  id UUID PRIMARY KEY, actor_id UUID, action VARCHAR(100) NOT NULL, entity_type VARCHAR(80), entity_id UUID,
  ip_hash VARCHAR(64), user_agent_hash VARCHAR(64), request_id VARCHAR(80) NOT NULL, result VARCHAR(32) NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE NOT NULL
);
CREATE INDEX idx_audit_created ON audit_logs(created_at DESC);
