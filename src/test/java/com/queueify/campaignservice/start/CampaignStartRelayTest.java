package com.queueify.campaignservice.start;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.queueify.campaignservice.storage.S3Properties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class CampaignStartRelayTest {
    private final CampaignStartOutbox outbox=mock(CampaignStartOutbox.class);
    @SuppressWarnings("unchecked") private final KafkaTemplate<String,String> kafka=mock(KafkaTemplate.class);
    private final ObjectMapper mapper=new ObjectMapper();
    private final S3Properties storage=new S3Properties();
    private CampaignStartRelay relay;
    private final CampaignStartOutbox.Claim claim=new CampaignStartOutbox.Claim(7,42,5,
            "s3://assets/campaign-assets/users/5/uploads/recipients/test.csv", null,"token");

    @BeforeEach void setup() {
        storage.setBucket("assets"); storage.setBasePrefix("campaign-assets");
        relay=new CampaignStartRelay(outbox,kafka,mapper,storage,"campaign-start-topic");
        when(outbox.claimOne()).thenReturn(Optional.of(claim));
        when(outbox.savePayload(any(),anyString())).thenReturn(true);
    }

    @Test void snapshotsAndSendsExactContractBeforeMarkingPublished() throws Exception {
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.completedFuture(null));
        relay.relayOne();
        var json=ArgumentCaptor.forClass(String.class);
        var order=inOrder(outbox,kafka);
        order.verify(outbox).claimOne();
        order.verify(outbox).savePayload(eq(claim),json.capture());
        order.verify(kafka).send("campaign-start-topic","42",json.getValue());
        order.verify(outbox).published(claim);
        assertThat(mapper.readTree(json.getValue())).isEqualTo(mapper.readTree("""
                {"schemaVersion":1,"eventId":7,"campaignId":42,"userId":5,
                 "recipientsBucket":"assets","recipientsKey":"campaign-assets/users/5/uploads/recipients/test.csv"}
                """));
    }

    @Test void failedSendLeavesOutboxPendingForRetry() {
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException()));
        relay.relayOne();
        verify(outbox).failed(claim);
        verify(outbox,never()).published(any());
    }

    @Test @SuppressWarnings("unchecked") void timeoutDoesNotReleaseUncertainClaim() throws Exception {
        CompletableFuture<SendResult<String,String>> pending=mock(CompletableFuture.class);
        when(pending.get(15,TimeUnit.SECONDS)).thenThrow(new TimeoutException());
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(pending);
        relay.relayOne();
        verify(outbox,never()).failed(any());
        verify(outbox,never()).published(any());
    }

    @Test void replayUsesSavedPayloadEvenIfCurrentCampaignMetadataDiffers() {
        var cached=new CampaignStartOutbox.Claim(7,42,5,"changed", "{\"saved\":true}", "token");
        when(outbox.claimOne()).thenReturn(Optional.of(cached));
        when(kafka.send(anyString(),anyString(),anyString())).thenReturn(CompletableFuture.completedFuture(null));
        relay.relayOne();
        verify(kafka).send("campaign-start-topic","42",cached.payload());
    }

    @Test void wrongOwnerSourceDoesNotReachKafka() {
        when(outbox.claimOne()).thenReturn(Optional.of(new CampaignStartOutbox.Claim(7,42,5,
                "s3://assets/campaign-assets/users/6/recipients.csv",null,"token")));
        relay.relayOne();
        verifyNoInteractions(kafka);
        verify(outbox).failed(any());
    }

    @Test void lostClaimDoesNotSend() {
        when(outbox.savePayload(any(),anyString())).thenReturn(false);
        relay.relayOne();
        verifyNoInteractions(kafka);
    }
}
