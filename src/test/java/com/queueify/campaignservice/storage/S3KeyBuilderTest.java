package com.queueify.campaignservice.storage;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class S3KeyBuilderTest {
    @Test
    void buildsUniqueOwnerScopedAssetKeysAndSanitizesPaths() {
        S3KeyBuilder builder = new S3KeyBuilder(s3Properties("/campaign-assets/"));
        String first = builder.campaignAssetKey(2L, CampaignAssetType.TEMPLATE, "../../Welcome @ Oct.html");
        String second = builder.campaignAssetKey(2L, CampaignAssetType.TEMPLATE, "../../Welcome @ Oct.html");
        assertTrue(first.matches("campaign-assets/users/2/uploads/templates/[a-f0-9-]{36}-Welcome___Oct.html"));
        assertNotEquals(first, second);
        assertTrue(builder.campaignAssetKey(2L, CampaignAssetType.RECIPIENTS, "C:\\fakepath\\leads.csv")
                .matches("campaign-assets/users/2/uploads/recipients/[a-f0-9-]{36}-leads.csv"));
    }


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
