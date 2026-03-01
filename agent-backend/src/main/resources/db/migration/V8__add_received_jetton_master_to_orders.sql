-- Add received_jetton_master column to orders to store the asset received when an order executes

ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS received_jetton_master VARCHAR(255);

-- Backfill existing rows: default to TON jetton master configured in application.yaml
-- NOTE: this is hardcoded based on current application.yaml addressbook.ton
UPDATE orders
SET received_jetton_master = 'EQAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAM9c'
WHERE received_jetton_master IS NULL;

-- Make the column non-nullable now that it is populated
ALTER TABLE orders
    ALTER COLUMN received_jetton_master SET NOT NULL;

-- Index to speed up queries by received_jetton_master if needed
CREATE INDEX IF NOT EXISTS idx_orders_received_jetton_master
    ON orders(received_jetton_master);
