CREATE TABLE email_accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    sender_email VARCHAR(255) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    auth_type VARCHAR(32) NOT NULL,
    encrypted_app_password VARCHAR(512) NOT NULL,
    status VARCHAR(32) NOT NULL,
    metadata JSON,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_email_accounts_user_id
        FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT uk_email_accounts_user_sender_email
        UNIQUE (user_id, sender_email),
    CONSTRAINT chk_email_accounts_provider
        CHECK (provider IN ('GMAIL', 'OUTLOOK', 'SMTP')),
    CONSTRAINT chk_email_accounts_auth_type
        CHECK (auth_type IN ('APP_PASSWORD', 'OAUTH')),
    CONSTRAINT chk_email_accounts_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'INVALID'))
);

CREATE INDEX idx_email_accounts_user_id
    ON email_accounts(user_id);

CREATE INDEX idx_email_accounts_user_status
    ON email_accounts(user_id, status);
