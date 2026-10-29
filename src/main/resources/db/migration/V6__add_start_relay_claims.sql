ALTER TABLE campaign_outbox_events
    ADD COLUMN payload_json JSON NULL,
    ADD COLUMN relay_claim_token CHAR(36) NULL,
    ADD COLUMN relay_claim_until TIMESTAMP(6) NULL,
    ADD COLUMN relay_attempt_count INT NOT NULL DEFAULT 0,
    ADD COLUMN next_attempt_at TIMESTAMP(6) NULL;
