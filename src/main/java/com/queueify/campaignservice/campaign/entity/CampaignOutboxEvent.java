package com.queueify.campaignservice.campaign.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Table(
        name = "campaign_outbox_events",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_campaign_outbox_event",
                columnNames = {"campaign_id", "event_type"}
        ),
        indexes = @Index(
                name = "idx_campaign_outbox_pending",
                columnList = "status,created_at,id"
        )
)
public class CampaignOutboxEvent {

    public enum EventType {
        CAMPAIGN_START_REQUESTED
    }

    public enum Status {
        PENDING,
        PUBLISHED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "campaign_id", nullable = false)
    private Long campaignId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private EventType eventType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static CampaignOutboxEvent startRequested(
            Long campaignId, Long userId, LocalDateTime now) {
        CampaignOutboxEvent event = new CampaignOutboxEvent();
        event.campaignId = campaignId;
        event.userId = userId;
        event.eventType = EventType.CAMPAIGN_START_REQUESTED;
        event.status = Status.PENDING;
        event.createdAt = now;
        return event;
    }
}