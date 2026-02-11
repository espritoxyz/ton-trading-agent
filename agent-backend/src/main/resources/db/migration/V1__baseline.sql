-- Baseline migration for existing database schema
-- This creates all existing tables that were previously managed by Hibernate DDL auto

-- Sequences
CREATE SEQUENCE IF NOT EXISTS user_id_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE IF NOT EXISTS offline_token_id_seq START WITH 1 INCREMENT BY 1;

-- agent_user table
CREATE TABLE IF NOT EXISTS agent_user (
    id BIGINT PRIMARY KEY DEFAULT nextval('user_id_seq'),
    subject VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_login_at TIMESTAMP
);

-- offline_tokens table
CREATE TABLE IF NOT EXISTS offline_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('offline_token_id_seq'),
    user_id BIGINT NOT NULL,
    refresh_token TEXT,
    token_hash TEXT,
    client_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    expires_at TIMESTAMP,
    encryption_key_id VARCHAR(255)
);

-- asset table
CREATE TABLE IF NOT EXISTS asset (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    address VARCHAR(255) NOT NULL,
    amount_nano BIGINT NOT NULL,
    CONSTRAINT uq_asset_user_address UNIQUE (user_id, address)
);

-- balance_transaction table
CREATE TABLE IF NOT EXISTS balance_transaction (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(255) NOT NULL,
    amount_usd_cents BIGINT NOT NULL,
    reference VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- deposit_request table
CREATE TABLE IF NOT EXISTS deposit_request (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    code VARCHAR(6) NOT NULL UNIQUE,
    deposit_wallet_address VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    amount_nano BIGINT,
    asset_type VARCHAR(20),
    jetton_master_address VARCHAR(100),
    jetton_symbol VARCHAR(20),
    jetton_decimals INTEGER,
    transaction_hash VARCHAR(64),
    transaction_lt BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP
);

-- processed_transaction table
CREATE TABLE IF NOT EXISTS processed_transaction (
    id BIGSERIAL PRIMARY KEY,
    body_hash VARCHAR(64) NOT NULL UNIQUE,
    transaction_lt BIGINT NOT NULL,
    transaction_hash VARCHAR(64) NOT NULL,
    deposit_request_id BIGINT,
    processed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for deposit_request
CREATE INDEX IF NOT EXISTS idx_deposit_request_code ON deposit_request(code);
CREATE INDEX IF NOT EXISTS idx_deposit_request_user_id ON deposit_request(user_id);
CREATE INDEX IF NOT EXISTS idx_deposit_request_status ON deposit_request(status);

-- Indexes for processed_transaction
CREATE UNIQUE INDEX IF NOT EXISTS idx_processed_tx_body_hash ON processed_transaction(body_hash);

-- orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    jetton_master VARCHAR(255) NOT NULL,
    action VARCHAR(50) NOT NULL,
    amount DOUBLE PRECISION NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fulfilled BOOLEAN NOT NULL DEFAULT FALSE
);

-- price_tracker table
CREATE TABLE IF NOT EXISTS price_tracker (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    jetton_master VARCHAR(255) NOT NULL,
    target_price DOUBLE PRECISION NOT NULL,
    triggered BOOLEAN NOT NULL DEFAULT FALSE,
    order_id BIGINT,
    direction VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for orders
CREATE INDEX IF NOT EXISTS idx_orders_user_id ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_jetton_master ON orders(jetton_master);
CREATE INDEX IF NOT EXISTS idx_orders_fulfilled ON orders(fulfilled);
CREATE INDEX IF NOT EXISTS idx_orders_user_fulfilled ON orders(user_id, fulfilled);

-- Indexes for price_tracker
CREATE INDEX IF NOT EXISTS idx_price_tracker_user_id ON price_tracker(user_id);
CREATE INDEX IF NOT EXISTS idx_price_tracker_jetton_master ON price_tracker(jetton_master);
CREATE INDEX IF NOT EXISTS idx_price_tracker_triggered ON price_tracker(triggered);
CREATE INDEX IF NOT EXISTS idx_price_tracker_order_id ON price_tracker(order_id);
CREATE INDEX IF NOT EXISTS idx_price_tracker_user_triggered ON price_tracker(user_id, triggered);
