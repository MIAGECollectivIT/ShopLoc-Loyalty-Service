package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.PhysicalGiftRedemptionRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.PhysicalGiftRedemptionRequest.PhysicalGiftRedemptionItemRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsAccountJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataLoyaltyGiftRepository;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataUserPointsAccountRepository;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataUserPointsTransactionRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@DisplayName("Web Adapter - PhysicalGiftRedemptionController Integration Test (SLE-43)")
class PhysicalGiftRedemptionControllerIntegrationTest {

  private static final String TEST_SECRET =
      "dGhpc2lzYXZlcnlzZWNyZXRqd3R1c2Vyc3NlcnZpY2VrZXkxMjM0NTY=";

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private SpringDataLoyaltyGiftRepository giftRepository;
  @Autowired private SpringDataUserPointsAccountRepository accountRepository;
  @Autowired private SpringDataUserPointsTransactionRepository transactionRepository;

  @MockitoBean private StoreVerificationPort storeVerificationPort;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private MockMvc mockMvc;
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    byte[] keyBytes = Base64.getDecoder().decode(TEST_SECRET);
    signingKey = Keys.hmacShaKeyFor(keyBytes);
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
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

  @Test
  @DisplayName("POST /physical-redemption -> atomic debit and transaction recording")
  void shouldRedeemGiftsPhysically() throws Exception {
    UUID storeUid = UUID.fromString("9b6ae7d0-7fa9-4f15-a47d-56d809984cba");
    UUID merchantUid = UUID.randomUUID();
    String customerUid = UUID.randomUUID().toString();

    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));
    when(storeVerificationPort.findStoreName(storeUid))
        .thenReturn(Optional.of("Burger King Lille"));

    LoyaltyGiftJpaEntity gift1 =
        giftRepository.save(
            LoyaltyGiftJpaEntity.builder()
                .uid(UUID.randomUUID())
                .storeUid(storeUid)
                .name("Menu Whopper Offert")
                .pointsCost(40)
                .stock(0) // Stock = 0 must still be redeemable physically
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

    LoyaltyGiftJpaEntity gift2 =
        giftRepository.save(
            LoyaltyGiftJpaEntity.builder()
                .uid(UUID.randomUUID())
                .storeUid(storeUid)
                .name("Gobelet Collector")
                .pointsCost(15)
                .stock(3)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

    accountRepository.save(
        UserPointsAccountJpaEntity.builder()
            .userId(customerUid)
            .clientCode("CL95001")
            .balance(new BigDecimal("100.0"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build());

    PhysicalGiftRedemptionRequest request =
        new PhysicalGiftRedemptionRequest(
            customerUid,
            "CL95001",
            List.of(
                new PhysicalGiftRedemptionItemRequest(gift1.getUid(), 2),
                new PhysicalGiftRedemptionItemRequest(gift2.getUid(), 1)));

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeUid + "/gifts/physical-redemption")
                .header(
                    HttpHeaders.AUTHORIZATION, "Bearer " + generateToken("MERCHANT", merchantUid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.storeUid").value(storeUid.toString()))
        .andExpect(jsonPath("$.storeName").value("Burger King Lille"))
        .andExpect(jsonPath("$.pointsDeducted").value(95.0))
        .andExpect(jsonPath("$.remainingBalance").value(5.0))
        .andExpect(jsonPath("$.items.length()").value(2));

    UserPointsAccountJpaEntity updatedAccount =
        accountRepository.findByUserId(customerUid).orElseThrow();
    assertThat(updatedAccount.getBalance()).isEqualByComparingTo("5.0");

    // Verify stock was NOT modified
    assertThat(giftRepository.findByUid(gift1.getUid()).orElseThrow().getStock()).isEqualTo(0);
    assertThat(giftRepository.findByUid(gift2.getUid()).orElseThrow().getStock()).isEqualTo(3);

    var transactions = transactionRepository.findByUserIdOrderByCreatedAtDescIdDesc(customerUid);
    assertThat(transactions).hasSize(1);
    assertThat(transactions.get(0).getSourceType()).isEqualTo("GIFT_REDEEM_PHYSICAL");
    assertThat(transactions.get(0).getPoints()).isEqualByComparingTo("95.0");
  }

  @Test
  @DisplayName("POST /physical-redemption -> 400 when insufficient points")
  void shouldReturnBadRequestWhenInsufficientPoints() throws Exception {
    UUID storeUid = UUID.fromString("9b6ae7d0-7fa9-4f15-a47d-56d809984cba");
    UUID merchantUid = UUID.randomUUID();
    String customerUid = UUID.randomUUID().toString();

    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));
    when(storeVerificationPort.findStoreName(storeUid))
        .thenReturn(Optional.of("Burger King Lille"));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(
            LoyaltyGiftJpaEntity.builder()
                .uid(UUID.randomUUID())
                .storeUid(storeUid)
                .name("Menu Whopper Offert")
                .pointsCost(60)
                .stock(5)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

    accountRepository.save(
        UserPointsAccountJpaEntity.builder()
            .userId(customerUid)
            .clientCode("CL95001")
            .balance(new BigDecimal("50.0"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build());

    PhysicalGiftRedemptionRequest request =
        new PhysicalGiftRedemptionRequest(
            customerUid,
            "CL95001",
            List.of(new PhysicalGiftRedemptionItemRequest(gift.getUid(), 1)));

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeUid + "/gifts/physical-redemption")
                .header(
                    HttpHeaders.AUTHORIZATION, "Bearer " + generateToken("MERCHANT", merchantUid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").exists());

    assertThat(accountRepository.findByUserId(customerUid).orElseThrow().getBalance())
        .isEqualByComparingTo("50.0");
  }

  @Test
  @DisplayName("POST /physical-redemption -> 403 when caller is CLIENT")
  void shouldForbidClientRole() throws Exception {
    UUID storeUid = UUID.fromString("9b6ae7d0-7fa9-4f15-a47d-56d809984cba");
    UUID clientUid = UUID.randomUUID();

    PhysicalGiftRedemptionRequest request =
        new PhysicalGiftRedemptionRequest(
            clientUid.toString(),
            "CL95001",
            List.of(new PhysicalGiftRedemptionItemRequest(UUID.randomUUID(), 1)));

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeUid + "/gifts/physical-redemption")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + generateToken("CLIENT", clientUid))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
  }

  @Test
  @DisplayName("POST /physical-redemption -> 409 when same idempotencyKey is submitted twice")
  void shouldRejectDuplicateIdempotencyKeyWithConflict() throws Exception {
    UUID storeUid = UUID.fromString("9b6ae7d0-7fa9-4f15-a47d-56d809984cba");
    UUID merchantUid = UUID.randomUUID();
    String customerUid = UUID.randomUUID().toString();

    when(storeVerificationPort.findStoreOwnerUid(storeUid)).thenReturn(Optional.of(merchantUid));
    when(storeVerificationPort.findStoreName(storeUid))
        .thenReturn(Optional.of("Burger King Lille"));

    LoyaltyGiftJpaEntity gift =
        giftRepository.save(
            LoyaltyGiftJpaEntity.builder()
                .uid(UUID.randomUUID())
                .storeUid(storeUid)
                .name("Dessert")
                .pointsCost(25)
                .stock(2)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());

    accountRepository.save(
        UserPointsAccountJpaEntity.builder()
            .userId(customerUid)
            .clientCode("CL95001")
            .balance(new BigDecimal("100.0"))
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build());

    PhysicalGiftRedemptionRequest request =
        new PhysicalGiftRedemptionRequest(
            customerUid,
            "CL95001",
            List.of(new PhysicalGiftRedemptionItemRequest(gift.getUid(), 1)),
            "idem-key-001");

    String token = generateToken("MERCHANT", merchantUid);
    String body = objectMapper.writeValueAsString(request);

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeUid + "/gifts/physical-redemption")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.remainingBalance").value(75.0));

    mockMvc
        .perform(
            post("/v1/loyalty/stores/" + storeUid + "/gifts/physical-redemption")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.message").exists());

    assertThat(accountRepository.findByUserId(customerUid).orElseThrow().getBalance())
        .isEqualByComparingTo("75.0");
  }
}
