-- Add network fee column to wallet_transaction table
-- fee_nano stores the blockchain network fee in nanotons (nullable - not available for incoming transactions)
ALTER TABLE wallet_transaction ADD COLUMN IF NOT EXISTS fee_nano BIGINT;
