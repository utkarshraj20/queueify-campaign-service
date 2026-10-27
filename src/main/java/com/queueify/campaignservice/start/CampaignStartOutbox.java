package com.queueify.campaignservice.start;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@ConditionalOnProperty(name = "queueify.processor-start.enabled", havingValue = "true")
public class CampaignStartOutbox {
    private final JdbcTemplate jdbc;
    public CampaignStartOutbox(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Optional<Claim> claimOne() {
        var rows = jdbc.query("""
                SELECT STRAIGHT_JOIN e.id, e.campaign_id, e.user_id, e.payload_json, c.csv_url
                FROM campaign_outbox_events e FORCE INDEX (PRIMARY)
                JOIN campaigns c ON c.id=e.campaign_id AND c.user_id=e.user_id
                WHERE e.status='PENDING' AND e.event_type='CAMPAIGN_START_REQUESTED' AND c.status='RUNNING'
                  AND (e.relay_claim_until IS NULL OR e.relay_claim_until <= CURRENT_TIMESTAMP(6))
                  AND (e.next_attempt_at IS NULL OR e.next_attempt_at <= CURRENT_TIMESTAMP(6))
                ORDER BY e.id LIMIT 1 FOR UPDATE OF e SKIP LOCKED
                """, (row, index) -> new Claim(row.getLong("id"), row.getLong("campaign_id"),
                row.getLong("user_id"), row.getString("csv_url"), row.getString("payload_json"), UUID.randomUUID().toString()));
        if (rows.isEmpty()) return Optional.empty();
        var claim = rows.getFirst();
        jdbc.update("""
                UPDATE campaign_outbox_events SET relay_claim_token=?,
                    relay_claim_until=TIMESTAMPADD(SECOND,300,CURRENT_TIMESTAMP(6)),
                    relay_attempt_count=relay_attempt_count+1 WHERE id=?
                """, claim.token(), claim.eventId());
        return Optional.of(claim);
    }

    public boolean savePayload(Claim claim, String json) {
        return jdbc.update("""
                UPDATE campaign_outbox_events SET payload_json=COALESCE(payload_json,CAST(? AS JSON))
                WHERE id=? AND status='PENDING' AND relay_claim_token=?
                """, json, claim.eventId(), claim.token()) == 1;
    }

    public void published(Claim claim) {
        jdbc.update("""
                UPDATE campaign_outbox_events SET status='PUBLISHED', published_at=CURRENT_TIMESTAMP,
                    relay_claim_token=NULL, relay_claim_until=NULL, next_attempt_at=NULL
                WHERE id=? AND status='PENDING' AND relay_claim_token=?
                """, claim.eventId(), claim.token());
    }

    public void failed(Claim claim) {
        jdbc.update("""
                UPDATE campaign_outbox_events SET relay_claim_token=NULL, relay_claim_until=NULL,
                    next_attempt_at=TIMESTAMPADD(SECOND,30,CURRENT_TIMESTAMP(6))
                WHERE id=? AND status='PENDING' AND relay_claim_token=?
                """, claim.eventId(), claim.token());
    }

    public record Claim(long eventId, long campaignId, long userId, String csvUri, String payload, String token) {}
}
