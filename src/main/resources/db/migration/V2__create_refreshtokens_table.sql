CREATE TABLE refresh_tokens(
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    refresh_token VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL ,
    created_at TIMESTAMP NOT NULL ,
    revoked BOOLEAN NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users(id)

);
