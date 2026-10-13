package com.queueify.campaignservice.integration;

import com.queueify.campaignservice.authentication.jwt.JwtService;
import com.queueify.campaignservice.authentication.entity.User;
import com.queueify.campaignservice.authentication.repository.UserRepository;
import com.queueify.campaignservice.authentication.service.AuthService;
import com.queueify.campaignservice.authentication.service.RefreshTokenService;
import com.queueify.campaignservice.campaign.service.CampaignService;
import com.queueify.campaignservice.campaign.service.CampaignFileCreationService;
import com.queueify.campaignservice.campaign.dto.CreateCampaignWithFilesRequest;
import com.queueify.campaignservice.campaign.dto.CampaignResponse;
import com.queueify.campaignservice.campaign.entity.CampaignStatus;
import com.queueify.campaignservice.emailaccount.service.EmailAccountService;
import com.queueify.campaignservice.storage.CampaignAssetUploadService;
import com.queueify.campaignservice.storage.CampaignAssetType;
import com.queueify.campaignservice.storage.StoredFile;
import com.queueify.campaignservice.storage.InvalidUploadException;
import com.queueify.campaignservice.storage.UploadTooLargeException;
import com.queueify.campaignservice.storage.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.mockito.Mockito.*;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {
                "spring.autoconfigure.exclude="
                        + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
                        + "org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration",
                "management.health.mail.enabled=false"
        }
)
@AutoConfigureMockMvc
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private RefreshTokenService refreshTokenService;

    @MockitoBean
    private EmailAccountService emailAccountService;

    @MockitoBean
    private CampaignService campaignService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private CampaignAssetUploadService uploadService;

    @MockitoBean
    private CampaignFileCreationService fileCreationService;

    @Test
    void createsCampaignFromJsonDetailsAndFiles() throws Exception {
        String token = assetOwnerToken();
        var now = java.time.LocalDateTime.now();
        var response = new CampaignResponse(10L, "Launch", 2L, "Hello", "s3://bucket/template", "s3://bucket/csv",
                1, 0, 0, CampaignStatus.DRAFT, 5L, null, null, now, now);
        when(fileCreationService.create(eq(2L), any(), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/users/2/campaigns")
                        .file(campaignPart(1))
                        .file(new MockMultipartFile("template", "welcome.html", "text/html", "Hello".getBytes()))
                        .file(new MockMultipartFile("recipients", "leads.csv", "text/csv", "email".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.csvUrl").value("s3://bucket/csv"))
                .andExpect(jsonPath("$.templateUrl").value("s3://bucket/template"));
        verify(fileCreationService).create(eq(2L),
                argThat((CreateCampaignWithFilesRequest request) -> request.getCampaignName().equals("Launch")
                        && request.getSenderEmailAccountId().equals(5L)),
                argThat(file -> file.getOriginalFilename().equals("welcome.html")),
                argThat(file -> file.getOriginalFilename().equals("leads.csv")));
    }

    @Test
    void rejectsMultipartCampaignForAnotherUser() throws Exception {
        String token = assetOwnerToken();
        mockMvc.perform(multipart("/api/v1/users/1/campaigns").file(campaignPart(1))
                        .file(new MockMultipartFile("template", "welcome.html", "text/html", "Hello".getBytes()))
                        .file(new MockMultipartFile("recipients", "leads.csv", "text/csv", "email".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(fileCreationService);
    }

    @Test
    void rejectsMissingCsvAndInvalidMetadataBeforeCreation() throws Exception {
        String token = assetOwnerToken();
        mockMvc.perform(multipart("/api/v1/users/2/campaigns").file(campaignPart(1))
                        .file(new MockMultipartFile("template", "welcome.html", "text/html", "Hello".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(multipart("/api/v1/users/2/campaigns").file(campaignPart(0))
                        .file(new MockMultipartFile("template", "welcome.html", "text/html", "Hello".getBytes()))
                        .file(new MockMultipartFile("recipients", "leads.csv", "text/csv", "email".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(fileCreationService);
    }

    private MockMultipartFile campaignPart(int count) {
        String json = "{\"campaignName\":\"Launch\",\"subject\":\"Hello\",\"totalEmails\":" + count
                + ",\"senderEmailAccountId\":5}";
        return new MockMultipartFile("campaign", "campaign.json", "application/json", json.getBytes());
    }

    @ParameterizedTest
    @ValueSource(strings = {"template", "recipients"})
    void uploadsAssetsForAuthenticatedOwner(String kind) throws Exception {
        String token = assetOwnerToken();
        CampaignAssetType type = kind.equals("template") ? CampaignAssetType.TEMPLATE : CampaignAssetType.RECIPIENTS;
        StoredFile stored = new StoredFile("bucket", "campaign-assets/users/2/uploads/file", "text/plain", 5);
        when(uploadService.upload(eq(2L), eq(type), any())).thenReturn(stored);

        mockMvc.perform(multipart("/api/v1/users/2/campaign-assets/" + kind)
                        .file(new MockMultipartFile("file", "sample.txt", "text/plain", "Hello".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.uri").value(stored.getUri()))
                .andExpect(jsonPath("$.size").value(5));
        verify(uploadService).upload(eq(2L), eq(type), any());
    }

    @Test
    void rejectsAnonymousAssetUpload() throws Exception {
        mockMvc.perform(multipart("/api/v1/users/2/campaign-assets/template")
                        .file(new MockMultipartFile("file", "sample.html", "text/html", "Hello".getBytes())))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(uploadService);
    }

    @Test
    void rejectsAssetUploadForAnotherUser() throws Exception {
        String token = assetOwnerToken();
        mockMvc.perform(multipart("/api/v1/users/1/campaign-assets/recipients")
                        .file(new MockMultipartFile("file", "sample.csv", "text/csv", "email".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        verifyNoInteractions(uploadService);
    }

    @Test
    void rejectsMissingUploadPart() throws Exception {
        mockMvc.perform(multipart("/api/v1/users/2/campaign-assets/template")
                        .header("Authorization", "Bearer " + assetOwnerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Select a file to upload."));
        verifyNoInteractions(uploadService);
    }

    @Test
    void returnsReadableUploadValidationAndSizeErrors() throws Exception {
        String token = assetOwnerToken();
        when(uploadService.upload(eq(2L), eq(CampaignAssetType.TEMPLATE), any()))
                .thenThrow(new InvalidUploadException("Select HTML or TXT."))
                .thenThrow(new UploadTooLargeException("Template too large."));
        var file = new MockMultipartFile("file", "sample.html", "text/html", "Hello".getBytes());
        mockMvc.perform(multipart("/api/v1/users/2/campaign-assets/template").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Select HTML or TXT."));
        mockMvc.perform(multipart("/api/v1/users/2/campaign-assets/template").file(file)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.message").value("Template too large."));
    }

    @Test
    void returnsStorageFailureWithoutLeakingAwsDetails() throws Exception {
        String token = assetOwnerToken();
        when(uploadService.upload(eq(2L), eq(CampaignAssetType.TEMPLATE), any()))
                .thenThrow(new StorageException("Private AWS configuration", new RuntimeException("private")));
        mockMvc.perform(multipart("/api/v1/users/2/campaign-assets/template")
                        .file(new MockMultipartFile("file", "sample.html", "text/html", "Hello".getBytes()))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("File storage is unavailable. Please try again."));
    }

    private String assetOwnerToken() {
        User user = new User();
        user.setId(2L);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        return jwtService.generateAccessToken("owner@example.com");
    }

    @Test
    void rejectsAnonymousCurrentUserRequest() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(userRepository);
    }

    @Test
    void returnsAuthenticatedUsersPublicProfile() throws Exception {
        User user = new User();
        user.setId(2L);
        user.setName("Queueify User");
        user.setEmail("owner@example.com");
        user.setPasswordHash("must-not-be-exposed");
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken("owner@example.com");

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Queueify User"))
                .andExpect(jsonPath("$.email").value("owner@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void rejectsCurrentUserWhoseAccountWasDeleted() throws Exception {
        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.empty());
        String token = jwtService.generateAccessToken("deleted@example.com");

        mockMvc.perform(get("/api/v1/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsAnonymousCampaignStart() throws Exception {
        mockMvc.perform(post("/api/v1/users/1/campaigns/10/start"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(campaignService);
    }

    @Test
    void acceptsCampaignStartForJwtOwner() throws Exception {
        User user = new User();
        user.setId(1L);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken("owner@example.com");

        mockMvc.perform(post("/api/v1/users/1/campaigns/10/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        verify(campaignService).startCampaign(1L, 10L);
    }

    @Test
    void rejectsCampaignStartForAnotherUsersPath() throws Exception {
        User user = new User();
        user.setId(2L);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        String token = jwtService.generateAccessToken("owner@example.com");

        mockMvc.perform(post("/api/v1/users/1/campaigns/10/start")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        verifyNoInteractions(campaignService);
    }

    @Test
    void allowsAnonymousAccessToLoginEndpoint() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allowsAnonymousAccessToHealthEndpoint() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnonymousAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/email-accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsMalformedBearerTokenAtProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/email-accounts")
                        .header("Authorization", "Bearer malformed-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsValidAccessTokenWithoutCreatingSession() throws Exception {
        String accessToken = jwtService.generateAccessToken("user@example.com");

        mockMvc.perform(get("/api/v1/users/1/email-accounts")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(cookie().doesNotExist("JSESSIONID"));
    }
}
