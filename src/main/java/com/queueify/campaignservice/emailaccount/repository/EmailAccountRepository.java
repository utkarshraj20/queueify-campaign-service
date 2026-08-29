package com.queueify.campaignservice.emailaccount.repository;

import com.queueify.campaignservice.emailaccount.entity.EmailAccount;
import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailAccountRepository extends JpaRepository<EmailAccount, Long> {

    List<EmailAccount> findByUserId(Long userId);

    List<EmailAccount> findByUserIdAndStatus(Long userId, EmailAccountStatus status);

    Page<EmailAccount> findByUserId(Long userId, Pageable pageable);

    Page<EmailAccount> findByUserIdAndStatus(Long userId, EmailAccountStatus status, Pageable pageable);

    Optional<EmailAccount> findByIdAndUserId(Long id, Long userId);

    Optional<EmailAccount> findByUserIdAndSenderEmail(Long userId, String senderEmail);

    boolean existsByUserIdAndSenderEmail(Long userId, String senderEmail);
}
