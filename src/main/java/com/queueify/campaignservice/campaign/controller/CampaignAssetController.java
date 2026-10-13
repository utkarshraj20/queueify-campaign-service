package com.queueify.campaignservice.campaign.controller;

import com.queueify.campaignservice.authentication.repository.UserRepository;
import com.queueify.campaignservice.storage.CampaignAssetType;
import com.queueify.campaignservice.storage.CampaignAssetUploadService;
import com.queueify.campaignservice.storage.StoredFile;
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
@RequestMapping("/api/v1/users/{userId}/campaign-assets")
public class CampaignAssetController {
    private final UserRepository userRepository;
    private final CampaignAssetUploadService uploadService;

    public CampaignAssetController(UserRepository userRepository, CampaignAssetUploadService uploadService) {
        this.userRepository = userRepository;
        this.uploadService = uploadService;
    }

    @PostMapping(value = "/template", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoredFile> uploadTemplate(@PathVariable @Min(1) Long userId,
            @RequestPart("file") MultipartFile file, Authentication authentication) {
        requireOwner(userId, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uploadService.upload(userId, CampaignAssetType.TEMPLATE, file));
    }

    @PostMapping(value = "/recipients", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<StoredFile> uploadRecipients(@PathVariable @Min(1) Long userId,
            @RequestPart("file") MultipartFile file, Authentication authentication) {
        requireOwner(userId, authentication);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(uploadService.upload(userId, CampaignAssetType.RECIPIENTS, file));
    }

    private void requireOwner(Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (!userId.equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
