package com.queueify.campaignservice.progress;

import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="queueify.progress.enabled",havingValue="true")
public class CampaignProgressSchedule {
    private static final Logger log=LoggerFactory.getLogger(CampaignProgressSchedule.class);
    private final CampaignProgressRepository repository;
    private final ProcessorProgressClient client;
    private long afterId;
    public CampaignProgressSchedule(CampaignProgressRepository repository,ProcessorProgressClient client) {this.repository=repository;this.client=client;}
    @Scheduled(fixedDelayString="${queueify.progress.poll-ms:5000}")
    public synchronized void refresh() {
        var candidates=repository.candidates(afterId,100);
        for(var candidate:candidates) {
            try {client.fetch(candidate).ifPresent(progress->repository.apply(candidate,progress));}
            catch(RuntimeException failure) {log.warn("Campaign progress refresh deferred: campaignId={}",candidate.campaignId());}
            afterId=candidate.campaignId();
        }
        if(candidates.size()<100) afterId=0;
    }
}
