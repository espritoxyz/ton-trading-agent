CREATE SEQUENCE newsletter_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE newsletter_subscription (
    id                            BIGINT PRIMARY KEY DEFAULT nextval('newsletter_id_seq'),
    email                         VARCHAR(255) NOT NULL UNIQUE,
    status                        VARCHAR(30) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    subscribed_at                 TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    unsubscribed_at               TIMESTAMP WITH TIME ZONE,
    unsubscribe_token             VARCHAR(255) NOT NULL UNIQUE,
    confirmed_at                  TIMESTAMP WITH TIME ZONE,
    confirmation_issuer           VARCHAR(50),
    verification_token            VARCHAR(255) UNIQUE,
    verification_token_expires_at TIMESTAMP WITH TIME ZONE,
    resend_count                  INT NOT NULL DEFAULT 0,
    last_resent_at                TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_newsletter_status ON newsletter_subscription (status);
