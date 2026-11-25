package com.queueify.campaignservice.internal;

import com.queueify.campaignservice.storage.S3Properties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="queueify.internal.campaign-api.enabled",havingValue="true")
public class CampaignContextRepository {
    private final JdbcTemplate jdbc;
    private final S3Properties storage;
    public CampaignContextRepository(JdbcTemplate jdbc,S3Properties storage) { this.jdbc=jdbc;this.storage=storage; }

    public Optional<Result> find(long campaignId) {
        var rows=jdbc.query("""
                SELECT c.id,c.user_id,c.subject,c.template_url,c.sender_email_account_id,
                       c.status AS campaign_status,a.status AS sender_status,a.user_id AS sender_owner
                FROM campaigns c JOIN email_accounts a ON a.id=c.sender_email_account_id
                WHERE c.id=?
                """,(row,n)->new Result(new CampaignDeliveryContext(row.getLong("id"),row.getLong("user_id"),
                        row.getString("subject"),row.getString("template_url"),row.getLong("sender_email_account_id")),
                        row.getString("campaign_status"),row.getString("sender_status"),row.getLong("sender_owner")),campaignId);
        return rows.stream().findFirst();
    }
    public boolean eligible(Result result) {
        var value=result.context();
        String prefix="s3://"+storage.getBucket()+"/"+
                storage.getBasePrefix().replaceAll("^/+|/+$", "")+"/users/"+value.userId()+"/";
        return "RUNNING".equals(result.campaignStatus()) && "ACTIVE".equals(result.senderStatus())
                && result.senderOwner()==value.userId() && value.subject()!=null && !value.subject().isBlank()
                && value.templateUri()!=null && value.templateUri().startsWith(prefix)
                && value.templateUri().length()>prefix.length();
    }
    public record Result(CampaignDeliveryContext context,String campaignStatus,String senderStatus,long senderOwner) {}
}
