CREATE TABLE campaign_outbox_events (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        campaign_id BIGINT NOT NULL,
                                        user_id BIGINT NOT NULL,
                                        event_type VARCHAR(64) NOT NULL,
                                        status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
                                        created_at TIMESTAMP NOT NULL,
                                        published_at TIMESTAMP NULL,

                                        CONSTRAINT uk_campaign_outbox_event
                                            UNIQUE (campaign_id, event_type),

                                        CONSTRAINT chk_campaign_outbox_status
                                            CHECK (status IN ('PENDING', 'PUBLISHED'))
);

CREATE INDEX idx_campaign_outbox_pending
    ON campaign_outbox_events(status, created_at, id);