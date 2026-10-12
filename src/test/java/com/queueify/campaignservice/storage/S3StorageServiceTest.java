package com.queueify.campaignservice.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageServiceTest {

    private S3Client s3Client;
    private S3StorageService s3StorageService;

    @BeforeEach
    void setUp() {
        s3Client = mock(S3Client.class);
        S3Properties s3Properties = s3Properties();
        S3KeyBuilder s3KeyBuilder = new S3KeyBuilder(s3Properties);
        s3StorageService = new S3StorageService(s3Client, s3Properties, s3KeyBuilder);
    }

    @Test
    void shouldUploadCampaignInputCsv() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "leads.csv",
                "text/csv",
                "email\nuser@example.com".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        StoredFile storedFile = s3StorageService.uploadCampaignInputCsv(1L, 10L, file);

        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));

        PutObjectRequest request = requestCaptor.getValue();
        assertEquals("queueify-test-bucket", request.bucket());
        assertEquals("text/csv", request.contentType());
        assertEquals(file.getSize(), request.contentLength());
        assertEquals(request.key(), storedFile.getKey());
        assertEquals("queueify-test-bucket", storedFile.getBucket());
        assertEquals("s3://queueify-test-bucket/" + request.key(), storedFile.getUri());
        assertEquals("text/csv", storedFile.getContentType());
        assertEquals(file.getSize(), storedFile.getSize());
    }

    @Test
    void shouldWrapIOExceptionWhenUploadFileCannotBeRead() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("leads.csv");
        when(file.getContentType()).thenReturn("text/csv");
        when(file.getSize()).thenReturn(10L);
        when(file.getInputStream()).thenThrow(new IOException("cannot read"));

        StorageException exception = assertThrows(
                StorageException.class,
                () -> s3StorageService.uploadCampaignInputCsv(1L, 10L, file)
        );

        assertEquals("Failed to read file for S3 upload.", exception.getMessage());
    }

    @Test
    void shouldWrapS3ExceptionWhenUploadFails() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "leads.csv",
                "text/csv",
                "email\nuser@example.com".getBytes()
        );

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(s3Exception());

        StorageException exception = assertThrows(
                StorageException.class,
                () -> s3StorageService.uploadCampaignInputCsv(1L, 10L, file)
        );

        assertEquals("Failed to upload file to S3.", exception.getMessage());
    }

    @Test
    void shouldDownloadFile() {
        @SuppressWarnings("unchecked")
        ResponseInputStream<GetObjectResponse> responseInputStream = mock(ResponseInputStream.class);
        when(s3Client.getObject(any(GetObjectRequest.class))).thenReturn(responseInputStream);

        ResponseInputStream<GetObjectResponse> response = s3StorageService.download("campaign-assets/users/1/file.csv");

        ArgumentCaptor<GetObjectRequest> requestCaptor = ArgumentCaptor.forClass(GetObjectRequest.class);
        verify(s3Client).getObject(requestCaptor.capture());

        GetObjectRequest request = requestCaptor.getValue();
        assertEquals("queueify-test-bucket", request.bucket());
        assertEquals("campaign-assets/users/1/file.csv", request.key());
        assertSame(responseInputStream, response);
    }

    @Test
    void shouldWrapS3ExceptionWhenDownloadFails() {
        when(s3Client.getObject(any(GetObjectRequest.class))).thenThrow(s3Exception());

        StorageException exception = assertThrows(
                StorageException.class,
                () -> s3StorageService.download("campaign-assets/users/1/file.csv")
        );

        assertEquals("Failed to download file from S3.", exception.getMessage());
    }

    @Test
    void shouldDeleteFile() {
        s3StorageService.delete("campaign-assets/users/1/file.csv");

        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());

        DeleteObjectRequest request = requestCaptor.getValue();
        assertEquals("queueify-test-bucket", request.bucket());
        assertEquals("campaign-assets/users/1/file.csv", request.key());
    }

    @Test
    void shouldWrapS3ExceptionWhenDeleteFails() {
        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenThrow(s3Exception());

        StorageException exception = assertThrows(
                StorageException.class,
                () -> s3StorageService.delete("campaign-assets/users/1/file.csv")
        );

        assertEquals("Failed to delete file from S3.", exception.getMessage());
    }

    private S3Properties s3Properties() {
        S3Properties properties = new S3Properties();
        properties.setBucket("queueify-test-bucket");
        properties.setRegion("ap-south-1");
        properties.setBasePrefix("campaign-assets");
        return properties;
    }

    private S3Exception s3Exception() {
        return mock(S3Exception.class);
    }
}
