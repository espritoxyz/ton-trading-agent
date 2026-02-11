-- Add user wallet support for burner wallets
-- Each user gets their own wallet with encrypted mnemonic

CREATE TABLE IF NOT EXISTS user_wallet (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_address VARCHAR(48) NOT NULL UNIQUE,
    encrypted_mnemonic TEXT NOT NULL,
    encryption_key_id VARCHAR(255) NOT NULL,
    workchain INTEGER NOT NULL DEFAULT 0,
    wallet_version VARCHAR(20) NOT NULL DEFAULT 'V5R1',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    CONSTRAINT fk_user_wallet_user FOREIGN KEY (user_id)
        REFERENCES agent_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_user_wallet_user UNIQUE (user_id)
);

CREATE INDEX idx_user_wallet_user_id ON user_wallet(user_id);
CREATE INDEX idx_user_wallet_address ON user_wallet(wallet_address);
CREATE INDEX idx_user_wallet_active ON user_wallet(is_active) WHERE is_active = true;

-- Transaction history for each wallet
CREATE TABLE IF NOT EXISTS wallet_transaction (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    wallet_address VARCHAR(48) NOT NULL,
    transaction_hash VARCHAR(64) NOT NULL,
    transaction_lt BIGINT NOT NULL,
    direction VARCHAR(10) NOT NULL, -- 'INCOMING' or 'OUTGOING'
    amount_nano BIGINT NOT NULL,
    asset_type VARCHAR(20) NOT NULL, -- 'TON' or 'JETTON'
    jetton_master_address VARCHAR(100),
    jetton_symbol VARCHAR(20),
    jetton_decimals INTEGER,
    sender_address VARCHAR(48),
    recipient_address VARCHAR(48),
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_wallet_tx_user FOREIGN KEY (user_id)
        REFERENCES agent_user(id) ON DELETE CASCADE,
    CONSTRAINT uq_wallet_tx_hash_dir UNIQUE (transaction_hash, direction)
);

CREATE INDEX idx_wallet_tx_user_id ON wallet_transaction(user_id);
CREATE INDEX idx_wallet_tx_wallet_address ON wallet_transaction(wallet_address);
CREATE INDEX idx_wallet_tx_hash ON wallet_transaction(transaction_hash);
CREATE INDEX idx_wallet_tx_created_at ON wallet_transaction(created_at DESC);
