package com.queueify.campaignservice.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueify.campaignservice.storage.S3Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@ConditionalOnProperty(name = "queueify.processor-start.enabled", havingValue = "true")
public class CampaignStartRelay {
    private static final Logger log = LoggerFactory.getLogger(CampaignStartRelay.class);
    private final CampaignStartOutbox outbox;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper mapper;
    private final S3Properties storage;
    private final String topic;

    public CampaignStartRelay(CampaignStartOutbox outbox, KafkaTemplate<String, String> kafka,
                              ObjectMapper mapper, S3Properties storage,
                              @Value("${queueify.processor-start.topic}") String topic) {
        this.outbox=outbox; this.kafka=kafka; this.mapper=mapper; this.storage=storage; this.topic=topic;
    }

    @Scheduled(fixedDelayString = "${queueify.processor-start.poll-ms:2000}")
    public void relayOne() {
        var next = outbox.claimOne();
        if (next.isEmpty()) return;
        var claim = next.get();
        try {
            String payload = claim.payload();
            if (payload == null) payload = mapper.writeValueAsString(event(claim));
            if (!outbox.savePayload(claim, payload)) return;
            kafka.send(topic, Long.toString(claim.campaignId()), payload).get(15, TimeUnit.SECONDS);
            outbox.published(claim);
        } catch (TimeoutException uncertain) {
            log.warn("Start event {} has an uncertain Kafka acknowledgement; retaining claim", claim.eventId());
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception failure) {
            outbox.failed(claim);
            log.warn("Start event {} will retry; failure type={}", claim.eventId(), failure.getClass().getSimpleName());
        }
    }

    private CampaignStartRequestedEvent event(CampaignStartOutbox.Claim claim) {
        String bucketPrefix = "s3://" + storage.getBucket() + "/";
        if (claim.csvUri() == null || !claim.csvUri().startsWith(bucketPrefix)) {
            throw new IllegalArgumentException("Recipient source must use the configured S3 bucket.");
        }
        String key = claim.csvUri().substring(bucketPrefix.length());
        String ownerPrefix = storage.getBasePrefix().replaceAll("^/+|/+$", "") + "/users/" + claim.userId() + "/";
        if (!key.startsWith(ownerPrefix)) throw new IllegalArgumentException("Recipient source does not belong to campaign owner.");
        return new CampaignStartRequestedEvent(1, claim.eventId(), claim.campaignId(), claim.userId(), storage.getBucket(), key);
    }
}
