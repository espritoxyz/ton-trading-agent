-- Drop deposit-related tables as they are replaced by burner wallet functionality

-- Drop tables (processed_transaction first due to foreign key reference)
DROP TABLE IF EXISTS processed_transaction;
DROP TABLE IF EXISTS deposit_request;
