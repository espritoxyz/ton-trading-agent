-- Add double opt-in columns
ALTER TABLE newsletter_subscription
    ADD COLUMN status                        VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    ADD COLUMN confirmed_at                  TIMESTAMP WITH TIME ZONE,
    ADD COLUMN verification_token            VARCHAR(255) UNIQUE,
    ADD COLUMN verification_token_expires_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN resend_count                  INT NOT NULL DEFAULT 0,
    ADD COLUMN last_resent_at                TIMESTAMP WITH TIME ZONE;

-- Migrate existing data
UPDATE newsletter_subscription SET status = 'ACTIVE'       WHERE active = TRUE;
UPDATE newsletter_subscription SET status = 'UNSUBSCRIBED'  WHERE active = FALSE;

-- Drop old column
ALTER TABLE newsletter_subscription DROP COLUMN active;

-- Indices for new columns
CREATE INDEX idx_newsletter_status             ON newsletter_subscription (status);
CREATE INDEX idx_newsletter_verification_token ON newsletter_subscription (verification_token);
