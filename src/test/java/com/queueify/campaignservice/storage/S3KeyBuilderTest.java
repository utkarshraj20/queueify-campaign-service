package com.queueify.campaignservice.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class S3KeyBuilderTest {

    @Test
    void shouldBuildCampaignInputCsvKey() {
        S3KeyBuilder keyBuilder = new S3KeyBuilder(s3Properties("campaign-assets"));

        String key = keyBuilder.campaignInputCsvKey(1L, 10L, "leads.csv");

        assertTrue(key.startsWith("campaign-assets/users/1/campaigns/10/input/"));
        assertTrue(key.endsWith("-leads.csv"));
    }

    @Test
    void shouldSanitizeCampaignInputCsvFilename() {
        S3KeyBuilder keyBuilder = new S3KeyBuilder(s3Properties("/campaign-assets/"));

        String key = keyBuilder.campaignInputCsvKey(1L, 10L, "../../My Leads @ Oct.csv");

        assertTrue(key.startsWith("campaign-assets/users/1/campaigns/10/input/"));
        assertTrue(key.endsWith("-My_Leads___Oct.csv"));
    }

    @Test
    void shouldUseDefaultFilenameWhenOriginalFilenameIsMissing() {
        S3KeyBuilder keyBuilder = new S3KeyBuilder(s3Properties("campaign-assets"));

        String key = keyBuilder.campaignInputCsvKey(1L, 10L, null);

        assertTrue(key.endsWith("-upload.csv"));
    }

    @Test
    void shouldCleanExtraSlashesFromBasePrefix() {
        S3KeyBuilder keyBuilder = new S3KeyBuilder(s3Properties("///campaign-assets///"));

        String key = keyBuilder.campaignInputCsvKey(1L, 10L, "leads.csv");

        assertTrue(key.startsWith("campaign-assets/users/1/campaigns/10/input/"));
    }

    private S3Properties s3Properties(String basePrefix) {
        S3Properties properties = new S3Properties();
        properties.setBasePrefix(basePrefix);
        return properties;
    }
}
