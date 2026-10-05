package com.queueify.campaignservice.campaign.repository;

import com.queueify.campaignservice.campaign.entity.Campaign;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Page<Campaign> findByUserId(Long userId, Pageable pageable);

    Page<Campaign> findByUserIdAndStatus(Long userId, CampaignStatus status, Pageable pageable);

    Optional<Campaign> findByIdAndUserId(Long id, Long userId);
}
