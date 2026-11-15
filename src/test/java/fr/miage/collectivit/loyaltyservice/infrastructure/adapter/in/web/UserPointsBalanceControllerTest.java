package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsBalance;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserPointsBalanceQuery;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
@DisplayName("Web Adapter - UserPointsBalanceController Integration Tests (SLE-38 / SLE-176)")
class UserPointsBalanceControllerTest {

  private static final String TEST_SECRET =
      "dGhpc2lzYXZlcnlzZWNyZXRqd3R1c2Vyc3NlcnZpY2VrZXkxMjM0NTY=";

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private GetUserPointsBalanceQuery getUserPointsBalanceQuery;

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
    var builder =
        Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(userId != null ? userId : String.valueOf(dbId))
            .issuer("shoploc-auth-service")
            .issuedAt(new Date(now))
            .expiration(new Date(now + 3600000))
            .claim("dbId", dbId)
            .claim("email", "client@shoploc.fr")
            .claim("role", role);
    if (userId != null) {
      builder.claim("userId", userId);
    }
    return builder.signWith(signingKey).compact();
  }

  @Test
  @DisplayName("SLE-176: Missing Bearer token should return 401 Unauthorized")
  void shouldReturn401WhenTokenMissing() throws Exception {
    mockMvc.perform(get("/v1/loyalty/balance")).andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/v1/loyalty/users/user-uuid-1/balance"))
        .andExpect(status().isUnauthorized());
    mockMvc
        .perform(get("/v1/loyalty/customers/user-uuid-1/points"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("SLE-176: Authenticated client querying own /balance should return 200 OK")
  void shouldReturnOwnBalanceOnSelfEndpoint() throws Exception {
    String userId = "550e8400-e29b-41d4-a716-446655440000";
    Instant now = Instant.parse("2026-11-15T10:30:00Z");
    PointsTransaction earnTx =
        new PointsTransaction(
            1L,
            "PTS-EARN-100",
            userId,
            10L,
            "Librairie du Quartier",
            PointsTransactionType.EARN,
            new BigDecimal("15.0"),
            new BigDecimal("125.0"),
            "PURCHASE",
            "Achat livre",
            now);

    when(getUserPointsBalanceQuery.getUserPointsBalance(userId))
        .thenReturn(
            new UserPointsBalance(
                userId, "CL95001", new BigDecimal("125.0"), now, List.of(earnTx)));

    String token = generateToken("CLIENT", userId, 100L);

    mockMvc
        .perform(get("/v1/loyalty/balance").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.clientCode").value("CL95001"))
        .andExpect(jsonPath("$.balance").value(125.0))
        .andExpect(jsonPath("$.points").value(125.0))
        .andExpect(jsonPath("$.transactions.length()").value(1))
        .andExpect(jsonPath("$.transactions[0].transactionReference").value("PTS-EARN-100"))
        .andExpect(jsonPath("$.transactions[0].type").value("EARN"))
        .andExpect(jsonPath("$.transactions[0].points").value(15.0));
  }

  @Test
  @DisplayName(
      "SLE-176: Client querying own /users/{userId}/balance without history should return 0.0")
  void shouldReturnZeroBalanceWhenNoHistoryExists() throws Exception {
    String userId = "user-uuid-empty";
    Instant now = Instant.parse("2026-11-15T11:00:00Z");
    when(getUserPointsBalanceQuery.getUserPointsBalance(userId))
        .thenReturn(
            new UserPointsBalance(userId, null, BigDecimal.ZERO, now, Collections.emptyList()));

    String token = generateToken("CLIENT", userId, 42L);

    mockMvc
        .perform(
            get("/api/v1/loyalty/users/" + userId + "/balance")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.balance").value(0.0))
        .andExpect(jsonPath("$.points").value(0.0))
        .andExpect(jsonPath("$.transactions").isEmpty());
  }

  @Test
  @DisplayName("SLE-176: Client querying another user's balance or points should return 403 BOLA")
  void shouldReturn403ForCrossUserAccess() throws Exception {
    String token = generateToken("CLIENT", "user-uuid-100", 100L);

    mockMvc
        .perform(
            get("/v1/loyalty/users/user-uuid-999/balance")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));

    mockMvc
        .perform(
            get("/v1/loyalty/customers/user-uuid-999/points")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @DisplayName("SLE-176: Admin querying another customer's /customers/{customerId}/points gets 200")
  void shouldAllowAdminToReadCustomerPoints() throws Exception {
    String targetUserId = "user-uuid-555";
    Instant now = Instant.parse("2026-11-15T11:00:00Z");
    when(getUserPointsBalanceQuery.getUserPointsBalance(targetUserId))
        .thenReturn(
            new UserPointsBalance(
                targetUserId, "CL95555", new BigDecimal("80.0"), now, Collections.emptyList()));

    String adminToken = generateToken("ADMIN", "admin-uuid-1", 1L);

    mockMvc
        .perform(
            get("/v1/loyalty/customers/" + targetUserId + "/points")
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(targetUserId))
        .andExpect(jsonPath("$.points").value(80.0))
        .andExpect(jsonPath("$.balance").value(80.0));
  }

  @Test
  @DisplayName("SLE-176: Client accessing via dbId fallback claim when userId claim is absent")
  void shouldResolveDbIdOnSelfBalanceWhenUserIdClaimAbsent() throws Exception {
    Instant now = Instant.parse("2026-11-15T11:00:00Z");
    when(getUserPointsBalanceQuery.getUserPointsBalance("77"))
        .thenReturn(new UserPointsBalance("77", null, new BigDecimal("10.0"), now, List.of()));

    String token = generateToken("CLIENT", null, 77L);

    mockMvc
        .perform(get("/v1/loyalty/balance").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value("77"))
        .andExpect(jsonPath("$.balance").value(10.0));
  }

  @Test
  @DisplayName(
      "SLE-176: Token missing both userId and dbId claims should return 403 Forbidden on /balance")
  void shouldReturn403WhenTokenHasNoUserIdentifiers() throws Exception {
    String token = generateToken("CLIENT", null, null);

    mockMvc
        .perform(get("/v1/loyalty/balance").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  @DisplayName("SLE-176: Unsupported role should return 403 Forbidden")
  void shouldReturn403ForUnsupportedRole() throws Exception {
    String token = generateToken("GUEST", "guest-uuid-1", 10L);

    mockMvc
        .perform(get("/v1/loyalty/balance").header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }
}
