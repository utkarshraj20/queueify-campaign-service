package com.queueify.campaignservice.campaign.repository;

import com.queueify.campaignservice.campaign.entity.Campaign;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, Long> {

    Page<Campaign> findByUserId(Long userId, Pageable pageable);

    Page<Campaign> findByUserIdAndStatus(Long userId, CampaignStatus status, Pageable pageable);

    Optional<Campaign> findByIdAndUserId(Long id, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT c FROM Campaign c
        WHERE c.id = :campaignId AND c.userId = :userId
        """)
    Optional<Campaign> findByIdAndUserIdForUpdate(
            @Param("campaignId") Long campaignId,
            @Param("userId") Long userId
    );
}
