package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.collectivit.loyaltyservice.domain.exception.SalesServiceUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateUserActivityUseCase;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
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
@DisplayName("Web Adapter - UserActivityEvaluationController Integration Test (SLE-163)")
class UserActivityEvaluationControllerTest {

  private static final String TEST_SECRET =
      "dGhpc2lzYXZlcnlzZWNyZXRqd3R1c2Vyc3NlcnZpY2VrZXkxMjM0NTY=";

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private EvaluateUserActivityUseCase evaluateUserActivityUseCase;

  private MockMvc mockMvc;
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
    signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  private String generateToken(String role, String userId, Long dbId) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(userId)
        .issuer("shoploc-auth-service")
        .issuedAt(new Date(now))
        .expiration(new Date(now + 3600000))
        .claim("userId", userId)
        .claim("dbId", dbId)
        .claim("email", userId + "@example.com")
        .claim("role", role)
        .signWith(signingKey)
        .compact();
  }

  @Test
  @DisplayName("SLE-163: Missing token should return 401 Unauthorized")
  void testGetActivityEvaluationWithoutToken() throws Exception {
    mockMvc
        .perform(get("/v1/loyalty/users/USER-100/activity-evaluation"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("SLE-163: Client querying another user's activity should return 403 Forbidden")
  void testGetActivityEvaluationForbiddenForOtherUser() throws Exception {
    String token = generateToken("CLIENT", "USER-100", 100L);

    mockMvc
        .perform(
            get("/v1/loyalty/users/USER-999/activity-evaluation")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(
            jsonPath("$.message")
                .value("Access denied: You can only view your own activity evaluation."));
  }

  @Test
  @DisplayName("SLE-163: Client querying own activity should return 200 OK and evaluation data")
  void testGetActivityEvaluationNominalClient() throws Exception {
    String userId = "USER-100";
    String token = generateToken("CLIENT", userId, 100L);

    Instant now = Instant.now();
    Instant windowStart = now.minus(15, ChronoUnit.DAYS);

    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(userId, 12, windowStart, now);

    when(evaluateUserActivityUseCase.evaluateActivity(eq(userId))).thenReturn(evaluation);

    mockMvc
        .perform(
            get("/v1/loyalty/users/" + userId + "/activity-evaluation")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.transactionCount").value(12))
        .andExpect(jsonPath("$.threshold").value(10))
        .andExpect(jsonPath("$.isEligibleVfp").value(true))
        .andExpect(jsonPath("$.windowStart").exists())
        .andExpect(jsonPath("$.windowEnd").exists());

    // Also verify alias /api/v1/loyalty/users/...
    mockMvc
        .perform(
            get("/api/v1/loyalty/users/" + userId + "/activity-evaluation")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.isEligibleVfp").value(true));
  }

  @Test
  @DisplayName("SLE-163: Admin can query any user's activity evaluation")
  void testGetActivityEvaluationAdminAccess() throws Exception {
    String targetUserId = "USER-TARGET";
    String adminToken = generateToken("ADMIN", "ADMIN-USER", 1L);

    Instant now = Instant.now();
    Instant windowStart = now.minus(15, ChronoUnit.DAYS);

    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(targetUserId, 5, windowStart, now);

    when(evaluateUserActivityUseCase.evaluateActivity(eq(targetUserId))).thenReturn(evaluation);

    mockMvc
        .perform(
            get("/v1/loyalty/users/" + targetUserId + "/activity-evaluation")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(targetUserId))
        .andExpect(jsonPath("$.transactionCount").value(5))
        .andExpect(jsonPath("$.threshold").value(10))
        .andExpect(jsonPath("$.isEligibleVfp").value(false));
  }

  @Test
  @DisplayName("SLE-163: Remote sales service unavailable should return 502 Bad Gateway")
  void testGetActivityEvaluationSalesUnavailable() throws Exception {
    String userId = "USER-100";
    String token = generateToken("CLIENT", userId, 100L);

    when(evaluateUserActivityUseCase.evaluateActivity(eq(userId)))
        .thenThrow(new SalesServiceUnavailableException("Sales service down"));

    mockMvc
        .perform(
            get("/v1/loyalty/users/" + userId + "/activity-evaluation")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isBadGateway())
        .andExpect(jsonPath("$.status").value(502))
        .andExpect(jsonPath("$.message").value("Sales service down"));
  }
}
