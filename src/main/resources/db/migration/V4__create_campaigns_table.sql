CREATE TABLE campaigns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_name VARCHAR(255) NOT NULL,
    user_id BIGINT NOT NULL,
    subject VARCHAR(255) NOT NULL,
    template_url VARCHAR(1024) NOT NULL,
    csv_url VARCHAR(1024) NOT NULL,
    total_emails INT NOT NULL,
    emails_sent INT NOT NULL,
    emails_failed INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    sender_email_account_id BIGINT NOT NULL,
    failure_reason VARCHAR(255),
    paused_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_campaigns_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_campaigns_sender_email_account_id
        FOREIGN KEY (sender_email_account_id) REFERENCES email_accounts(id),
    CONSTRAINT chk_campaigns_status
        CHECK (status IN ('DRAFT', 'RUNNING', 'PAUSED', 'COMPLETED', 'COMPLETED_WITH_FAILURES', 'FAILED')),
    CONSTRAINT chk_campaigns_total_emails
        CHECK (total_emails >= 1),
    CONSTRAINT chk_campaigns_email_counters
        CHECK (emails_sent >= 0 AND emails_failed >= 0 AND emails_sent + emails_failed <= total_emails)
);

CREATE INDEX idx_campaigns_user_id
    ON campaigns(user_id);

CREATE INDEX idx_campaigns_user_status
    ON campaigns(user_id, status);

CREATE INDEX idx_campaigns_sender_email_account_id
    ON campaigns(sender_email_account_id);
