package com.queueify.campaignservice.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CampaignAssetUploadServiceTest {
    private final S3StorageService storage = mock(S3StorageService.class);
    private final CampaignAssetUploadService service = new CampaignAssetUploadService(storage);

    @ParameterizedTest
    @CsvSource({"TEMPLATE,welcome.html", "TEMPLATE,welcome.htm", "TEMPLATE,welcome.TXT", "RECIPIENTS,leads.CSV"})
    void uploadsSupportedTextFiles(CampaignAssetType type, String filename) {
        var file = textFile(filename, "\uFEFFHello,\nworld");
        var stored = new StoredFile("bucket", "key", type.contentType(filename), file.getSize());
        when(storage.uploadCampaignAsset(2L, type, file)).thenReturn(stored);

        assertSame(stored, service.upload(2L, type, file));
        verify(storage).uploadCampaignAsset(2L, type, file);
    }

    @Test
    void rejectsMissingAndEmptyFiles() {
        assertThrows(InvalidUploadException.class, () -> service.upload(2L, CampaignAssetType.TEMPLATE, null));
        assertThrows(InvalidUploadException.class,
                () -> service.upload(2L, CampaignAssetType.TEMPLATE, textFile("template.html", "")));
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"malware.exe", "template.html.exe", "recipients.csv", " "})
    void rejectsInvalidTemplateNames(String filename) {
        assertThrows(InvalidUploadException.class,
                () -> service.upload(2L, CampaignAssetType.TEMPLATE, textFile(filename, "test")));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsWrongRecipientsExtensionAndLongName() {
        assertThrows(InvalidUploadException.class,
                () -> service.upload(2L, CampaignAssetType.RECIPIENTS, textFile("leads.xlsx", "test")));
        assertThrows(InvalidUploadException.class,
                () -> service.upload(2L, CampaignAssetType.RECIPIENTS, textFile("a".repeat(256) + ".csv", "test")));
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TEMPLATE", "RECIPIENTS"})
    void rejectsOversizedFiles(CampaignAssetType type) {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getSize()).thenReturn(type.getMaxBytes() + 1);
        assertThrows(UploadTooLargeException.class, () -> service.upload(2L, type, file));
        verifyNoInteractions(storage);
    }

    @ParameterizedTest
    @ValueSource(strings = {" ", "\n\r\t", "\uFEFF", "a\0b"})
    void rejectsWhitespaceAndBinaryContent(String content) {
        assertThrows(InvalidUploadException.class,
                () -> service.upload(2L, CampaignAssetType.RECIPIENTS, textFile("leads.csv", content)));
        verifyNoInteractions(storage);
    }

    @Test
    void rejectsMalformedUtf8() {
        var file = new MockMultipartFile("file", "leads.csv", "text/csv", new byte[]{(byte) 0xC3, 0x28});
        assertThrows(InvalidUploadException.class, () -> service.upload(2L, CampaignAssetType.RECIPIENTS, file));
        verifyNoInteractions(storage);
    }

    @Test
    void handlesUnreadableInputWithoutUploading() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("leads.csv");
        when(file.getInputStream()).thenThrow(new IOException("Read failed"));
        assertThrows(InvalidUploadException.class, () -> service.upload(2L, CampaignAssetType.RECIPIENTS, file));
        verifyNoInteractions(storage);
    }

    private MockMultipartFile textFile(String filename, String text) {
        return new MockMultipartFile("file", filename, "application/octet-stream", text.getBytes(StandardCharsets.UTF_8));
    }
}
