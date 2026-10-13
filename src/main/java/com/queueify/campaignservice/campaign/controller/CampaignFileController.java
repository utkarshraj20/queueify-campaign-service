package com.queueify.campaignservice.campaign.controller;

import com.queueify.campaignservice.authentication.repository.UserRepository;
import com.queueify.campaignservice.campaign.dto.CampaignResponse;
import com.queueify.campaignservice.campaign.dto.CreateCampaignWithFilesRequest;
import com.queueify.campaignservice.campaign.service.CampaignFileCreationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Validated
@RestController
@RequestMapping("/api/v1/users/{userId}/campaigns")
public class CampaignFileController {
    private final UserRepository userRepository;
    private final CampaignFileCreationService creationService;

    public CampaignFileController(UserRepository userRepository, CampaignFileCreationService creationService) {
        this.userRepository = userRepository;
        this.creationService = creationService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CampaignResponse> createWithFiles(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestPart("campaign") CreateCampaignWithFilesRequest request,
            @RequestPart("template") MultipartFile template,
            @RequestPart("recipients") MultipartFile recipients,
            Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!userId.equals(user.getId())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(creationService.create(userId, request, template, recipients));
    }
}
