package com.queueify.campaignservice.emailaccount.dto;

import com.queueify.campaignservice.emailaccount.entity.EmailAccountStatus;
import com.queueify.campaignservice.emailaccount.entity.EmailAuthType;
import com.queueify.campaignservice.emailaccount.entity.EmailProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UpdateEmailAccountRequest {

    @NotBlank(message = "Sender email is required")
    @Email(message = "Please provide a valid sender email")
    private String senderEmail;

    @NotNull(message = "Email provider is required")
    private EmailProvider provider;

    @NotNull(message = "Authentication type is required")
    private EmailAuthType authType;

    @NotBlank(message = "Encrypted app password is required")
    @Size(max = 512, message = "Encrypted app password must not exceed 512 characters")
    private String encryptedAppPassword;

    @NotNull(message = "Email account status is required")
    private EmailAccountStatus status;

    private String metadata;

    public UpdateEmailAccountRequest(
            String senderEmail,
            EmailProvider provider,
            EmailAuthType authType,
            String encryptedAppPassword,
            EmailAccountStatus status,
            String metadata
    ) {
        this.senderEmail = senderEmail;
        this.provider = provider;
        this.authType = authType;
        this.encryptedAppPassword = encryptedAppPassword;
        this.status = status;
        this.metadata = metadata;
    }
}
