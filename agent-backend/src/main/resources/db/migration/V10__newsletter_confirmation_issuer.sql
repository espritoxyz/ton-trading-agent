-- Track how a newsletter subscription was confirmed.
-- Values mirror the ConfirmationIssuer enum: EMAIL_CONFIRMATION | REGISTRATION_CHECKBOX | ACCOUNT_SETTINGS
ALTER TABLE newsletter_subscription
    ADD COLUMN confirmation_issuer VARCHAR(50);

-- Back-fill: all existing ACTIVE records were confirmed via the email link (the only flow before this change)
UPDATE newsletter_subscription
SET confirmation_issuer = 'EMAIL_CONFIRMATION'
WHERE status = 'ACTIVE';
