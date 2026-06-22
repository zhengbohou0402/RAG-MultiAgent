CREATE TABLE IF NOT EXISTS tenants (
  id VARCHAR(64) PRIMARY KEY,
  name VARCHAR(128) NOT NULL,
  plan_name VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS users (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  username VARCHAR(64) NOT NULL UNIQUE,
  display_name VARCHAR(128) NOT NULL,
  password_hash VARCHAR(128) NOT NULL,
  role_name VARCHAR(64) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS billing_accounts (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  billing_month VARCHAR(16) NOT NULL,
  total_cost DECIMAL(12,2) NOT NULL,
  unpaid_amount DECIMAL(12,2) NOT NULL,
  currency VARCHAR(12) NOT NULL,
  top_product VARCHAR(128) NOT NULL,
  recommendation VARCHAR(512) NOT NULL
);

CREATE TABLE IF NOT EXISTS invoices (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  invoice_no VARCHAR(64) NOT NULL,
  amount DECIMAL(12,2) NOT NULL,
  status VARCHAR(32) NOT NULL,
  issued_at DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS conversation_cases (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  user_id VARCHAR(64) NOT NULL,
  title VARCHAR(255) NOT NULL,
  route VARCHAR(64),
  status VARCHAR(32) NOT NULL,
  updated_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS saga_compensation_log (
  id VARCHAR(64) PRIMARY KEY,
  tenant_id VARCHAR(64) NOT NULL,
  operation VARCHAR(128) NOT NULL,
  detail TEXT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO tenants(id, name, plan_name)
VALUES ('tenant-demo', 'Demo Enterprise Cloud', 'Enterprise')
ON DUPLICATE KEY UPDATE name = VALUES(name), plan_name = VALUES(plan_name);

INSERT INTO users(id, tenant_id, username, display_name, password_hash, role_name)
VALUES ('demo-admin', 'tenant-demo', 'demo-admin', 'Demo Admin', 'demo123456', 'TENANT_ADMIN')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name), role_name = VALUES(role_name);

INSERT INTO billing_accounts(id, tenant_id, billing_month, total_cost, unpaid_amount, currency, top_product, recommendation)
VALUES ('bill-demo-2026-06', 'tenant-demo', '2026-06', 1286.40, 286.40, 'RM', 'ECS Standard Instance', 'Enable reserved-instance billing for stable workloads and keep auto-scaling for burst traffic.')
ON DUPLICATE KEY UPDATE total_cost = VALUES(total_cost), unpaid_amount = VALUES(unpaid_amount), top_product = VALUES(top_product);

INSERT INTO invoices(id, tenant_id, invoice_no, amount, status, issued_at)
VALUES
  ('inv-demo-001', 'tenant-demo', 'INV-202606-001', 860.00, 'PAID', '2026-06-03'),
  ('inv-demo-002', 'tenant-demo', 'INV-202606-002', 426.40, 'UNPAID', '2026-06-16')
ON DUPLICATE KEY UPDATE amount = VALUES(amount), status = VALUES(status);
