package com.queueify.campaignservice.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;

@Service
public class CampaignAssetUploadService {
    private final S3StorageService storageService;

    public CampaignAssetUploadService(S3StorageService storageService) {
        this.storageService = storageService;
    }

    public StoredFile upload(Long userId, CampaignAssetType type, MultipartFile file) {
        validate(type, file);
        return storageService.uploadCampaignAsset(userId, type, file);
    }

    public void validate(CampaignAssetType type, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidUploadException("Select a non-empty file.");
        }
        if (file.getSize() > type.getMaxBytes()) {
            throw new UploadTooLargeException(type == CampaignAssetType.TEMPLATE
                    ? "Template files must be 1 MB or smaller."
                    : "Recipients CSV files must be 25 MB or smaller.");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isBlank() || filename.length() > 255 || !type.accepts(filename)) {
            throw new InvalidUploadException(type == CampaignAssetType.TEMPLATE
                    ? "Select an HTML or TXT template file with a filename of at most 255 characters."
                    : "Select a CSV file with a filename of at most 255 characters.");
        }
        validateText(file);
    }

    private void validateText(MultipartFile file) {
        var decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT);
        try (var reader = new InputStreamReader(file.getInputStream(), decoder)) {
            char[] buffer = new char[8192];
            boolean hasContent = false;
            int count;
            while ((count = reader.read(buffer)) != -1) {
                for (int i = 0; i < count; i++) {
                    char value = buffer[i];
                    if (value == '\0') throw new InvalidUploadException("Binary files are not supported.");
                    if (value != '\uFEFF' && !Character.isWhitespace(value)) hasContent = true;
                }
            }
            if (!hasContent) throw new InvalidUploadException("The selected file contains no text.");
        } catch (IOException exception) {
            throw new InvalidUploadException("The file could not be read as UTF-8 text.");
        }
    }
}
