package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpProgress;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserVfpProgressQuery;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@DisplayName("Web Adapter - UserVfpProgressController Integration Test (SLE-51 / SLE-238)")
class UserVfpProgressControllerTest {

  private static final String TEST_SECRET =
      "dGhpc2lzYXZlcnlzZWNyZXRqd3R1c2Vyc3NlcnZpY2VrZXkxMjM0NTY=";

  @Autowired private WebApplicationContext webApplicationContext;

  @MockitoBean private GetUserVfpProgressQuery getUserVfpProgressQuery;

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
        .claim("email", userId + "@shoploc.fr")
        .claim("role", role)
        .signWith(signingKey)
        .compact();
  }

  @Test
  @DisplayName("Should return 200 OK with VFP progress for self-requesting client")
  void shouldReturnProgressForSelfUser() throws Exception {
    String userId = "USER-123";
    String token = generateToken("CLIENT", userId, 1L);
    Instant now = Instant.parse("2026-11-16T12:00:00Z");

    VfpProgress progress =
        new VfpProgress(
            userId,
            true,
            10,
            10,
            0,
            1,
            2L,
            14L,
            now.plusSeconds(86400 * 2),
            1,
            now.minusSeconds(86400 * 15),
            now,
            now);

    when(getUserVfpProgressQuery.getProgress(userId)).thenReturn(progress);

    mockMvc
        .perform(
            get("/v1/loyalty/users/{userId}/vfp-progress", userId)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(userId))
        .andExpect(jsonPath("$.isVfp").value(true))
        .andExpect(jsonPath("$.currentTransactionCount").value(10))
        .andExpect(jsonPath("$.threshold").value(10))
        .andExpect(jsonPath("$.remainingTransactionsToMaintain").value(1))
        .andExpect(jsonPath("$.daysRemainingBeforeRevocation").value(2))
        .andExpect(jsonPath("$.estimatedRevocationDate").exists());
  }

  @Test
  @DisplayName(
      "Should return 403 Forbidden when client attempts BOLA access to another user's progress")
  void shouldDenyAccessToOtherUser() throws Exception {
    String token = generateToken("CLIENT", "USER-ME", 1L);

    mockMvc
        .perform(
            get("/v1/loyalty/users/{userId}/vfp-progress", "USER-OTHER")
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("Should return 200 OK when admin queries any user's progress")
  void shouldAllowAdminAccess() throws Exception {
    String targetUserId = "USER-CITIZEN";
    String adminToken = generateToken("ADMIN", "ADMIN-USER", 99L);
    Instant now = Instant.parse("2026-11-16T12:00:00Z");

    VfpProgress progress =
        new VfpProgress(
            targetUserId,
            false,
            4,
            10,
            6,
            0,
            null,
            null,
            null,
            0,
            now.minusSeconds(86400 * 15),
            now,
            now);

    when(getUserVfpProgressQuery.getProgress(targetUserId)).thenReturn(progress);

    mockMvc
        .perform(
            get("/v1/loyalty/users/{userId}/vfp-progress", targetUserId)
                .header("Authorization", "Bearer " + adminToken))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.userId").value(targetUserId))
        .andExpect(jsonPath("$.isVfp").value(false))
        .andExpect(jsonPath("$.remainingTransactionsToQualify").value(6));
  }

  @Test
  @DisplayName("Should return 401 Unauthorized when authorization token is missing")
  void shouldReturn401WhenNoToken() throws Exception {
    mockMvc
        .perform(get("/v1/loyalty/users/{userId}/vfp-progress", "USER-123"))
        .andExpect(status().isUnauthorized());
  }
}
