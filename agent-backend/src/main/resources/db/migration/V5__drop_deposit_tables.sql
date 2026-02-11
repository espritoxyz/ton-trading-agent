-- Drop deposit-related tables as they are replaced by burner wallet functionality

-- Drop indexes first
DROP INDEX IF EXISTS idx_deposit_request_code;
DROP INDEX IF EXISTS idx_deposit_request_user_id;
DROP INDEX IF EXISTS idx_deposit_request_status;
DROP INDEX IF EXISTS idx_processed_tx_body_hash;

-- Drop tables (processed_transaction first due to foreign key reference)
DROP TABLE IF EXISTS processed_transaction;
DROP TABLE IF EXISTS deposit_request;
