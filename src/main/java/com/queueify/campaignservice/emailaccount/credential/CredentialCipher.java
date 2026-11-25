package com.queueify.campaignservice.emailaccount.credential;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/** Versioned AES-256-GCM envelope. No plaintext fallback or embedded key. */
public final class CredentialCipher {
    private static final String PREFIX = "qcred:v1:";
    private static final int NONCE_BYTES = 12;
    private static final int MAX_PLAINTEXT_BYTES = 256;
    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public CredentialCipher(String base64Key) {
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(base64Key == null ? "" : base64Key);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Credential key must be Base64-encoded 32 bytes.");
        }
        if (decoded.length != 32) {
            throw new IllegalArgumentException("Credential key must be Base64-encoded 32 bytes.");
        }
        this.key = new SecretKeySpec(decoded, "AES");
        java.util.Arrays.fill(decoded, (byte) 0);
    }

    public String encrypt(long userId, String password) {
        validateOwner(userId);
        if (password == null || password.isBlank() || password.indexOf('\0') >= 0) throw failure();
        byte[] plaintext = password.getBytes(StandardCharsets.UTF_8);
        if (plaintext.length > MAX_PLAINTEXT_BYTES) {
            java.util.Arrays.fill(plaintext, (byte) 0);
            throw failure();
        }
        try {
            byte[] nonce = new byte[NONCE_BYTES];
            random.nextBytes(nonce);
            byte[] encrypted = cipher(Cipher.ENCRYPT_MODE, userId, nonce).doFinal(plaintext);
            byte[] envelope = ByteBuffer.allocate(nonce.length + encrypted.length)
                    .put(nonce).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(envelope);
        } catch (GeneralSecurityException e) {
            throw failure();
        } finally {
            java.util.Arrays.fill(plaintext, (byte) 0);
        }
    }

    public String decrypt(long userId, String envelope) {
        validateOwner(userId);
        if (envelope == null || !envelope.startsWith(PREFIX) || envelope.length() > 512) throw failure();
        byte[] plaintext = null;
        try {
            byte[] bytes = Base64.getDecoder().decode(envelope.substring(PREFIX.length()));
            if (bytes.length < NONCE_BYTES + 16 + 1 || bytes.length > NONCE_BYTES + 16 + MAX_PLAINTEXT_BYTES) {
                throw failure();
            }
            byte[] nonce = java.util.Arrays.copyOfRange(bytes, 0, NONCE_BYTES);
            plaintext = cipher(Cipher.DECRYPT_MODE, userId, nonce)
                    .doFinal(bytes, NONCE_BYTES, bytes.length - NONCE_BYTES);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw failure();
        } finally {
            if (plaintext != null) java.util.Arrays.fill(plaintext, (byte) 0);
        }
    }

    private Cipher cipher(int mode, long userId, byte[] nonce) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(mode, key, new GCMParameterSpec(128, nonce));
        cipher.updateAAD(("queueify:app-password:v1:user:" + userId).getBytes(StandardCharsets.UTF_8));
        return cipher;
    }

    private static void validateOwner(long userId) {
        if (userId <= 0) throw failure();
    }

    private static IllegalArgumentException failure() {
        return new IllegalArgumentException("Sender credential could not be processed.");
    }
}
