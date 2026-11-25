package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.loyaltyservice.domain.model.GiftImageResource;
import fr.miage.collectivit.loyaltyservice.domain.port.out.GiftImageStoragePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftCreateRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftStockAdjustRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftUpdateRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataLoyaltyGiftRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
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
  @MockitoBean private GiftImageStoragePort giftImageStoragePort;

  private MockMvc mockMvc;
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
    signingKey = Keys.hmacShaKeyFor(keyBytes);
    giftRepository.deleteAll();
  }

  private String generateToken(String role, UUID userId) {
    long now = System.currentTimeMillis();
    return Jwts.builder()
        .id(UUID.randomUUID().toString())
        .subject(userId.toString())
        .issuer("shoploc-auth-service")
        .issuedAt(new Date(now))
        .expiration(new Date(now + 3600000))
        .claim("userId", userId.toString())
        .claim("email", role.toLowerCase() + "@shoploc.fr")
        .claim("role", role)
        .signWith(signingKey)
        .compact();
  }

  private LoyaltyGiftJpaEntity buildGiftEntity(
      UUID storeUid, String name, String description, int pointsCost, int stock, String imageUrl) {
    return LoyaltyGiftJpaEntity.builder()
        .uid(UUID.randomUUID())
        .storeUid(storeUid)
        .name(name)
        .description(description)
        .pointsCost(pointsCost)
        .stock(stock)
        .imageUrl(imageUrl)
        .active(true)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Test
  @DisplayName("GET /v1/loyalty/stores/{storeUid}/gifts -> public retrieval of store gifts")
  void shouldRetrieveStoreGiftsPublicly() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    giftRepository.save(buildGiftEntity(storeUid, "Mug ShopLoc", "Superbe mug", 50, 10, null));
    giftRepository.save(
        buildGiftEntity(storeUid, "T-shirt ShopLoc", "T-shirt coton", 100, 5, null));

    mockMvc
        .perform(get("/v1/loyalty/stores/" + storeUid + "/gifts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Mug ShopLoc"))
        .andExpect(jsonPath("$[0].storeUid").value(storeUid.toString()))
        .andExpect(jsonPath("$[1].name").value("T-shirt ShopLoc"));
  }

  @Test
  @DisplayName("GET /v1/loyalty/stores/{storeUid}/gifts/{giftUid} -> retrieve single gift")
  void shouldRetrieveSingleGift() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    LoyaltyGiftJpaEntity gift =
        giftRepository.save(buildGiftEntity(storeUid, "Mug ShopLoc", "Superbe mug", 50, 10, null));

    mockMvc
        .perform(get("/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.uid").value(gift.getUid().toString()))
        .andExpect(jsonPath("$.storeUid").value(storeUid.toString()))
        .andExpect(jsonPath("$.name").value("Mug ShopLoc"))
        .andExpect(jsonPath("$.pointsCost").value(50));
  }

  @Test
  @DisplayName("GET /v1/loyalty/stores/not-a-uuid/gifts -> 400 Bad Request")
  void shouldRejectNonUuidStoreUid() throws Exception {
    mockMvc.perform(get("/v1/loyalty/stores/not-a-uuid/gifts")).andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("POST /v1/loyalty/stores/{storeUid}/gifts -> 401 if missing token")
  void shouldRejectCreateWithoutToken() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    GiftCreateRequest request = new GiftCreateRequest("Mug", "Desc", 50, 10, null);

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeUid + "/gifts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
  }

  @Test
  @DisplayName("POST /v1/loyalty/stores/{storeUid}/gifts -> 403 if CLIENT role")
  void shouldRejectCreateForClientRole() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    String token = generateToken("CLIENT", UUID.randomUUID());
    GiftCreateRequest request = new GiftCreateRequest("Mug", "Desc", 50, 10, null);

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeUid + "/gifts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /v1/loyalty/stores/{storeUid}/gifts -> 403 if merchant not store owner")
  void shouldRejectCreateIfMerchantNotOwner() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID ownerUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    UUID otherMerchantUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(ownerUid));

    String token = generateToken("MERCHANT", otherMerchantUid);
    GiftCreateRequest request = new GiftCreateRequest("Mug", "Desc", 50, 10, null);

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeUid + "/gifts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /v1/loyalty/stores/{storeUid}/gifts -> 201 Created by store owner")
  void shouldCreateGiftSuccessfully() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID merchantUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));

    String token = generateToken("MERCHANT", merchantUid);
    GiftCreateRequest request =
        new GiftCreateRequest(
            "Casquette ShopLoc", "Casquette brodée", 80, 15, "https://cdn.shoploc.fr/cap.png");

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeUid + "/gifts")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.uid").isNotEmpty())
        .andExpect(jsonPath("$.storeUid").value(storeUid.toString()))
        .andExpect(jsonPath("$.name").value("Casquette ShopLoc"))
        .andExpect(jsonPath("$.pointsCost").value(80))
        .andExpect(jsonPath("$.stock").value(15))
        .andExpect(jsonPath("$.imageUrl").value("https://cdn.shoploc.fr/cap.png"))
        .andExpect(jsonPath("$.active").value(true));
  }

  @Test
  @DisplayName("PUT /v1/loyalty/stores/{storeUid}/gifts/{giftUid} -> 200 OK update gift details")
  void shouldUpdateGiftSuccessfully() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID merchantUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(buildGiftEntity(storeUid, "Ancien Nom", "Ancienne desc", 50, 10, null));

    String token = generateToken("MERCHANT", merchantUid);
    GiftUpdateRequest request =
        new GiftUpdateRequest("Nouveau Nom", "Nouvelle desc", 90, "https://cdn.shoploc.fr/new.png");

    mockMvc
        .perform(
            put("/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid())
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
      "PUT /v1/loyalty/stores/{storeUid}/gifts/{giftUid} -> 403 Forbidden when caller is not store owner (IDOR protection)")
  void shouldRejectUpdateWhenCallerNotStoreOwner() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID ownerUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    UUID attackerUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(ownerUid));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(buildGiftEntity(storeUid, "Nom", "Desc", 50, 10, null));

    String attackerToken = generateToken("MERCHANT", attackerUid);
    GiftUpdateRequest request = new GiftUpdateRequest("Hacked", "Hacked desc", 10, null);

    mockMvc
        .perform(
            put("/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid())
                .header("Authorization", "Bearer " + attackerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName(
      "PATCH /v1/loyalty/stores/{storeUid}/gifts/{giftUid}/stock -> 200 OK adjust stock (SLE-193)")
  void shouldAdjustGiftStockSuccessfully() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID merchantUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(buildGiftEntity(storeUid, "Mug", "Desc", 50, 10, null));

    String token = generateToken("MERCHANT", merchantUid);
    GiftStockAdjustRequest request = new GiftStockAdjustRequest(15);

    mockMvc
        .perform(
            patch("/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid() + "/stock")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.stock").value(25));
  }

  @Test
  @DisplayName(
      "PATCH /v1/loyalty/stores/{storeUid}/gifts/{giftUid}/stock -> 400 Bad Request on stock underflow")
  void shouldRejectNegativeResultingStock() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID merchantUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(buildGiftEntity(storeUid, "Mug", "Desc", 50, 10, null));

    String token = generateToken("MERCHANT", merchantUid);
    GiftStockAdjustRequest request = new GiftStockAdjustRequest(-20);

    mockMvc
        .perform(
            patch("/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid() + "/stock")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("DELETE /v1/loyalty/stores/{storeUid}/gifts/{giftUid} -> 204 No Content")
  void shouldDeleteGiftSuccessfully() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID merchantUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(buildGiftEntity(storeUid, "Mug", "Desc", 50, 10, null));

    String token = generateToken("MERCHANT", merchantUid);

    mockMvc
        .perform(
            delete("/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid())
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isNoContent());

    mockMvc
        .perform(get("/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName(
      "POST /v1/loyalty/stores/{storeUid}/gifts (multipart) -> 201 Created with image upload")
  void shouldCreateGiftWithMultipartImage() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID merchantUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));
    when(giftImageStoragePort.storeImage(any(), anyLong(), any(), any()))
        .thenReturn("http://minio:9000/product-images/gifts/store-10/test.png");

    String token = generateToken("MERCHANT", merchantUid);
    GiftCreateRequest request = new GiftCreateRequest("Tote bag", "Coton bio", 75, 20, null);
    MockMultipartFile giftPart =
        new MockMultipartFile(
            "gift",
            "gift.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request));
    MockMultipartFile imagePart =
        new MockMultipartFile(
            "image",
            "totebag.png",
            MediaType.IMAGE_PNG_VALUE,
            "dummy-image-bytes".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart("/v1/loyalty/stores/" + storeUid + "/gifts")
                .file(giftPart)
                .file(imagePart)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Tote bag"))
        .andExpect(
            jsonPath("$.imageUrl")
                .value("http://minio:9000/product-images/gifts/store-10/test.png"));
  }

  @Test
  @DisplayName(
      "PUT /v1/loyalty/stores/{storeUid}/gifts/{giftUid} (multipart) -> 200 OK with new image")
  void shouldUpdateGiftWithMultipartImage() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID merchantUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));
    when(giftImageStoragePort.storeImage(any(), anyLong(), any(), any()))
        .thenReturn("http://minio:9000/product-images/gifts/store-10/updated.png");

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(
            buildGiftEntity(
                storeUid,
                "Old Name",
                "Old Desc",
                50,
                10,
                "http://minio:9000/product-images/gifts/store-10/old.png"));

    String token = generateToken("MERCHANT", merchantUid);
    GiftUpdateRequest request = new GiftUpdateRequest("New Name", "New Desc", 60, null);
    MockMultipartFile giftPart =
        new MockMultipartFile(
            "gift",
            "gift.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request));
    MockMultipartFile imagePart =
        new MockMultipartFile(
            "image",
            "updated.png",
            MediaType.IMAGE_PNG_VALUE,
            "updated-bytes".getBytes(StandardCharsets.UTF_8));

    mockMvc
        .perform(
            multipart(HttpMethod.PUT, "/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid())
                .file(giftPart)
                .file(imagePart)
                .header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("New Name"))
        .andExpect(
            jsonPath("$.imageUrl")
                .value("http://minio:9000/product-images/gifts/store-10/updated.png"));
  }

  @Test
  @DisplayName(
      "GET /v1/loyalty/stores/{storeUid}/gifts/{giftUid}/image -> 200 OK with binary stream")
  void shouldStreamGiftImageSuccessfully() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    String s3Url = "http://minio:9000/product-images/gifts/store-10/sample.png";
    LoyaltyGiftJpaEntity gift =
        giftRepository.save(buildGiftEntity(storeUid, "Gourde", "Gourde isotherme", 80, 5, s3Url));

    byte[] fakePng = "PNG-BYTES-SAMPLE".getBytes(StandardCharsets.UTF_8);
    when(giftImageStoragePort.downloadImage(s3Url))
        .thenReturn(Optional.of(new GiftImageResource(fakePng, "image/png")));

    mockMvc
        .perform(get("/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid() + "/image"))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=86400"))
        .andExpect(content().contentType("image/png"))
        .andExpect(content().bytes(fakePng));
  }

  @Test
  @DisplayName(
      "GET /v1/loyalty/stores/{storeUid}/gifts/{giftUid}/image -> 404 when gift has no image")
  void shouldReturn404WhenGiftHasNoImage() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    LoyaltyGiftJpaEntity gift =
        giftRepository.save(buildGiftEntity(storeUid, "Sans Image", "Desc", 40, 5, null));

    mockMvc
        .perform(get("/v1/loyalty/stores/" + storeUid + "/gifts/" + gift.getUid() + "/image"))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName(
      "POST & PUT /v1/loyalty/stores/{storeUid}/gifts -> persist and toggle vfpExclusive flag (SLE-49)")
  void shouldCreateAndUpdateVfpExclusiveGift() throws Exception {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    UUID merchantUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));

    String token = generateToken("MERCHANT", merchantUid);
    GiftCreateRequest createRequest =
        new GiftCreateRequest(
            "Coffret VFP",
            "Réservé aux clients VFP",
            200,
            5,
            "https://cdn.shoploc.fr/vfp-box.png",
            true);

    String createdBody =
        mockMvc
            .perform(
                post("/v1/loyalty/stores/" + storeUid + "/gifts")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Coffret VFP"))
            .andExpect(jsonPath("$.vfpExclusive").value(true))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String giftUid = objectMapper.readTree(createdBody).get("uid").asText();

    GiftUpdateRequest updateRequest =
        new GiftUpdateRequest(
            "Coffret Standard",
            "Ouvert à tous",
            150,
            "https://cdn.shoploc.fr/std-box.png",
            false);

    mockMvc
        .perform(
            put("/v1/loyalty/stores/" + storeUid + "/gifts/" + giftUid)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Coffret Standard"))
        .andExpect(jsonPath("$.vfpExclusive").value(false));
  }
}
