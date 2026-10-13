package com.queueify.campaignservice.storage;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;

@Service
public class S3StorageService {
    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final S3KeyBuilder s3KeyBuilder;

    public S3StorageService(S3Client s3Client, S3Properties s3Properties, S3KeyBuilder s3KeyBuilder){
        this.s3Client = s3Client;
        this.s3Properties = s3Properties;
        this.s3KeyBuilder = s3KeyBuilder;
    }

    public StoredFile uploadCampaignInputCsv(Long userId, Long campaignId, MultipartFile file){
        String key = s3KeyBuilder.campaignInputCsvKey(userId, campaignId, file.getOriginalFilename());
        return upload(key, file);
    }

    private StoredFile upload(String key, MultipartFile file) {
        return upload(key, file, file.getContentType());
    }

    public StoredFile uploadCampaignAsset(Long userId, CampaignAssetType type, MultipartFile file) {
        String key = s3KeyBuilder.campaignAssetKey(userId, type, file.getOriginalFilename());
        return upload(key, file, type.contentType(file.getOriginalFilename()));
    }

    private StoredFile upload(String key, MultipartFile file, String contentType) {
        try (var input = file.getInputStream()) {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(input, file.getSize()));

            return new StoredFile(
                    s3Properties.getBucket(),
                    key,
                    contentType,
                    file.getSize()
            );
        }catch (IOException exception) {
            throw new StorageException("Failed to read file for S3 upload.", exception);
        } catch (S3Exception exception) {
            throw new StorageException("Failed to upload file to S3.", exception);
        } catch (SdkClientException exception) {
            throw new StorageException("Failed to connect to file storage.", exception);
        }
    }

    public ResponseInputStream<GetObjectResponse> download(String key){

        try{
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build();

            return s3Client.getObject(request);
        }
        catch (S3Exception exception){
            throw new StorageException("Failed to download file from S3.", exception);
        }
    }

    public void delete(String key) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucket())
                    .key(key)
                    .build();

            s3Client.deleteObject(request);
        } catch (S3Exception exception) {
            throw new StorageException("Failed to delete file from S3.", exception);
        }
    }

}
