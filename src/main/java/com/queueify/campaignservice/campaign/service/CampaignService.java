package com.queueify.campaignservice.campaign.service;

import com.queueify.campaignservice.campaign.dto.CampaignPageResponse;
import com.queueify.campaignservice.campaign.dto.CampaignResponse;
import com.queueify.campaignservice.campaign.dto.CreateCampaignRequest;
import com.queueify.campaignservice.campaign.dto.UpdateCampaignRequest;
import com.queueify.campaignservice.campaign.entity.Campaign;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import com.queueify.campaignservice.campaign.exception.CampaignNotFoundException;
import com.queueify.campaignservice.campaign.exception.InvalidCampaignException;
import com.queueify.campaignservice.campaign.repository.CampaignRepository;
import com.queueify.campaignservice.emailaccount.entity.EmailAccount;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.repository.EmailAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CampaignService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CampaignRepository campaignRepository;
    private final EmailAccountRepository emailAccountRepository;

    public CampaignService(CampaignRepository campaignRepository, EmailAccountRepository emailAccountRepository) {
        this.campaignRepository = campaignRepository;
        this.emailAccountRepository = emailAccountRepository;
    }

    public CampaignResponse createCampaign(Long userId, CreateCampaignRequest request) {
        validateActiveSenderAccount(userId, request.getSenderEmailAccountId());

        LocalDateTime now = LocalDateTime.now();
        Campaign campaign = new Campaign(
                request.getCampaignName(),
                userId,
                request.getSubject(),
                request.getTemplateUrl(),
                request.getCsvUrl(),
                request.getTotalEmails(),
                0,
                0,
                CampaignStatus.DRAFT,
                request.getSenderEmailAccountId(),
                null,
                null,
                now,
                now
        );

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    public CampaignPageResponse listCampaigns(Long userId, CampaignStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, normalizePageSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Campaign> campaignPage = status == null
                ? campaignRepository.findByUserId(userId, pageable)
                : campaignRepository.findByUserIdAndStatus(userId, status, pageable);

        List<CampaignResponse> content = campaignPage.getContent()
                .stream()
                .map(CampaignResponse::from)
                .toList();

        return new CampaignPageResponse(
                content,
                campaignPage.getNumber(),
                campaignPage.getSize(),
                campaignPage.getTotalElements(),
                campaignPage.getTotalPages(),
                campaignPage.isLast()
        );
    }

    public CampaignResponse getCampaign(Long userId, Long campaignId) {
        return CampaignResponse.from(getCampaignEntity(userId, campaignId));
    }

    public CampaignResponse updateCampaign(Long userId, Long campaignId, UpdateCampaignRequest request) {
        Campaign campaign = getCampaignEntity(userId, campaignId);

        validateCampaignCanBeEdited(campaign);
        validateActiveSenderAccount(userId, request.getSenderEmailAccountId());
        validateRequestedStatus(request.getStatus(), request.getPausedReason(), request.getFailureReason());

        campaign.setCampaignName(request.getCampaignName());
        campaign.setSubject(request.getSubject());
        campaign.setTemplateUrl(request.getTemplateUrl());
        campaign.setCsvUrl(request.getCsvUrl());
        campaign.setTotalEmails(request.getTotalEmails());
        campaign.setStatus(request.getStatus());
        campaign.setSenderEmailAccountId(request.getSenderEmailAccountId());
        campaign.setFailureReason(request.getFailureReason());
        campaign.setPausedReason(request.getPausedReason());
        campaign.setUpdatedAt(LocalDateTime.now());

        return CampaignResponse.from(campaignRepository.save(campaign));
    }

    public void deleteCampaign(Long userId, Long campaignId) {
        Campaign campaign = getCampaignEntity(userId, campaignId);

        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            throw new InvalidCampaignException("Running campaigns cannot be deleted.");
        }

        campaignRepository.delete(campaign);
    }

    private Campaign getCampaignEntity(Long userId, Long campaignId) {
        return campaignRepository.findByIdAndUserId(campaignId, userId)
                .orElseThrow(() -> new CampaignNotFoundException("Campaign not found."));
    }

    private void validateActiveSenderAccount(Long userId, Long senderEmailAccountId) {
        EmailAccount senderAccount = emailAccountRepository.findByIdAndUserId(senderEmailAccountId, userId)
                .orElseThrow(() -> new InvalidCampaignException("Sender email account not found for this user."));

        if (senderAccount.getStatus() != EmailAccountStatus.ACTIVE) {
            throw new InvalidCampaignException("Sender email account must be ACTIVE.");
        }
    }

    private void validateCampaignCanBeEdited(Campaign campaign) {
        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            throw new InvalidCampaignException("Running campaigns cannot be updated.");
        }

        if (campaign.getStatus() == CampaignStatus.COMPLETED || campaign.getStatus() == CampaignStatus.COMPLETED_WITH_FAILURES) {
            throw new InvalidCampaignException("Completed campaigns cannot be updated.");
        }
    }

    private void validateRequestedStatus(CampaignStatus status, String pausedReason, String failureReason) {
        if (status == CampaignStatus.RUNNING) {
            throw new InvalidCampaignException("Campaigns cannot be moved to RUNNING from the CRUD API.");
        }

        if (status == CampaignStatus.PAUSED && isBlank(pausedReason)) {
            throw new InvalidCampaignException("Paused campaigns require a pause reason.");
        }

        if (status == CampaignStatus.FAILED && isBlank(failureReason)) {
            throw new InvalidCampaignException("Failed campaigns require a failure reason.");
        }
    }

    private int normalizePageSize(int size) {
        if (size < 1) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
