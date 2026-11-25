package com.queueify.campaignservice.emailaccount.credential;

import com.queueify.campaignservice.emailaccount.exception.InvalidEmailAccountException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Optional rollout: absent cipher retains the explicitly documented legacy write mode. */
@Component
public class CredentialStorage {
    private final CredentialCipher cipher;

    public CredentialStorage(ObjectProvider<CredentialCipher> cipher) {
        this.cipher = cipher.getIfAvailable();
    }

    public String encode(long userId, String suppliedPassword) {
        // The request carries plaintext, never a previously stored server envelope.
        if (suppliedPassword != null && suppliedPassword.startsWith("qcred:")) {
            throw new InvalidEmailAccountException("Enter the app password, not a stored credential value.");
        }
        if (cipher == null) return suppliedPassword;
        try {
            return cipher.encrypt(userId, suppliedPassword);
        } catch (IllegalArgumentException e) {
            throw new InvalidEmailAccountException("App password must be nonblank, contain no NUL, and fit within 256 UTF-8 bytes.");
        }
    }
}
