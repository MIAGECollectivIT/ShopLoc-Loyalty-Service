package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftCreateRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftStockAdjustRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftUpdateRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataLoyaltyGiftRepository;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@DisplayName("Web Adapter - GiftCatalogController Integration Test (SLE-60 / SLE-192, SLE-193)")
class GiftCatalogControllerIntegrationTest {

  private static final String TEST_SECRET =
      "dGhpc2lzYXZlcnlzZWNyZXRqd3R1c2Vyc3NlcnZpY2VrZXkxMjM0NTY=";

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private SpringDataLoyaltyGiftRepository giftRepository;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean private StoreVerificationPort storeVerificationPort;

  private MockMvc mockMvc;
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
    signingKey = Keys.hmacShaKeyFor(keyBytes);
    giftRepository.deleteAll();
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
  @DisplayName("GET /v1/loyalty/stores/{storeId}/gifts -> public retrieval of store gifts")
  void shouldRetrieveStoreGiftsPublicly() throws Exception {
    Long storeId = 10L;
    LoyaltyGiftJpaEntity gift1 =
        new LoyaltyGiftJpaEntity(
            null,
            storeId,
            "Mug ShopLoc",
            "Superbe mug",
            50,
            10,
            null,
            true,
            Instant.now(),
            Instant.now());
    LoyaltyGiftJpaEntity gift2 =
        new LoyaltyGiftJpaEntity(
            null,
            storeId,
            "T-shirt ShopLoc",
            "T-shirt coton",
            100,
            5,
            null,
            true,
            Instant.now(),
            Instant.now());
    giftRepository.save(gift1);
    giftRepository.save(gift2);

    mockMvc
        .perform(get("/v1/loyalty/stores/" + storeId + "/gifts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Mug ShopLoc"))
        .andExpect(jsonPath("$[1].name").value("T-shirt ShopLoc"));
  }

  @Test
  @DisplayName("GET /v1/loyalty/stores/{storeId}/gifts/{giftId} -> retrieve single gift")
  void shouldRetrieveSingleGift() throws Exception {
    Long storeId = 10L;
    LoyaltyGiftJpaEntity gift =
        giftRepository.save(
            new LoyaltyGiftJpaEntity(
                null,
                storeId,
                "Mug ShopLoc",
                "Superbe mug",
                50,
                10,
                null,
                true,
                Instant.now(),
                Instant.now()));

    mockMvc
        .perform(get("/v1/loyalty/stores/" + storeId + "/gifts/" + gift.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(gift.getId()))
        .andExpect(jsonPath("$.name").value("Mug ShopLoc"))
        .andExpect(jsonPath("$.pointsCost").value(50));
  }

  @Test
  @DisplayName("POST /v1/loyalty/stores/{storeId}/gifts -> 401 if missing token")
  void shouldRejectCreateWithoutToken() throws Exception {
    GiftCreateRequest request = new GiftCreateRequest("Mug", "Desc", 50, 10, null);

    mockMvc
        .perform(
            post("/v1/loyalty/stores/10/gifts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST /v1/loyalty/stores/{storeId}/gifts -> 403 if CLIENT role")
  void shouldRejectCreateForClientRole() throws Exception {
    String token = generateToken("CLIENT", 42L);
    GiftCreateRequest request = new GiftCreateRequest("Mug", "Desc", 50, 10, null);

    mockMvc
        .perform(
            post("/v1/loyalty/stores/10/gifts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /v1/loyalty/stores/{storeId}/gifts -> 403 if merchant not store owner")
  void shouldRejectCreateIfMerchantNotOwner() throws Exception {
    Long storeId = 10L;
    Long ownerId = 42L;
    Long otherMerchantId = 99L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(ownerId));

    String token = generateToken("MERCHANT", otherMerchantId);
    GiftCreateRequest request = new GiftCreateRequest("Mug", "Desc", 50, 10, null);

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeId + "/gifts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /v1/loyalty/stores/{storeId}/gifts -> 201 Created by store owner")
  void shouldCreateGiftSuccessfully() throws Exception {
    Long storeId = 10L;
    Long merchantId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantId));

    String token = generateToken("MERCHANT", merchantId);
    GiftCreateRequest request =
        new GiftCreateRequest(
            "Casquette ShopLoc", "Casquette brodée", 80, 15, "https://cdn.shoploc.fr/cap.png");

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeId + "/gifts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isNotEmpty())
        .andExpect(jsonPath("$.storeId").value(storeId))
        .andExpect(jsonPath("$.name").value("Casquette ShopLoc"))
        .andExpect(jsonPath("$.pointsCost").value(80))
        .andExpect(jsonPath("$.stock").value(15))
        .andExpect(jsonPath("$.imageUrl").value("https://cdn.shoploc.fr/cap.png"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  @DisplayName("PUT /v1/loyalty/stores/{storeId}/gifts/{giftId} -> 200 OK update gift details")
  void shouldUpdateGiftSuccessfully() throws Exception {
    Long storeId = 10L;
    Long merchantId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantId));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(
            new LoyaltyGiftJpaEntity(
                null,
                storeId,
                "Ancien Nom",
                "Ancienne desc",
                50,
                10,
                null,
                true,
                Instant.now(),
                Instant.now()));

    String token = generateToken("MERCHANT", merchantId);
    GiftUpdateRequest request =
        new GiftUpdateRequest("Nouveau Nom", "Nouvelle desc", 90, "https://cdn.shoploc.fr/new.png");

    mockMvc
        .perform(
            put("/v1/loyalty/stores/" + storeId + "/gifts/" + gift.getId())
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Nouveau Nom"))
        .andExpect(jsonPath("$.description").value("Nouvelle desc"))
        .andExpect(jsonPath("$.pointsCost").value(90))
        .andExpect(jsonPath("$.imageUrl").value("https://cdn.shoploc.fr/new.png"));
  }

  @Test
  @DisplayName(
      "PATCH /v1/loyalty/stores/{storeId}/gifts/{giftId}/stock -> 200 OK adjust stock (SLE-193)")
  void shouldAdjustGiftStockSuccessfully() throws Exception {
    Long storeId = 10L;
    Long merchantId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantId));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(
            new LoyaltyGiftJpaEntity(
                null, storeId, "Mug", "Desc", 50, 10, null, true, Instant.now(), Instant.now()));

    String token = generateToken("MERCHANT", merchantId);
    GiftStockAdjustRequest request = new GiftStockAdjustRequest(15);

    mockMvc
        .perform(
            patch("/v1/loyalty/stores/" + storeId + "/gifts/" + gift.getId() + "/stock")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(25));
  }

  @Test
  @DisplayName(
      "PATCH /v1/loyalty/stores/{storeId}/gifts/{giftId}/stock -> 400 Bad Request on stock underflow")
  void shouldRejectNegativeResultingStock() throws Exception {
    Long storeId = 10L;
    Long merchantId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantId));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(
            new LoyaltyGiftJpaEntity(
                null, storeId, "Mug", "Desc", 50, 10, null, true, Instant.now(), Instant.now()));

    String token = generateToken("MERCHANT", merchantId);
    GiftStockAdjustRequest request = new GiftStockAdjustRequest(-20);

    mockMvc
        .perform(
            patch("/v1/loyalty/stores/" + storeId + "/gifts/" + gift.getId() + "/stock")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("DELETE /v1/loyalty/stores/{storeId}/gifts/{giftId} -> 204 No Content")
  void shouldDeleteGiftSuccessfully() throws Exception {
    Long storeId = 10L;
    Long merchantId = 42L;
    when(storeVerificationPort.findStoreOwnerId(storeId)).thenReturn(Optional.of(merchantId));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(
            new LoyaltyGiftJpaEntity(
                null, storeId, "Mug", "Desc", 50, 10, null, true, Instant.now(), Instant.now()));

    String token = generateToken("MERCHANT", merchantId);

    mockMvc
        .perform(
            delete("/v1/loyalty/stores/" + storeId + "/gifts/" + gift.getId())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/v1/loyalty/stores/" + storeId + "/gifts/" + gift.getId()))
        .andExpect(status().isNotFound());
  }
}
