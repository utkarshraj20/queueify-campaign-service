package com.queueify.campaignservice.emailaccount.controller;

import com.queueify.campaignservice.emailaccount.dto.CreateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountPageResponse;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountResponse;
import com.queueify.campaignservice.emailaccount.dto.UpdateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.service.EmailAccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/v1/users/{userId}/email-accounts")
public class EmailAccountController {

    private final EmailAccountService emailAccountService;

    public EmailAccountController(EmailAccountService emailAccountService) {
        this.emailAccountService = emailAccountService;
    }

    @PostMapping
    public ResponseEntity<EmailAccountResponse> createEmailAccount(
            @PathVariable @Min(1) Long userId,
            @Valid @RequestBody CreateEmailAccountRequest request
    ) {
        EmailAccountResponse response = emailAccountService.createEmailAccount(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<EmailAccountPageResponse> listEmailAccounts(
            @PathVariable @Min(1) Long userId,
            @RequestParam(required = false) EmailAccountStatus status,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        EmailAccountPageResponse response = emailAccountService.listEmailAccounts(userId, status, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{emailAccountId}")
    public ResponseEntity<EmailAccountResponse> getEmailAccount(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long emailAccountId
    ) {
        EmailAccountResponse response = emailAccountService.getEmailAccount(userId, emailAccountId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{emailAccountId}")
    public ResponseEntity<EmailAccountResponse> updateEmailAccount(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long emailAccountId,
            @Valid @RequestBody UpdateEmailAccountRequest request
    ) {
        EmailAccountResponse response = emailAccountService.updateEmailAccount(userId, emailAccountId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{emailAccountId}")
    public ResponseEntity<Void> deleteEmailAccount(
            @PathVariable @Min(1) Long userId,
            @PathVariable @Min(1) Long emailAccountId
    ) {
        emailAccountService.deleteEmailAccount(userId, emailAccountId);
        return ResponseEntity.noContent().build();
    }
}
