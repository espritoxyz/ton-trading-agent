CREATE SEQUENCE newsletter_id_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE newsletter_subscription (
    id                BIGINT PRIMARY KEY DEFAULT nextval('newsletter_id_seq'),
    email             VARCHAR(255) NOT NULL UNIQUE,
    subscribed_at     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    unsubscribed_at   TIMESTAMP WITH TIME ZONE,
    unsubscribe_token VARCHAR(255) NOT NULL UNIQUE,
    active            BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_newsletter_email         ON newsletter_subscription (email);
CREATE INDEX idx_newsletter_unsubscribe   ON newsletter_subscription (unsubscribe_token);
