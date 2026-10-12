package com.queueify.campaignservice.storage;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "queueify.aws.s3")
public class S3Properties {

    @NotBlank
    private String bucket;

    @NotBlank
    private String region;

    @NotBlank
    private String basePrefix;
}