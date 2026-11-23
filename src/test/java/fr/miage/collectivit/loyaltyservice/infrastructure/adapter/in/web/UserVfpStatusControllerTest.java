package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateUserActivityUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserVfpStatusQuery;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@DisplayName("Web Adapter - UserVfpStatusController Integration Test (SLE-45 / SLE-203)")
class UserVfpStatusControllerTest {

  private static final String TEST_SECRET =
      "dGhpc2lzYXZlcnlzZWNyZXRqd3R1c2Vyc3NlcnZpY2VrZXkxMjM0NTY=";

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private GetUserVfpStatusQuery getUserVfpStatusQuery;
  @MockitoBean private EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase;
  @MockitoBean private EvaluateUserActivityUseCase evaluateUserActivityUseCase;

  private MockMvc mockMvc;
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
    signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  private String generateToken(String role, String userId) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(userId)
        .issuer("shoploc-auth-service")
        .issuedAt(new Date(now))
        .expiration(new Date(now + 3600000))
        .claim("userId", userId)
        .claim("email", userId + "@shoploc.fr")
        .claim("role", role)
        .signWith(signingKey)
        .compact();
  }

  @Test
  @DisplayName("Should return 200 OK with VFP status for self-requesting client")
  void shouldReturnStatusForSelfUser() throws Exception {
    String userId = "USER-123";
    String token = generateToken("CLIENT", userId);
    Instant now = Instant.parse("2026-11-14T12:00:00Z");

    UserVfpStatus persistedStatus = new UserVfpStatus(userId, true, now, null, now);
    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(userId, 12, now.minusSeconds(86400 * 15), now);

    when(getUserVfpStatusQuery.getStatus(userId)).thenReturn(Optional.of(persistedStatus));
    when(evaluateUserActivityUseCase.evaluateActivity(userId)).thenReturn(evaluation);

    mockMvc
        .perform(
            get("/v1/loyalty/users/{userId}/vfp-status", userId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.isVfp").value(true))
        .andExpect(jsonPath("$.currentTransactionCount").value(12))
        .andExpect(jsonPath("$.threshold").value(10))
        .andExpect(jsonPath("$.grantedAt").exists());
  }

  @Test
  @DisplayName("Should return 200 OK for admin requesting any user's VFP status")
  void shouldReturnStatusForAdmin() throws Exception {
    String targetUserId = "USER-TARGET";
    String adminToken = generateToken("ADMIN", "admin-user");
    Instant now = Instant.parse("2026-11-14T12:00:00Z");

    UserVfpStatus persistedStatus = new UserVfpStatus(targetUserId, false, null, null, now);
    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(targetUserId, 5, now.minusSeconds(86400 * 15), now);

    when(getUserVfpStatusQuery.getStatus(targetUserId)).thenReturn(Optional.of(persistedStatus));
    when(evaluateUserActivityUseCase.evaluateActivity(targetUserId)).thenReturn(evaluation);

    mockMvc
        .perform(
            get("/v1/loyalty/users/{userId}/vfp-status", targetUserId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(targetUserId))
        .andExpect(jsonPath("$.isVfp").value(false))
        .andExpect(jsonPath("$.currentTransactionCount").value(5));
  }

  @Test
  @DisplayName(
      "Should return 403 Forbidden when client attempts to access another user's VFP status")
  void shouldReturnForbiddenWhenAccessingOtherUser() throws Exception {
    String token = generateToken("CLIENT", "USER-CLIENT-A");

    mockMvc
        .perform(
            get("/v1/loyalty/users/{userId}/vfp-status", "USER-CLIENT-B")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(
            jsonPath("$.message").value("Access denied: You can only view your own VFP status."));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when Authorization header is absent")
  void shouldReturnUnauthorizedWhenNoToken() throws Exception {
    mockMvc
        .perform(get("/v1/loyalty/users/USER-123/vfp-status"))
        .andExpect(status().isUnauthorized());
  }
}
