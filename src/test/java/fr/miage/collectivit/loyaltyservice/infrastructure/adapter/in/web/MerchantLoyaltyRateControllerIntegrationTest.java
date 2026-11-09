package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.MerchantLoyaltyRateRequest;
import fr.miage.collectivit.loyaltyservice.security.JwtValidatorService;
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
  @Autowired private JwtValidatorService jwtValidatorService;
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

  private String generateToken(String role, Long dbId, String jti) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .id(jti != null ? jti : UUID.randomUUID().toString())
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

  private String generateToken(String role, Long dbId) {
    return generateToken(role, dbId, null);
  }

  @Test
  @DisplayName(
      "GET /v1/loyalty/merchants/{storeId}/rates without token -> should be allowed and return default rate (1€ = 1pt)")
  void shouldAllowPublicGetDefaultRate() throws Exception {
    mockMvc
        .perform(get("/v1/loyalty/merchants/100/rates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeId").value(100))
        .andExpect(jsonPath("$.euroAmount").value(1.00))
        .andExpect(jsonPath("$.pointsReward").value(1.0));
  }

  @Test
  @DisplayName(
      "OPTIONS /v1/loyalty/merchants/{storeId}/rates -> should be allowed for CORS preflight")
  void shouldAllowOptionsPreflightRequest() throws Exception {
    mockMvc.perform(options("/v1/loyalty/merchants/10/rates")).andExpect(status().isOk());
  }

  @Test
  @DisplayName("PUT without token -> should return 401 Unauthorized")
  void shouldDenyPutWithoutToken() throws Exception {
    MerchantLoyaltyRateRequest request =
        new MerchantLoyaltyRateRequest(new BigDecimal("2.50"), new BigDecimal("5.0"));

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/10/rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("PUT with revoked token -> should return 401 Unauthorized")
  void shouldDenyPutWithRevokedToken() throws Exception {
    String revokedJti = UUID.randomUUID().toString();
    jwtValidatorService.addRevokedJtiForTest(revokedJti);

    String token = generateToken("MERCHANT", 42L, revokedJti);
    MerchantLoyaltyRateRequest request =
        new MerchantLoyaltyRateRequest(new BigDecimal("2.50"), new BigDecimal("5.0"));

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/10/rates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("PUT with CLIENT role -> should return 403 Forbidden")
  void shouldDenyPutWithClientRole() throws Exception {
    String clientToken = generateToken("CLIENT", 1L);
    MerchantLoyaltyRateRequest request =
        new MerchantLoyaltyRateRequest(new BigDecimal("2.50"), new BigDecimal("5.0"));

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/10/rates")
                .header("Authorization", "Bearer " + clientToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("PUT with invalid payload (euro <= 0) -> should return 400 Bad Request")
  void shouldReturnBadRequestForInvalidPayload() throws Exception {
    String merchantToken = generateToken("MERCHANT", 42L);
    MerchantLoyaltyRateRequest invalidRequest =
        new MerchantLoyaltyRateRequest(new BigDecimal("0.00"), new BigDecimal("0.0"));

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
      "PUT with euro amount having more than 2 decimal places -> should return 400 Bad Request")
  void shouldRejectEuroAmountWithMoreThanTwoDecimals() throws Exception {
    String merchantToken = generateToken("MERCHANT", 42L);
    String payload = "{\"euroAmount\": 2.345, \"pointsReward\": 5}";

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/10/rates")
                .header("Authorization", "Bearer " + merchantToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT for unknown store on Store Service -> should return 404 Not Found")
  void shouldReturnNotFoundWhenStoreDoesNotExistOnStoreService() throws Exception {
    Long unknownStoreId = 888L;
    when(storeVerificationPort.storeExists(unknownStoreId)).thenReturn(false);

    String token = generateToken("MERCHANT", 42L);
    MerchantLoyaltyRateRequest request =
        new MerchantLoyaltyRateRequest(new BigDecimal("2.00"), new BigDecimal("3.0"));

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/" + unknownStoreId + "/rates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not found")));
  }

  @Test
  @DisplayName("PUT with merchant who does not own the store -> should return 403 Forbidden (BOLA)")
  void shouldDenyPutWhenMerchantDoesNotOwnStore() throws Exception {
    Long storeId = 10L;
    Long otherMerchantId = 99L;
    when(storeVerificationPort.storeExists(storeId)).thenReturn(true);
    when(storeVerificationPort.isStoreOwnedBy(storeId, otherMerchantId)).thenReturn(false);

    String token = generateToken("MERCHANT", otherMerchantId);
    MerchantLoyaltyRateRequest request =
        new MerchantLoyaltyRateRequest(new BigDecimal("2.00"), new BigDecimal("3.0"));

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/" + storeId + "/rates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("owner")));
  }

  @Test
  @DisplayName("PUT with legitimate store owner merchant -> should configure and return 200 OK")
  void shouldAllowStoreOwnerToConfigureRate() throws Exception {
    Long storeId = 25L;
    Long ownerId = 42L;
    when(storeVerificationPort.storeExists(storeId)).thenReturn(true);
    when(storeVerificationPort.isStoreOwnedBy(storeId, ownerId)).thenReturn(true);

    String token = generateToken("MERCHANT", ownerId);
    MerchantLoyaltyRateRequest request =
        new MerchantLoyaltyRateRequest(new BigDecimal("5.00"), new BigDecimal("10.0"));

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/" + storeId + "/rates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeId").value(storeId))
        .andExpect(jsonPath("$.euroAmount").value(5.00))
        .andExpect(jsonPath("$.pointsReward").value(10.0));

    // Verify that subsequent public GET returns the updated configuration
    mockMvc
        .perform(get("/v1/loyalty/merchants/" + storeId + "/rates"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.euroAmount").value(5.00))
        .andExpect(jsonPath("$.pointsReward").value(10.0));
  }

  @Test
  @DisplayName("PUT with half-points (e.g. 0.5 point) -> should succeed and be persisted")
  void shouldAllowConfiguringHalfPointReward() throws Exception {
    Long storeId = 26L;
    Long ownerId = 42L;
    when(storeVerificationPort.storeExists(storeId)).thenReturn(true);
    when(storeVerificationPort.isStoreOwnedBy(storeId, ownerId)).thenReturn(true);

    String token = generateToken("MERCHANT", ownerId);
    MerchantLoyaltyRateRequest request =
        new MerchantLoyaltyRateRequest(new BigDecimal("2.00"), new BigDecimal("0.5"));

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/" + storeId + "/rates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeId").value(storeId))
        .andExpect(jsonPath("$.euroAmount").value(2.00))
        .andExpect(jsonPath("$.pointsReward").value(0.5));
  }

  @Test
  @DisplayName("PUT with points not multiple of 0.5 (e.g. 0.7 pt) -> should return 400 Bad Request")
  void shouldRejectPointsNotMultipleOfHalf() throws Exception {
    Long storeId = 27L;
    Long ownerId = 42L;
    when(storeVerificationPort.storeExists(storeId)).thenReturn(true);
    when(storeVerificationPort.isStoreOwnedBy(storeId, ownerId)).thenReturn(true);

    String token = generateToken("MERCHANT", ownerId);
    String payload = "{\"euroAmount\": 2.00, \"pointsReward\": 0.7}";

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/" + storeId + "/rates")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PUT in ADMIN mode -> should succeed without ownership restriction")
  void shouldAllowAdminToConfigureRate() throws Exception {
    Long storeId = 30L;
    String adminToken = generateToken("ADMIN", 1L);
    MerchantLoyaltyRateRequest request =
        new MerchantLoyaltyRateRequest(new BigDecimal("10.00"), new BigDecimal("20.0"));

    mockMvc
        .perform(
            put("/v1/loyalty/merchants/" + storeId + "/rates")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeId").value(storeId))
        .andExpect(jsonPath("$.euroAmount").value(10.00))
        .andExpect(jsonPath("$.pointsReward").value(20.0));
  }
}
