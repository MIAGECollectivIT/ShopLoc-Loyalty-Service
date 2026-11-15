package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@DisplayName("Web Adapter - TransactionEventWebhookController Integration Test (SLE-45 / SLE-201)")
class TransactionEventWebhookControllerTest {

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
        .claim("role", role)
        .signWith(signingKey)
        .compact();
  }

  @Test
  @DisplayName(
      "Should re-evaluate VFP status and return 200 OK on transaction approved notification")
  void shouldReevaluateVfpOnTransactionApproved() throws Exception {
    String userId = "USER-TRANSACTION-1";
    String token = generateToken("CLIENT", userId);
    Instant now = Instant.parse("2026-11-14T15:00:00Z");

    UserVfpStatus updatedStatus = new UserVfpStatus(userId, true, now, null, now);
    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(userId, 10, now.minusSeconds(86400 * 15), now);

    when(evaluateAndApplyVfpStatusUseCase.evaluateAndApply(userId)).thenReturn(updatedStatus);
    when(evaluateUserActivityUseCase.evaluateActivity(userId)).thenReturn(evaluation);

    String payload =
        """
        {
          "userId": "%s",
          "transactionId": "TXN-999",
          "amount": 29.90,
          "status": "TRANSACTION_APPROVED"
        }
        """
            .formatted(userId);

    mockMvc
        .perform(
            post("/v1/loyalty/events/transaction-approved")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.isVfp").value(true))
        .andExpect(jsonPath("$.currentTransactionCount").value(10))
        .andExpect(jsonPath("$.threshold").value(10));

    verify(evaluateAndApplyVfpStatusUseCase).evaluateAndApply(userId);
  }

  @Test
  @DisplayName("Should return 400 Bad Request when userId is blank")
  void shouldReturnBadRequestWhenUserIdBlank() throws Exception {
    String token = generateToken("CLIENT", "USER-1");
    String payload =
        """
        {
          "userId": "   ",
          "transactionId": "TXN-999"
        }
        """;

    mockMvc
        .perform(
            post("/v1/loyalty/events/transaction-approved")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }
}
