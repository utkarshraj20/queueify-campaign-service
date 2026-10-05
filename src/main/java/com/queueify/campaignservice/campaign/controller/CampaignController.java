package com.queueify.campaignservice.campaign.controller;

import com.queueify.campaignservice.campaign.dto.CampaignPageResponse;
import com.queueify.campaignservice.campaign.dto.CampaignResponse;
import com.queueify.campaignservice.campaign.dto.CreateCampaignRequest;
import com.queueify.campaignservice.campaign.dto.UpdateCampaignRequest;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import com.queueify.campaignservice.campaign.service.CampaignService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/users/{userId}/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> createCampaign(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody CreateCampaignRequest request
    ) {
        CampaignResponse response = campaignService.createCampaign(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<CampaignPageResponse> listCampaigns(
            @PathVariable @Min(1) Long userId,
            @RequestParam(required = false) CampaignStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        CampaignPageResponse response = campaignService.listCampaigns(userId, status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> getCampaign(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long campaignId
    ) {
        CampaignResponse response = campaignService.getCampaign(userId, campaignId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{campaignId}")
    public ResponseEntity<CampaignResponse> updateCampaign(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long campaignId,
            @Valid @RequestBody UpdateCampaignRequest request
    ) {
        CampaignResponse response = campaignService.updateCampaign(userId, campaignId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{campaignId}")
    public ResponseEntity<Void> deleteCampaign(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long campaignId
    ) {
        campaignService.deleteCampaign(userId, campaignId);
        return ResponseEntity.noContent().build();
    }
}
