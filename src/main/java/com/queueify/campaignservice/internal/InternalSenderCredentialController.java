package com.queueify.campaignservice.internal;

import com.queueify.campaignservice.emailaccount.credential.CredentialCipher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/v1/campaigns")
@ConditionalOnProperty(name={"queueify.internal.campaign-api.enabled", "queueify.internal.campaign-api.sender-credentials-enabled"}, havingValue="true")
public class InternalSenderCredentialController {
    private final SenderCredentialRepository repository;
    private final CredentialCipher cipher;
    public InternalSenderCredentialController(SenderCredentialRepository repository, CredentialCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    @GetMapping("/{campaignId}/sender-credentials")
    public ResponseEntity<SenderCredentials> get(@PathVariable long campaignId, HttpServletRequest request) {
        // Plain HTTP is only allowed for direct local development, never forwarded requests.
        String peer = request.getRemoteAddr();
        boolean local = "127.0.0.1".equals(peer) || "::1".equals(peer) || "0:0:0:0:0:0:0:1".equals(peer);
        if (!request.isSecure() && (!local || request.getHeader("Forwarded") != null
                || request.getHeader("X-Forwarded-For") != null || request.getHeader("X-Forwarded-Proto") != null)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        if (campaignId < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var sender = repository.find(campaignId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!sender.eligible()) throw new ResponseStatusException(HttpStatus.CONFLICT);
        String password;
        try {
            password = cipher.decrypt(sender.userId(), sender.envelope());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT);
        }
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache")
                .body(new SenderCredentials(campaignId, sender.userId(), sender.senderId(), sender.email(),
                        sender.provider(), sender.authType(), password));
    }

    public record SenderCredentials(long campaignId, long userId, long senderEmailAccountId,
                                    String senderEmail, String provider, String authType, String appPassword) {
        @Override public String toString() { return "SenderCredentials[REDACTED]"; }
    }
}
