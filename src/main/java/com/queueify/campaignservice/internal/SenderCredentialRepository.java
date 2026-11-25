package com.queueify.campaignservice.internal;

import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name={"queueify.internal.campaign-api.enabled", "queueify.internal.campaign-api.sender-credentials-enabled"}, havingValue="true")
public class SenderCredentialRepository {
    private final JdbcTemplate jdbc;
    public SenderCredentialRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    public Optional<Sender> find(long campaignId) {
        return jdbc.query("""
                SELECT c.user_id, c.status AS campaign_status, a.id, a.user_id AS sender_owner,
                       a.sender_email, a.provider, a.auth_type, a.status, a.encrypted_app_password
                FROM campaigns c JOIN email_accounts a ON a.id=c.sender_email_account_id WHERE c.id=?
                """, (row, n) -> new Sender(row.getLong("user_id"), row.getString("campaign_status"),
                row.getLong("id"), row.getLong("sender_owner"), row.getString("sender_email"),
                row.getString("provider"), row.getString("auth_type"), row.getString("status"),
                row.getString("encrypted_app_password")), campaignId).stream().findFirst();
    }

    public record Sender(long userId, String campaignStatus, long senderId, long senderOwner,
                         String email, String provider, String authType, String status, String envelope) {
        public boolean eligible() {
            return userId > 0 && userId == senderOwner && "RUNNING".equals(campaignStatus)
                    && "ACTIVE".equals(status) && "GMAIL".equals(provider) && "APP_PASSWORD".equals(authType)
                    && email != null && email.matches("[A-Za-z0-9+_.-]+@gmail\\.com");
        }
        @Override public String toString() { return "Sender[REDACTED]"; }
    }
}
