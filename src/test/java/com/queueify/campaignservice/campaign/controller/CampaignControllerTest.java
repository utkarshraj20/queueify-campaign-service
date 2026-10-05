package com.queueify.campaignservice.campaign.controller;

import com.queueify.campaignservice.campaign.dto.CampaignPageResponse;
import com.queueify.campaignservice.campaign.dto.CampaignResponse;
import com.queueify.campaignservice.campaign.dto.CreateCampaignRequest;
import com.queueify.campaignservice.campaign.dto.UpdateCampaignRequest;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import com.queueify.campaignservice.campaign.service.CampaignService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class CampaignControllerTest {

    private final CampaignService campaignService = mock(CampaignService.class);
    private final CampaignController campaignController = new CampaignController(campaignService);

    @Test
    void shouldCreateCampaign() {
        CreateCampaignRequest request = createRequest();
        CampaignResponse campaignResponse = campaignResponse();

        when(campaignService.createCampaign(1L, request)).thenReturn(campaignResponse);

        ResponseEntity<CampaignResponse> response = campaignController.createCampaign(1L, request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(campaignResponse, response.getBody());
    }

    @Test
    void shouldListCampaigns() {
        CampaignPageResponse pageResponse = new CampaignPageResponse(
                List.of(campaignResponse()),
                0,
                20,
                1,
                1,
                true
        );

        when(campaignService.listCampaigns(1L, CampaignStatus.DRAFT, 0, 20)).thenReturn(pageResponse);

        ResponseEntity<CampaignPageResponse> response =
                campaignController.listCampaigns(1L, CampaignStatus.DRAFT, 0, 20);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(pageResponse, response.getBody());
    }

    @Test
    void shouldGetCampaign() {
        CampaignResponse campaignResponse = campaignResponse();

        when(campaignService.getCampaign(1L, 10L)).thenReturn(campaignResponse);

        ResponseEntity<CampaignResponse> response = campaignController.getCampaign(1L, 10L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(campaignResponse, response.getBody());
    }

    @Test
    void shouldUpdateCampaign() {
        UpdateCampaignRequest request = updateRequest();
        CampaignResponse campaignResponse = campaignResponse();

        when(campaignService.updateCampaign(1L, 10L, request)).thenReturn(campaignResponse);

        ResponseEntity<CampaignResponse> response = campaignController.updateCampaign(1L, 10L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(campaignResponse, response.getBody());
    }

    @Test
    void shouldDeleteCampaign() {
        ResponseEntity<Void> response = campaignController.deleteCampaign(1L, 10L);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        assertNull(response.getBody());
        verify(campaignService).deleteCampaign(1L, 10L);
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

    private UpdateCampaignRequest updateRequest() {
        return new UpdateCampaignRequest(
                "Updated launch",
                "Updated subject",
                "s3://templates/updated.html",
                "s3://csv/updated.csv",
                100,
                CampaignStatus.DRAFT,
                5L,
                null,
                null
        );
    }

    private CampaignResponse campaignResponse() {
        LocalDateTime now = LocalDateTime.now();

        return new CampaignResponse(
                10L,
                "October launch",
                1L,
                "Hello from Queueify",
                "s3://templates/october.html",
                "s3://csv/october.csv",
                100,
                0,
                0,
                CampaignStatus.DRAFT,
                5L,
                null,
                null,
                now,
                now
        );
    }
}
