-- Create orders and price_tracker tables for tracking user orders and price alerts

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
