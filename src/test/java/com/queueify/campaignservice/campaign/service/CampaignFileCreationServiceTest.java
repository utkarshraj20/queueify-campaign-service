package com.queueify.campaignservice.campaign.service;

import com.queueify.campaignservice.campaign.dto.CreateCampaignWithFilesRequest;
import com.queueify.campaignservice.campaign.entity.Campaign;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import com.queueify.campaignservice.campaign.exception.InvalidCampaignException;
import com.queueify.campaignservice.campaign.repository.CampaignOutboxEventRepository;
import com.queueify.campaignservice.campaign.repository.CampaignRepository;
import com.queueify.campaignservice.emailaccount.entity.EmailAccount;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.repository.EmailAccountRepository;
import com.queueify.campaignservice.storage.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CampaignFileCreationServiceTest {
    private final EmailAccountRepository senders = mock(EmailAccountRepository.class);
    private final CampaignRepository campaigns = mock(CampaignRepository.class);
    private final S3StorageService storage = mock(S3StorageService.class);
    private final CampaignAssetUploadService uploads = mock(CampaignAssetUploadService.class);
    private final TestTransactionManager transactions = new TestTransactionManager();
    private final CampaignService campaignService = new CampaignService(campaigns, senders,
            mock(CampaignOutboxEventRepository.class));
    private final CampaignFileCreationService service = new CampaignFileCreationService(
            senders, uploads, storage, campaignService, transactions);
    private final MockMultipartFile template = new MockMultipartFile("template", "welcome.html", "text/html", "Hello".getBytes());
    private final MockMultipartFile recipients = new MockMultipartFile("recipients", "leads.csv", "text/csv", "email".getBytes());
    private final StoredFile storedTemplate = new StoredFile("bucket", "template-key", "text/html", 5);
    private final StoredFile storedCsv = new StoredFile("bucket", "csv-key", "text/csv", 5);

    @BeforeEach
    void setUp() {
        EmailAccount sender = new EmailAccount();
        sender.setId(5L);
        sender.setStatus(EmailAccountStatus.ACTIVE);
        when(senders.findByIdAndUserId(5L, 2L)).thenReturn(java.util.Optional.of(sender));
        when(uploads.upload(2L, CampaignAssetType.TEMPLATE, template)).thenReturn(storedTemplate);
        when(uploads.upload(2L, CampaignAssetType.RECIPIENTS, recipients)).thenReturn(storedCsv);
        when(campaigns.save(any(Campaign.class))).thenAnswer(call -> {
            Campaign campaign = call.getArgument(0);
            campaign.setId(10L);
            return campaign;
        });
    }

    @Test
    void uploadsFilesAndPersistsReturnedS3UrisOnDraft() {
        var response = service.create(2L, request(), template, recipients);

        var captor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaigns).save(captor.capture());
        Campaign saved = captor.getValue();
        assertEquals("s3://bucket/csv-key", saved.getCsvUrl());
        assertEquals("s3://bucket/template-key", saved.getTemplateUrl());
        assertEquals(CampaignStatus.DRAFT, saved.getStatus());
        assertEquals(2L, saved.getUserId());
        assertEquals(10L, response.getId());
        assertEquals(saved.getCsvUrl(), response.getCsvUrl());
        assertTrue(transactions.committed);
        verifyNoInteractions(storage);
    }

    @Test
    void validatesSenderBeforeAnyUpload() {
        when(senders.findByIdAndUserId(5L, 2L)).thenReturn(java.util.Optional.empty());
        assertThrows(InvalidCampaignException.class, () -> service.create(2L, request(), template, recipients));
        verifyNoInteractions(uploads, campaigns, storage);
    }

    @Test
    void validatesBothFilesBeforeWritingAnyObject() {
        doThrow(new InvalidUploadException("Invalid CSV")).when(uploads).validate(CampaignAssetType.RECIPIENTS, recipients);
        assertThrows(InvalidUploadException.class, () -> service.create(2L, request(), template, recipients));
        verify(uploads, never()).upload(anyLong(), any(), any());
        verifyNoInteractions(campaigns, storage);
    }

    @Test
    void cleansUpTemplateIfCsvUploadFails() {
        when(uploads.upload(2L, CampaignAssetType.RECIPIENTS, recipients))
                .thenThrow(new StorageException("S3 unavailable", new RuntimeException()));
        assertThrows(StorageException.class, () -> service.create(2L, request(), template, recipients));
        verify(storage).delete("template-key");
        verify(storage, never()).delete("csv-key");
        verifyNoInteractions(campaigns);
    }

    @Test
    void cleansUpBothObjectsAfterDatabaseRollback() {
        RuntimeException failure = new RuntimeException("Insert failed");
        when(campaigns.save(any())).thenThrow(failure);
        assertSame(failure, assertThrows(RuntimeException.class,
                () -> service.create(2L, request(), template, recipients)));
        assertTrue(transactions.rolledBack);
        verify(storage).delete("template-key");
        verify(storage).delete("csv-key");
    }

    @Test
    void cleansUpIfDatabaseTransactionCannotBegin() {
        transactions.failBegin = true;
        assertThrows(TransactionSystemException.class, () -> service.create(2L, request(), template, recipients));
        verify(storage).delete("template-key");
        verify(storage).delete("csv-key");
        verifyNoInteractions(campaigns);
    }

    @Test
    void retainsObjectsWhenCommitOutcomeIsUnknown() {
        transactions.failCommit = true;
        assertThrows(TransactionSystemException.class, () -> service.create(2L, request(), template, recipients));
        verifyNoInteractions(storage);
    }

    @Test
    void cleanupFailureDoesNotHideTheOriginalFailureOrSkipOtherObjects() {
        RuntimeException original = new RuntimeException("Insert failed");
        when(campaigns.save(any())).thenThrow(original);
        doThrow(new RuntimeException("Cleanup unavailable")).when(storage).delete("template-key");
        assertSame(original, assertThrows(RuntimeException.class,
                () -> service.create(2L, request(), template, recipients)));
        verify(storage).delete("csv-key");
    }

    private CreateCampaignWithFilesRequest request() {
        var request = new CreateCampaignWithFilesRequest();
        request.setCampaignName("Launch");
        request.setSubject("Hello");
        request.setTotalEmails(1);
        request.setSenderEmailAccountId(5L);
        return request;
    }

    private static class TestTransactionManager extends AbstractPlatformTransactionManager {
        boolean committed;
        boolean rolledBack;
        boolean failBegin;
        boolean failCommit;

        @Override
        protected Object doGetTransaction() { return new Object(); }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            if (failBegin) throw new TransactionSystemException("Database unavailable");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            if (failCommit) throw new TransactionSystemException("Commit outcome unknown");
            committed = true;
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) { rolledBack = true; }
    }
}
