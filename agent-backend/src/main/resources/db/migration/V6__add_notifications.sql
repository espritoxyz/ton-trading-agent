-- Create notification type enum
CREATE TYPE notification_type AS ENUM (
    'BALANCE_CHANGE',
    'TRANSACTION_COMPLETE',
    'SWAP_EXECUTED',
    'ORDER_FILLED'
);

-- Create notification table
CREATE TABLE notification (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type notification_type NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    metadata JSONB NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    read_at TIMESTAMP,

    -- Foreign key constraint to agent_user
    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES agent_user(id)
        ON DELETE CASCADE
);

-- Composite index for user notifications ordered by creation time (for pagination)
CREATE INDEX idx_notification_user_created
    ON notification (user_id, created_at DESC);

-- Composite index for unread notification queries
CREATE INDEX idx_notification_user_unread
    ON notification (user_id, is_read, created_at DESC);

-- Comment on table
COMMENT ON TABLE notification IS 'User notifications for wallet operations and events';
COMMENT ON COLUMN notification.metadata IS 'Type-specific notification data in JSONB format';
