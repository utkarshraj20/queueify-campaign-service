package com.queueify.campaignservice.internal;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class InternalCampaignConfigurationTest {
    @Test void tokenIsRequiredOnlyWhenEnabledAndNeverPrinted() {
        assertThatCode(()->new InternalCampaignProperties(false,"")).doesNotThrowAnyException();
        assertThatThrownBy(()->new InternalCampaignProperties(true,"short")).isInstanceOf(IllegalArgumentException.class);
        String token="test-token-123456789012345678901234567890";
        assertThat(new InternalCampaignProperties(true,token).toString()).doesNotContain(token);
    }
}
