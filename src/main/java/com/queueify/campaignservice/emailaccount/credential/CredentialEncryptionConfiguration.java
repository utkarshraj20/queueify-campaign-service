package com.queueify.campaignservice.emailaccount.credential;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "queueify.credentials.encryption.enabled", havingValue = "true")
public class CredentialEncryptionConfiguration {
    @Bean
    CredentialCipher credentialCipher(@Value("${queueify.credentials.encryption.key:}") String key) {
        return new CredentialCipher(key);
    }
}
