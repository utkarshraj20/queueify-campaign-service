package com.queueify.campaignservice.campaign.service;

import com.queueify.campaignservice.campaign.dto.CampaignPageResponse;
import com.queueify.campaignservice.campaign.dto.CampaignResponse;
import com.queueify.campaignservice.campaign.dto.CreateCampaignRequest;
import com.queueify.campaignservice.campaign.dto.UpdateCampaignRequest;
import com.queueify.campaignservice.campaign.entity.Campaign;
import com.queueify.campaignservice.campaign.entity.CampaignOutboxEvent;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import com.queueify.campaignservice.campaign.exception.CampaignNotFoundException;
import com.queueify.campaignservice.campaign.exception.InvalidCampaignException;
import com.queueify.campaignservice.campaign.repository.CampaignRepository;
import com.queueify.campaignservice.campaign.repository.CampaignOutboxEventRepository;
import com.queueify.campaignservice.emailaccount.entity.EmailAccount;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.entity.EmailAuthType;
import com.queueify.campaignservice.emailaccount.entity.EmailProvider;
import com.queueify.campaignservice.emailaccount.repository.EmailAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CampaignServiceTest {

    @Mock
    private CampaignRepository campaignRepository;

    @Mock
    private EmailAccountRepository emailAccountRepository;

    @Mock
    private CampaignOutboxEventRepository campaignOutboxEventRepository;

    @InjectMocks
    private CampaignService campaignService;

    @Test
    void shouldCreateCampaign() {
        CreateCampaignRequest request = createRequest();

        when(emailAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(emailAccount(EmailAccountStatus.ACTIVE)));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> {
            Campaign campaign = invocation.getArgument(0);
            campaign.setId(10L);
            return campaign;
        });

        CampaignResponse response = campaignService.createCampaign(1L, request);

        assertEquals(10L, response.getId());
        assertEquals("October launch", response.getCampaignName());
        assertEquals(1L, response.getUserId());
        assertEquals(CampaignStatus.DRAFT, response.getStatus());
        assertEquals(0, response.getEmailsSent());
        assertEquals(0, response.getEmailsFailed());

        ArgumentCaptor<Campaign> campaignCaptor = ArgumentCaptor.forClass(Campaign.class);
        verify(campaignRepository).save(campaignCaptor.capture());
        assertNotNull(campaignCaptor.getValue().getCreatedAt());
        assertNotNull(campaignCaptor.getValue().getUpdatedAt());
    }

    @Test
    void shouldThrowExceptionWhenSenderAccountDoesNotBelongToUser() {
        CreateCampaignRequest request = createRequest();

        when(emailAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        InvalidCampaignException exception = assertThrows(
                InvalidCampaignException.class,
                () -> campaignService.createCampaign(1L, request)
        );

        assertEquals("Sender email account not found for this user.", exception.getMessage());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void shouldThrowExceptionWhenSenderAccountIsNotActive() {
        CreateCampaignRequest request = createRequest();

        when(emailAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(emailAccount(EmailAccountStatus.INVALID)));

        InvalidCampaignException exception = assertThrows(
                InvalidCampaignException.class,
                () -> campaignService.createCampaign(1L, request)
        );

        assertEquals("Sender email account must be ACTIVE.", exception.getMessage());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void shouldListCampaignsWithStatusFilter() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);

        when(campaignRepository.findByUserIdAndStatus(eq(1L), eq(CampaignStatus.DRAFT), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(campaign)));

        CampaignPageResponse response = campaignService.listCampaigns(1L, CampaignStatus.DRAFT, 0, 20);

        assertEquals(1, response.getContent().size());
        assertEquals("October launch", response.getContent().getFirst().getCampaignName());
        assertEquals(0, response.getPage());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void shouldGetCampaign() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);

        when(campaignRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.getCampaign(1L, 10L);

        assertEquals(10L, response.getId());
        assertEquals("October launch", response.getCampaignName());
    }

    @Test
    void shouldThrowExceptionWhenCampaignDoesNotExist() {
        when(campaignRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.empty());

        CampaignNotFoundException exception = assertThrows(
                CampaignNotFoundException.class,
                () -> campaignService.getCampaign(1L, 10L)
        );

        assertEquals("Campaign not found.", exception.getMessage());
    }

    @Test
    void shouldUpdateCampaign() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);
        UpdateCampaignRequest request = updateRequest(CampaignStatus.PAUSED, "Waiting for sender setup", null);

        when(campaignRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(campaign));
        when(emailAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(emailAccount(EmailAccountStatus.ACTIVE)));
        when(campaignRepository.save(any(Campaign.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CampaignResponse response = campaignService.updateCampaign(1L, 10L, request);

        assertEquals("Updated launch", response.getCampaignName());
        assertEquals(CampaignStatus.PAUSED, response.getStatus());
        assertEquals("Waiting for sender setup", response.getPausedReason());
    }

    @Test
    void shouldRejectRunningStatusFromCrudUpdate() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);
        UpdateCampaignRequest request = updateRequest(CampaignStatus.RUNNING, null, null);

        when(campaignRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(campaign));
        when(emailAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(emailAccount(EmailAccountStatus.ACTIVE)));

        InvalidCampaignException exception = assertThrows(
                InvalidCampaignException.class,
                () -> campaignService.updateCampaign(1L, 10L, request)
        );

        assertEquals("Campaigns cannot be moved to RUNNING from the CRUD API.", exception.getMessage());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void shouldRejectPausedStatusWithoutPauseReason() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);
        UpdateCampaignRequest request = updateRequest(CampaignStatus.PAUSED, " ", null);

        when(campaignRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(campaign));
        when(emailAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.of(emailAccount(EmailAccountStatus.ACTIVE)));

        InvalidCampaignException exception = assertThrows(
                InvalidCampaignException.class,
                () -> campaignService.updateCampaign(1L, 10L, request)
        );

        assertEquals("Paused campaigns require a pause reason.", exception.getMessage());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void shouldRejectUpdatingRunningCampaign() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.RUNNING);
        UpdateCampaignRequest request = updateRequest(CampaignStatus.DRAFT, null, null);

        when(campaignRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(campaign));

        InvalidCampaignException exception = assertThrows(
                InvalidCampaignException.class,
                () -> campaignService.updateCampaign(1L, 10L, request)
        );

        assertEquals("Running campaigns cannot be updated.", exception.getMessage());
        verify(emailAccountRepository, never()).findByIdAndUserId(any(), any());
        verify(campaignRepository, never()).save(any());
    }

    @Test
    void shouldDeleteCampaign() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);

        when(campaignRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(campaign));

        campaignService.deleteCampaign(1L, 10L);

        verify(campaignRepository).delete(campaign);
    }

    @Test
    void shouldRejectDeletingRunningCampaign() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.RUNNING);

        when(campaignRepository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(campaign));

        InvalidCampaignException exception = assertThrows(
                InvalidCampaignException.class,
                () -> campaignService.deleteCampaign(1L, 10L)
        );

        assertEquals("Running campaigns cannot be deleted.", exception.getMessage());
        verify(campaignRepository, never()).delete(any());
    }

    @Test
    void shouldStartDraftCampaignAndRecordPendingEvent() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);
        campaign.setUpdatedAt(LocalDateTime.now().minusDays(1));
        LocalDateTime previousUpdate = campaign.getUpdatedAt();
        when(campaignRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(campaign));
        when(emailAccountRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(emailAccount(EmailAccountStatus.ACTIVE)));

        CampaignResponse response = campaignService.startCampaign(1L, 10L);

        assertEquals(CampaignStatus.RUNNING, response.getStatus());
        assertEquals(10L, response.getId());
        assertTrue(campaign.getUpdatedAt().isAfter(previousUpdate));
        assertEquals(0, campaign.getEmailsSent());
        assertEquals(0, campaign.getEmailsFailed());
        ArgumentCaptor<CampaignOutboxEvent> captor = ArgumentCaptor.forClass(CampaignOutboxEvent.class);
        verify(campaignOutboxEventRepository).save(captor.capture());
        CampaignOutboxEvent event = captor.getValue();
        assertEquals(10L, event.getCampaignId());
        assertEquals(1L, event.getUserId());
        assertEquals(CampaignOutboxEvent.EventType.CAMPAIGN_START_REQUESTED, event.getEventType());
        assertEquals(CampaignOutboxEvent.Status.PENDING, event.getStatus());
        assertEquals(campaign.getUpdatedAt(), event.getCreatedAt());
        assertNull(event.getPublishedAt());
    }

    @Test
    void shouldRecordOnlyOneEventWhenStartIsRetried() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);
        when(campaignRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(campaign));
        when(emailAccountRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(emailAccount(EmailAccountStatus.ACTIVE)));

        campaignService.startCampaign(1L, 10L);
        LocalDateTime firstUpdate = campaign.getUpdatedAt();
        CampaignResponse retryResponse = campaignService.startCampaign(1L, 10L);

        assertEquals(CampaignStatus.RUNNING, retryResponse.getStatus());
        assertEquals(firstUpdate, campaign.getUpdatedAt());
        verify(campaignOutboxEventRepository, times(1)).save(any(CampaignOutboxEvent.class));
        verify(emailAccountRepository, times(1)).findByIdAndUserId(5L, 1L);
    }

    @Test
    void shouldReturnAlreadyRunningCampaignWithoutRevalidatingOrEnqueuing() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.RUNNING);
        LocalDateTime previousUpdate = campaign.getUpdatedAt();
        when(campaignRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(campaign));

        CampaignResponse response = campaignService.startCampaign(1L, 10L);

        assertEquals(CampaignStatus.RUNNING, response.getStatus());
        assertEquals(previousUpdate, campaign.getUpdatedAt());
        verifyNoInteractions(emailAccountRepository, campaignOutboxEventRepository);
    }

    @Test
    void shouldRejectStartWhenCampaignIsMissingOrOwnedByAnotherUser() {
        when(campaignRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.empty());

        assertThrows(CampaignNotFoundException.class, () -> campaignService.startCampaign(1L, 10L));

        verifyNoInteractions(emailAccountRepository, campaignOutboxEventRepository);
    }

    @ParameterizedTest
    @EnumSource(value = CampaignStatus.class,
            names = {"PAUSED", "COMPLETED", "COMPLETED_WITH_FAILURES", "FAILED"})
    void shouldRejectStartingNonDraftCampaign(CampaignStatus status) {
        Campaign campaign = campaign(10L, 1L, status);
        when(campaignRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(campaign));

        assertThrows(InvalidCampaignException.class, () -> campaignService.startCampaign(1L, 10L));

        assertEquals(status, campaign.getStatus());
        verifyNoInteractions(emailAccountRepository, campaignOutboxEventRepository);
    }

    @ParameterizedTest
    @MethodSource("invalidStartDetails")
    void shouldRejectStartingCampaignWithInvalidDetails(Consumer<Campaign> invalidate) {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);
        invalidate.accept(campaign);
        LocalDateTime previousUpdate = campaign.getUpdatedAt();
        when(campaignRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(campaign));

        assertThrows(InvalidCampaignException.class, () -> campaignService.startCampaign(1L, 10L));

        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());
        assertEquals(previousUpdate, campaign.getUpdatedAt());
        verifyNoInteractions(emailAccountRepository, campaignOutboxEventRepository);
    }

    private static Stream<Consumer<Campaign>> invalidStartDetails() {
        return Stream.of(
                c -> c.setSubject(null), c -> c.setSubject(""), c -> c.setSubject(" "),
                c -> c.setTemplateUrl(null), c -> c.setTemplateUrl(""), c -> c.setTemplateUrl(" "),
                c -> c.setCsvUrl(null), c -> c.setCsvUrl(""), c -> c.setCsvUrl(" "),
                c -> c.setTotalEmails(null), c -> c.setTotalEmails(0), c -> c.setTotalEmails(-1)
        );
    }

    @ParameterizedTest
    @EnumSource(value = EmailAccountStatus.class, names = "ACTIVE", mode = EnumSource.Mode.EXCLUDE)
    void shouldRejectStartWhenSenderIsInactive(EmailAccountStatus status) {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);
        when(campaignRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(campaign));
        when(emailAccountRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(emailAccount(status)));

        assertThrows(InvalidCampaignException.class, () -> campaignService.startCampaign(1L, 10L));

        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());
        verifyNoInteractions(campaignOutboxEventRepository);
    }

    @Test
    void shouldRejectStartWhenSenderDoesNotBelongToUser() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);
        when(campaignRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(campaign));
        when(emailAccountRepository.findByIdAndUserId(5L, 1L)).thenReturn(Optional.empty());

        assertThrows(InvalidCampaignException.class, () -> campaignService.startCampaign(1L, 10L));

        assertEquals(CampaignStatus.DRAFT, campaign.getStatus());
        verifyNoInteractions(campaignOutboxEventRepository);
    }

    @Test
    void shouldPropagateOutboxFailureToTransactionBoundary() {
        Campaign campaign = campaign(10L, 1L, CampaignStatus.DRAFT);
        when(campaignRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(campaign));
        when(emailAccountRepository.findByIdAndUserId(5L, 1L))
                .thenReturn(Optional.of(emailAccount(EmailAccountStatus.ACTIVE)));
        RuntimeException failure = new RuntimeException("Database unavailable");
        when(campaignOutboxEventRepository.save(any(CampaignOutboxEvent.class))).thenThrow(failure);

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> campaignService.startCampaign(1L, 10L)));
    }

    private CreateCampaignRequest createRequest() {
        return new CreateCampaignRequest(
                "October launch",
                "Hello from Queueify",
                "s3://templates/october.html",
                "s3://csv/october.csv",
                100,
                5L
        );
    }

    private UpdateCampaignRequest updateRequest(CampaignStatus status, String pausedReason, String failureReason) {
        return new UpdateCampaignRequest(
                "Updated launch",
                "Updated subject",
                "s3://templates/updated.html",
                "s3://csv/updated.csv",
                100,
                status,
                5L,
                failureReason,
                pausedReason
        );
    }

    private Campaign campaign(Long id, Long userId, CampaignStatus status) {
        LocalDateTime now = LocalDateTime.now();
        Campaign campaign = new Campaign(
                "October launch",
                userId,
                "Hello from Queueify",
                "s3://templates/october.html",
                "s3://csv/october.csv",
                100,
                0,
                0,
                status,
                5L,
                null,
                null,
                now,
                now
        );
        campaign.setId(id);
        return campaign;
    }

    private EmailAccount emailAccount(EmailAccountStatus status) {
        LocalDateTime now = LocalDateTime.now();
        EmailAccount emailAccount = new EmailAccount(
                1L,
                "sender@gmail.com",
                EmailProvider.GMAIL,
                EmailAuthType.APP_PASSWORD,
                "encrypted-password",
                status,
                null,
                now,
                now
        );
        emailAccount.setId(5L);
        return emailAccount;
    }
}
