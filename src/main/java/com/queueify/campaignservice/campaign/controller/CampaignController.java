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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/users/{userId}/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    private final UserRepository userRepository;

    public CampaignController(
            CampaignService campaignService,
            UserRepository userRepository) {
        this.campaignService = campaignService;
        this.userRepository = userRepository;
    }
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping("/{campaignId}/start")
    public ResponseEntity<CampaignResponse> startCampaign(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long campaignId,
            Authentication authentication
    ) {
        if (authentication == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }

        Long authenticatedUserId = userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED))
                .getId();

        if (!authenticatedUserId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return ResponseEntity.ok(
                campaignService.startCampaign(authenticatedUserId, campaignId)
        );
    }
}
