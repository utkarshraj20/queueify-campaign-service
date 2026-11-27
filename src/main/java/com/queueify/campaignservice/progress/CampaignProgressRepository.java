package com.queueify.campaignservice.progress;

import java.util.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;

@Repository
@ConditionalOnProperty(name="queueify.progress.enabled",havingValue="true")
public class CampaignProgressRepository {
    private final JdbcTemplate jdbc;
    public CampaignProgressRepository(JdbcTemplate jdbc) {this.jdbc=jdbc;}
    public record Candidate(long campaignId,long eventId) {public String runId(){return "start-"+eventId;}}
    public List<Candidate> candidates(long afterId,int limit) {
        if(afterId<0 || limit<1 || limit>100) throw new IllegalArgumentException("Invalid progress scan bounds.");
        return jdbc.query("""
                SELECT c.id,o.id AS event_id FROM campaigns c JOIN campaign_outbox_events o ON o.campaign_id=c.id AND o.user_id=c.user_id
                WHERE c.status IN ('RUNNING','PAUSED') AND o.event_type='CAMPAIGN_START_REQUESTED' AND c.id>?
                ORDER BY c.id LIMIT ?
                """,(row,n)->new Candidate(row.getLong("id"),row.getLong("event_id")),afterId,limit);
    }
    /** Locks only the projection row, after HTTP; immutable event/run binding and monotonic counters fence stale snapshots. */
    @Transactional(isolation=Isolation.READ_COMMITTED)
    public boolean apply(Candidate candidate,RunProgress progress) {
        if(candidate.campaignId()!=progress.campaignId() || !candidate.runId().equals(progress.runId())) throw new IllegalArgumentException("Progress identity mismatch.");
        var rows=jdbc.query("""
                SELECT c.status,c.total_emails,c.emails_sent,c.emails_failed,c.failure_reason
                FROM campaigns c JOIN campaign_outbox_events o ON o.campaign_id=c.id AND o.user_id=c.user_id
                WHERE c.id=? AND o.id=? AND o.event_type='CAMPAIGN_START_REQUESTED' FOR UPDATE OF c
                """,(row,n)->new Current(row.getString("status"),row.getInt("total_emails"),row.getInt("emails_sent"),row.getInt("emails_failed"),row.getString("failure_reason")),
                candidate.campaignId(),candidate.eventId());
        if(rows.isEmpty()) return false;
        var current=rows.getFirst();
        if(!Set.of("RUNNING","PAUSED").contains(current.status()) || progress.runStatus().equals("PROCESSING")) return false;
        if(progress.sent()<current.sent() || progress.failed()<current.failed()) return false;
        int total=current.total();String state=current.status();String reason=null;
        if(progress.runStatus().equals("READY")) {
            total=progress.validRows().intValue();
            if(progress.sent()+progress.failed()==total) {
                state=progress.failed()>0 ? "COMPLETED_WITH_FAILURES" : "COMPLETED";
                if(progress.failed()>0) reason="One or more messages could not be sent.";
            } else if(progress.unknown()>0) reason="Delivery acceptance is uncertain; manual reconciliation is required.";
        } else {
            state="FAILED";
            reason=progress.runStatus().equals("BLOCKED") ? "Recipient CSV contains invalid rows; no delivery was started." : "Recipient preparation failed; inspect the processing run.";
            if(progress.validRows()!=null) total=(int)(progress.validRows()+progress.invalidRows());
        }
        if(total==current.total() && progress.sent()==current.sent() && progress.failed()==current.failed()
                && state.equals(current.status()) && Objects.equals(reason,current.reason())) return false;
        return jdbc.update("""
                UPDATE campaigns SET total_emails=?,emails_sent=?,emails_failed=?,status=?,failure_reason=?,updated_at=?
                WHERE id=?
                """,total,progress.sent(),progress.failed(),state,reason,java.time.LocalDateTime.now(),candidate.campaignId())==1;
    }
    private record Current(String status,int total,int sent,int failed,String reason) {}
}
