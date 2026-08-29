package com.queueify.campaignservice.emailaccount.service;

import com.queueify.campaignservice.emailaccount.dto.CreateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountPageResponse;
import com.queueify.campaignservice.emailaccount.dto.EmailAccountResponse;
import com.queueify.campaignservice.emailaccount.dto.UpdateEmailAccountRequest;
import com.queueify.campaignservice.emailaccount.entity.EmailAccount;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.exception.DuplicateEmailAccountException;
import com.queueify.campaignservice.emailaccount.exception.EmailAccountNotFoundException;
import com.queueify.campaignservice.emailaccount.repository.EmailAccountRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmailAccountService {

    private static final int MAX_PAGE_SIZE = 100;

    private final EmailAccountRepository emailAccountRepository;

    public EmailAccountService(EmailAccountRepository emailAccountRepository) {
        this.emailAccountRepository = emailAccountRepository;
    }

    public EmailAccountResponse createEmailAccount(Long userId, CreateEmailAccountRequest request) {
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
}
