package com.queueify.campaignservice.emailaccount.credential;

import com.queueify.campaignservice.emailaccount.dto.*;
import com.queueify.campaignservice.emailaccount.entity.*;
import com.queueify.campaignservice.emailaccount.repository.EmailAccountRepository;
import com.queueify.campaignservice.emailaccount.service.EmailAccountService;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

class CredentialStorageTest {
    private final String key = Base64.getEncoder().encodeToString(new byte[32]);
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CredentialEncryptionConfiguration.class, CredentialStorage.class);

    @Test void defaultsToLegacyAndRejectsServerEnvelopes() {
        runner.run(context -> {
            assertThat(context).doesNotHaveBean(CredentialCipher.class);
            var storage = context.getBean(CredentialStorage.class);
            assertThat(storage.encode(1, "password")).isEqualTo("password");
            assertThatThrownBy(() -> storage.encode(1, "qcred:v1:value"))
                    .hasMessage("Enter the app password, not a stored credential value.");
        });
    }

    @Test void enabledWithoutAValidKeyFailsStartup() {
        runner.withPropertyValues("queueify.credentials.encryption.enabled=true")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("queueify.credentials.encryption.enabled=true", "queueify.credentials.encryption.key=bad")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test void createAndUpdateEncryptAndDoNotReturnCredentials() {
        runner.withPropertyValues("queueify.credentials.encryption.enabled=true", "queueify.credentials.encryption.key=" + key)
                .run(context -> {
                    var repository = mock(EmailAccountRepository.class);
                    when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
                    var service = new EmailAccountService(repository, context.getBean(CredentialStorage.class));
                    var response = service.createEmailAccount(1L, new CreateEmailAccountRequest(
                            "sender@gmail.com", EmailProvider.GMAIL, EmailAuthType.APP_PASSWORD, "first-password", null));
                    var captor = org.mockito.ArgumentCaptor.forClass(EmailAccount.class);
                    verify(repository).save(captor.capture());
                    EmailAccount account = captor.getValue();
                    String original = account.getEncryptedAppPassword();
                    assertThat(original).startsWith("qcred:v1:").doesNotContain("first-password");
                    assertThat(context.getBean(CredentialCipher.class).decrypt(1, original)).isEqualTo("first-password");
                    assertThat(new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules().writeValueAsString(response))
                            .doesNotContain("first-password", "qcred:", "encryptedAppPassword");
                    account.setId(10L);
                    account.setEncryptedAppPassword("legacy-value");
                    when(repository.findByIdAndUserId(10L, 1L)).thenReturn(Optional.of(account));
                    service.updateEmailAccount(1L, 10L, new UpdateEmailAccountRequest("sender@gmail.com",
                            EmailProvider.GMAIL, EmailAuthType.APP_PASSWORD, "new-password", EmailAccountStatus.ACTIVE, null));
                    assertThat(context.getBean(CredentialCipher.class).decrypt(1, account.getEncryptedAppPassword()))
                            .isEqualTo("new-password");
                    assertThatThrownBy(() -> service.updateEmailAccount(1L, 10L, new UpdateEmailAccountRequest(
                            "changed@gmail.com", EmailProvider.GMAIL, EmailAuthType.APP_PASSWORD, "x".repeat(257),
                            EmailAccountStatus.ACTIVE, null))).hasMessageContaining("256 UTF-8 bytes");
                    assertThat(account.getSenderEmail()).isEqualTo("sender@gmail.com");
                    verify(repository, times(2)).save(any());
                });
    }
}
