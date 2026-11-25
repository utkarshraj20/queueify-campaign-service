package com.queueify.campaignservice.emailaccount.credential;

import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CredentialCipherTest {
    private final String key = Base64.getEncoder().encodeToString(new byte[32]);
    private final CredentialCipher cipher = new CredentialCipher(key);

    @Test void roundTripsUnicodeAndFitsExistingColumn() {
        String encrypted = cipher.encrypt(7, "demo-密碼");
        assertThat(encrypted).startsWith("qcred:v1:").doesNotContain("demo").hasSizeLessThanOrEqualTo(512);
        assertThat(new CredentialCipher(key).decrypt(7, encrypted)).isEqualTo("demo-密碼");
        assertThat(cipher.encrypt(7, "a".repeat(256))).hasSizeLessThanOrEqualTo(512);
    }

    @Test void usesNewNonceForEveryEncryption() {
        assertThat(cipher.encrypt(7, "same")).isNotEqualTo(cipher.encrypt(7, "same"));
    }

    @Test void rejectsWrongOwnerWrongKeyAndTampering() {
        String encrypted = cipher.encrypt(7, "private");
        assertThatThrownBy(() -> cipher.decrypt(8, encrypted)).hasMessage("Sender credential could not be processed.");
        byte[] otherKey = new byte[32]; otherKey[0] = 1;
        assertThatThrownBy(() -> new CredentialCipher(Base64.getEncoder().encodeToString(otherKey)).decrypt(7, encrypted))
                .isInstanceOf(IllegalArgumentException.class);
        byte[] data = Base64.getDecoder().decode(encrypted.substring("qcred:v1:".length()));
        data[data.length - 1] ^= 1;
        assertThatThrownBy(() -> cipher.decrypt(7, "qcred:v1:" + Base64.getEncoder().encodeToString(data)))
                .hasMessage("Sender credential could not be processed.");
    }

    @Test void neverFallsBackToPlaintextOrUnknownVersions() {
        for (String value : new String[]{"plain-secret", "qcred:v2:abc", "qcred:v1:???", "qcred:v1:AA=="}) {
            assertThatThrownBy(() -> cipher.decrypt(7, value)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test void validatesKeyWithoutLeakingIt() {
        for (String value : new String[]{"invalid-secret", "", Base64.getEncoder().encodeToString(new byte[16])}) {
            assertThatThrownBy(() -> new CredentialCipher(value))
                    .hasMessage("Credential key must be Base64-encoded 32 bytes.");
        }
    }

    @Test void validatesOwnerAndPasswordLimits() {
        assertThatThrownBy(() -> cipher.encrypt(0, "password")).isInstanceOf(IllegalArgumentException.class);
        for (String value : new String[]{"", " ", "a\0b", "a".repeat(257), "密".repeat(86)}) {
            assertThatThrownBy(() -> cipher.encrypt(7, value)).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
