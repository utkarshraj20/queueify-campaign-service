package com.queueify.campaignservice.campaign.service;

import com.queueify.campaignservice.campaign.dto.CampaignResponse;
import com.queueify.campaignservice.campaign.dto.CreateCampaignRequest;
import com.queueify.campaignservice.campaign.dto.CreateCampaignWithFilesRequest;
import com.queueify.campaignservice.campaign.exception.InvalidCampaignException;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.repository.EmailAccountRepository;
import com.queueify.campaignservice.storage.CampaignAssetType;
import com.queueify.campaignservice.storage.CampaignAssetUploadService;
import com.queueify.campaignservice.storage.S3StorageService;
import com.queueify.campaignservice.storage.StoredFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CampaignFileCreationService {
    private static final Logger log = LoggerFactory.getLogger(CampaignFileCreationService.class);
    private final EmailAccountRepository emailAccountRepository;
    private final CampaignAssetUploadService uploadService;
    private final S3StorageService storageService;
    private final CampaignService campaignService;
    private final TransactionTemplate transaction;

    public CampaignFileCreationService(EmailAccountRepository emailAccountRepository,
            CampaignAssetUploadService uploadService, S3StorageService storageService,
            CampaignService campaignService, PlatformTransactionManager transactionManager) {
        this.emailAccountRepository = emailAccountRepository;
        this.uploadService = uploadService;
        this.storageService = storageService;
        this.campaignService = campaignService;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    public CampaignResponse create(Long userId, CreateCampaignWithFilesRequest request,
            MultipartFile template, MultipartFile recipients) {
        var sender = emailAccountRepository.findByIdAndUserId(request.getSenderEmailAccountId(), userId)
                .orElseThrow(() -> new InvalidCampaignException("Sender email account not found for this user."));
        if (sender.getStatus() != EmailAccountStatus.ACTIVE) {
            throw new InvalidCampaignException("Sender email account must be ACTIVE.");
        }
        uploadService.validate(CampaignAssetType.TEMPLATE, template);
        uploadService.validate(CampaignAssetType.RECIPIENTS, recipients);

        List<StoredFile> uploaded = new ArrayList<>();
        StoredFile storedTemplate;
        StoredFile storedRecipients;
        try {
            storedTemplate = uploadService.upload(userId, CampaignAssetType.TEMPLATE, template);
            uploaded.add(storedTemplate);
            storedRecipients = uploadService.upload(userId, CampaignAssetType.RECIPIENTS, recipients);
            uploaded.add(storedRecipients);
        } catch (RuntimeException failure) {
            cleanup(uploaded);
            throw failure;
        }

        // Start the database transaction only after remote uploads have finished.
        AtomicBoolean synchronizationRegistered = new AtomicBoolean();
        try {
            return transaction.execute(status -> {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int completionStatus) {
                        if (completionStatus == STATUS_ROLLED_BACK) cleanup(uploaded);
                        else if (completionStatus == STATUS_UNKNOWN) {
                            log.warn("Campaign commit outcome unknown; retaining uploaded assets for reconciliation.");
                        }
                    }
                });
                synchronizationRegistered.set(true);
                var campaign = new CreateCampaignRequest(request.getCampaignName(), request.getSubject(),
                        storedTemplate.getUri(), storedRecipients.getUri(), request.getTotalEmails(),
                        request.getSenderEmailAccountId());
                return campaignService.createCampaign(userId, campaign);
            });
        } catch (RuntimeException failure) {
            if (!synchronizationRegistered.get()) cleanup(uploaded);
            throw failure;
        }
    }

    private void cleanup(List<StoredFile> files) {
        for (StoredFile file : files) {
            try {
                storageService.delete(file.getKey());
            } catch (RuntimeException cleanupFailure) {
                log.warn("Unable to clean up unreferenced campaign asset {}", file.getKey(), cleanupFailure);
            }
        }
    }
}
