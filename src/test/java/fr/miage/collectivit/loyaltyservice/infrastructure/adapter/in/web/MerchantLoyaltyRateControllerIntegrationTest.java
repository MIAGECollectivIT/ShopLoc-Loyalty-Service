package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.MerchantLoyaltyRateRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@DisplayName("Web Adapter - MerchantLoyaltyRateController Integration Test")
class MerchantLoyaltyRateControllerIntegrationTest {

  private static final String TEST_SECRET =
      "dGhpc2lzYXZlcnlzZWNyZXRqd3R1c2Vyc3NlcnZpY2VrZXkxMjM0NTY=";

  @Autowired private WebApplicationContext webApplicationContext;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean private StoreVerificationPort storeVerificationPort;

  private MockMvc mockMvc;
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
    signingKey = Keys.hmacShaKeyFor(keyBytes);
  }

  private String generateToken(String role, Long dbId) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(UUID.randomUUID().toString())
        .issuer("shoploc-auth-service")
        .issuedAt(new Date(now))
        .expiration(new Date(now + 3600000))
        .claim("userId", UUID.randomUUID().toString())
        .claim("dbId", dbId)
        .claim("email", role.toLowerCase() + "@shoploc.fr")
        .claim("role", role)
        .signWith(signingKey)
        .compact();
  }

  @Test
  @DisplayName(
      "GET /v1/loyalty/merchants/{storeId}/rates sans token -> doit être autorisé et retourner le barème par défaut (1€ = 1pt)")
  void shouldAllowPublicGetDefaultRate() throws Exception {
    mockMvc
        .perform(get("/v1/loyalty/merchants/100/rates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeId").value(100))
        .andExpect(jsonPath("$.euroAmount").value(1.00))
        .andExpect(jsonPath("$.pointsReward").value(1));
  }

  @Test
  @DisplayName("PUT sans token -> doit renvoyer 401 Unauthorized")
  void shouldDenyPutWithoutToken() throws Exception {
    MerchantLoyaltyRateRequest request = new MerchantLoyaltyRateRequest(new BigDecimal("2.50"), 5);

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/10/rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("PUT avec rôle CLIENT -> doit renvoyer 403 Forbidden")
  void shouldDenyPutWithClientRole() throws Exception {
    String clientToken = generateToken("CLIENT", 1L);
    MerchantLoyaltyRateRequest request = new MerchantLoyaltyRateRequest(new BigDecimal("2.50"), 5);

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/10/rates")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PUT avec payload invalide (euro <= 0) -> doit renvoyer 400 Bad Request")
  void shouldReturnBadRequestForInvalidPayload() throws Exception {
    String merchantToken = generateToken("MERCHANT", 42L);
    MerchantLoyaltyRateRequest invalidRequest =
        new MerchantLoyaltyRateRequest(new BigDecimal("0.00"), 0);

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/10/rates")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName(
      "PUT avec commerçant qui n'est pas propriétaire -> doit renvoyer 403 Forbidden (BOLA)")
  void shouldDenyPutWhenMerchantDoesNotOwnStore() throws Exception {
    Long storeId = 10L;
    Long otherMerchantId = 99L;
    when(storeVerificationPort.storeExists(storeId)).thenReturn(true);
    when(storeVerificationPort.isStoreOwnedBy(storeId, otherMerchantId)).thenReturn(false);

    String token = generateToken("MERCHANT", otherMerchantId);
    MerchantLoyaltyRateRequest request = new MerchantLoyaltyRateRequest(new BigDecimal("2.00"), 3);

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/" + storeId + "/rates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(
            jsonPath("$.message").value(org.hamcrest.Matchers.containsString("propriétaire")));
  }

  @Test
  @DisplayName("PUT avec commerçant propriétaire légitime -> doit configurer et retourner 200 OK")
  void shouldAllowStoreOwnerToConfigureRate() throws Exception {
    Long storeId = 25L;
    Long ownerId = 42L;
    when(storeVerificationPort.storeExists(storeId)).thenReturn(true);
    when(storeVerificationPort.isStoreOwnedBy(storeId, ownerId)).thenReturn(true);

    String token = generateToken("MERCHANT", ownerId);
    MerchantLoyaltyRateRequest request = new MerchantLoyaltyRateRequest(new BigDecimal("5.00"), 10);

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/" + storeId + "/rates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeId").value(storeId))
        .andExpect(jsonPath("$.euroAmount").value(5.00))
        .andExpect(jsonPath("$.pointsReward").value(10));

    // Vérifier que le GET public retourne ensuite la nouvelle configuration
    mockMvc
        .perform(get("/v1/loyalty/merchants/" + storeId + "/rates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.euroAmount").value(5.00))
        .andExpect(jsonPath("$.pointsReward").value(10));
  }

  @Test
  @DisplayName("PUT en mode ADMIN -> doit réussir sans restriction de propriétaire")
  void shouldAllowAdminToConfigureRate() throws Exception {
    Long storeId = 30L;
    String adminToken = generateToken("ADMIN", 1L);
    MerchantLoyaltyRateRequest request =
        new MerchantLoyaltyRateRequest(new BigDecimal("10.00"), 20);

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/" + storeId + "/rates")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeId").value(storeId))
        .andExpect(jsonPath("$.euroAmount").value(10.00))
        .andExpect(jsonPath("$.pointsReward").value(20));
  }
}
