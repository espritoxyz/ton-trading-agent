-- Add email verification fields to agent_user table
ALTER TABLE agent_user
    ADD COLUMN IF NOT EXISTS email_verified BOOLEAN DEFAULT FALSE NOT NULL,
    ADD COLUMN IF NOT EXISTS email_verification_sent_at TIMESTAMP;

-- Create sequence for email_verification_tokens
CREATE SEQUENCE IF NOT EXISTS email_verification_token_id_seq START WITH 1 INCREMENT BY 1;

-- Create email_verification_tokens table
CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id BIGINT PRIMARY KEY DEFAULT nextval('email_verification_token_id_seq'),
    user_id BIGINT NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    attempts INTEGER DEFAULT 0,
    last_resent_at TIMESTAMP,
    resend_count INTEGER DEFAULT 0,
    CONSTRAINT fk_email_verification_user FOREIGN KEY (user_id) REFERENCES agent_user(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_email_verification_token ON email_verification_tokens(token);
CREATE INDEX IF NOT EXISTS idx_email_verification_expires_at ON email_verification_tokens(expires_at);
CREATE INDEX IF NOT EXISTS idx_email_verification_user_id ON email_verification_tokens(user_id);

-- Unique constraint: one unverified token per user
-- Note: Expired tokens are cleaned up by scheduled task
CREATE UNIQUE INDEX IF NOT EXISTS idx_active_verification_token
    ON email_verification_tokens(user_id)
    WHERE verified_at IS NULL;
