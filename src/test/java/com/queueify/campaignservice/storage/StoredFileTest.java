package com.queueify.campaignservice.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StoredFileTest {

    @Test
    void shouldCreateS3UriFromBucketAndKey() {
        StoredFile storedFile = new StoredFile(
                "queueify-test-bucket",
                "campaign-assets/users/1/campaigns/10/input/leads.csv",
                "text/csv",
                128L
        );

        assertEquals("queueify-test-bucket", storedFile.getBucket());
        assertEquals("campaign-assets/users/1/campaigns/10/input/leads.csv", storedFile.getKey());
        assertEquals(
                "s3://queueify-test-bucket/campaign-assets/users/1/campaigns/10/input/leads.csv",
                storedFile.getUri()
        );
        assertEquals("text/csv", storedFile.getContentType());
        assertEquals(128L, storedFile.getSize());
    }
}
