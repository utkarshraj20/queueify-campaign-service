package com.queueify.campaignservice.campaign.repository;

import com.queueify.campaignservice.campaign.entity.CampaignOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignOutboxEventRepository
        extends JpaRepository<CampaignOutboxEvent, Long> {
}