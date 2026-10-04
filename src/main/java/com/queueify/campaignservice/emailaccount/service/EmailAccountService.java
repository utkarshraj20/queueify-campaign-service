package com.queueify.campaignservice.emailaccount.service;

import com.queueify.campaignservice.emailaccount.dto.CreateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountPageResponse;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountResponse;
import com.queueify.campaignservice.emailaccount.dto.UpdateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.entity.EmailAccount;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.entity.EmailAuthType;
import com.queueify.campaignservice.emailaccount.entity.EmailProvider;
import com.queueify.campaignservice.emailaccount.exception.DuplicateEmailAccountException;
import com.queueify.campaignservice.emailaccount.exception.EmailAccountNotFoundException;
import com.queueify.campaignservice.emailaccount.exception.InvalidEmailAccountException;
import com.queueify.campaignservice.emailaccount.repository.EmailAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class EmailAccountService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private final EmailAccountRepository emailAccountRepository;

    public EmailAccountService(EmailAccountRepository emailAccountRepository) {
        this.emailAccountRepository = emailAccountRepository;
    }

    public EmailAccountResponse createEmailAccount(Long userId, CreateEmailAccountRequest request) {
        validateEmailAccount(
                request.getSenderEmail(),
                request.getProvider(),
                request.getAuthType(),
                request.getEncryptedAppPassword(),
                request.getMetadata()
        );

        if (emailAccountRepository.existsByUserIdAndSenderEmail(userId, request.getSenderEmail())) {
            throw new DuplicateEmailAccountException("Email account already exists for this user.");
        }

        LocalDateTime now = LocalDateTime.now();
        EmailAccount emailAccount = new EmailAccount(
                userId,
                request.getSenderEmail(),
                request.getProvider(),
                request.getAuthType(),
                request.getEncryptedAppPassword(),
                EmailAccountStatus.ACTIVE,
                request.getMetadata(),
                now,
                now
        );

        return EmailAccountResponse.from(emailAccountRepository.save(emailAccount));
    }

    public EmailAccountPageResponse listEmailAccounts(Long userId, EmailAccountStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, normalizePageSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<EmailAccount> emailAccountPage = status == null
                ? emailAccountRepository.findByUserId(userId, pageable)
                : emailAccountRepository.findByUserIdAndStatus(userId, status, pageable);

        List<EmailAccountResponse> content = emailAccountPage.getContent()
                .stream()
                .map(EmailAccountResponse::from)
                .toList();

        return new EmailAccountPageResponse(
                content,
                emailAccountPage.getNumber(),
                emailAccountPage.getSize(),
                emailAccountPage.getTotalElements(),
                emailAccountPage.getTotalPages(),
                emailAccountPage.isLast()
        );
    }

    public EmailAccountResponse getEmailAccount(Long userId, Long emailAccountId) {
        return EmailAccountResponse.from(getEmailAccountEntity(userId, emailAccountId));
    }

    public EmailAccountResponse updateEmailAccount(Long userId, Long emailAccountId, UpdateEmailAccountRequest request) {
        EmailAccount emailAccount = getEmailAccountEntity(userId, emailAccountId);

        validateEmailAccount(
                request.getSenderEmail(),
                request.getProvider(),
                request.getAuthType(),
                request.getEncryptedAppPassword(),
                request.getMetadata()
        );

        if (!emailAccount.getSenderEmail().equals(request.getSenderEmail())
                && emailAccountRepository.existsByUserIdAndSenderEmail(userId, request.getSenderEmail())) {
            throw new DuplicateEmailAccountException("Email account already exists for this user.");
        }

        emailAccount.setSenderEmail(request.getSenderEmail());
        emailAccount.setProvider(request.getProvider());
        emailAccount.setAuthType(request.getAuthType());
        emailAccount.setEncryptedAppPassword(request.getEncryptedAppPassword());
        emailAccount.setStatus(request.getStatus());
        emailAccount.setMetadata(request.getMetadata());
        emailAccount.setUpdatedAt(LocalDateTime.now());

        return EmailAccountResponse.from(emailAccountRepository.save(emailAccount));
    }

    public void deleteEmailAccount(Long userId, Long emailAccountId) {
        EmailAccount emailAccount = getEmailAccountEntity(userId, emailAccountId);
        emailAccountRepository.delete(emailAccount);
    }

    private EmailAccount getEmailAccountEntity(Long userId, Long emailAccountId) {
        return emailAccountRepository.findByIdAndUserId(emailAccountId, userId)
                .orElseThrow(() -> new EmailAccountNotFoundException("Email account not found."));
    }

    private int normalizePageSize(int size) {
        if (size < 1) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void validateEmailAccount(
            String senderEmail,
            EmailProvider provider,
            EmailAuthType authType,
            String encryptedAppPassword,
            String metadata
    ) {
        validateRequiredFields(senderEmail, provider, authType);
        validateProviderDomain(senderEmail, provider);
        validateAuthTypeFields(authType, encryptedAppPassword, metadata);
    }

    private void validateRequiredFields(String senderEmail, EmailProvider provider, EmailAuthType authType) {
        if (isBlank(senderEmail) || !EMAIL_PATTERN.matcher(senderEmail).matches()) {
            throw new InvalidEmailAccountException("Please provide a valid sender email.");
        }

        if (provider == null) {
            throw new InvalidEmailAccountException("Email provider is required.");
        }

        if (authType == null) {
            throw new InvalidEmailAccountException("Authentication type is required.");
        }
    }

    private void validateProviderDomain(String senderEmail, EmailProvider provider) {
        String domain = senderEmail.substring(senderEmail.lastIndexOf("@") + 1).toLowerCase(Locale.ROOT);

        if (provider == EmailProvider.GMAIL && !domain.equals("gmail.com")) {
            throw new InvalidEmailAccountException("GMAIL provider requires a gmail.com sender email.");
        }

        if (provider == EmailProvider.OUTLOOK
                && !List.of("outlook.com", "hotmail.com", "live.com").contains(domain)) {
            throw new InvalidEmailAccountException("OUTLOOK provider requires an outlook.com, hotmail.com, or live.com sender email.");
        }
    }

    private void validateAuthTypeFields(EmailAuthType authType, String encryptedAppPassword, String metadata) {
        if (authType == EmailAuthType.APP_PASSWORD && isBlank(encryptedAppPassword)) {
            throw new InvalidEmailAccountException("Encrypted app password is required for APP_PASSWORD authentication.");
        }

        if (authType == EmailAuthType.OAUTH && isBlank(metadata)) {
            throw new InvalidEmailAccountException("OAuth metadata is required for OAUTH authentication.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
