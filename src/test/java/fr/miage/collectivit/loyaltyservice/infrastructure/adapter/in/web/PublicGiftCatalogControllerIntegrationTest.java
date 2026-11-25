package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import fr.miage.collectivit.loyaltyservice.domain.port.out.GiftImageStoragePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataLoyaltyGiftRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
@Transactional
@DisplayName("Web Adapter - PublicGiftCatalogController Integration Test (SLE-39 / SLE-223)")
class PublicGiftCatalogControllerIntegrationTest {

  private static final UUID STORE_UID_10 = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
  private static final UUID STORE_UID_20 = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private SpringDataLoyaltyGiftRepository giftRepository;

  @MockitoBean private StoreVerificationPort storeVerificationPort;
  @MockitoBean private GiftImageStoragePort giftImageStoragePort;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    giftRepository.deleteAll();

    when(storeVerificationPort.findStoreName(STORE_UID_10))
        .thenReturn(Optional.of("Boulangerie Paul"));
    when(storeVerificationPort.findStoreCategory(STORE_UID_10))
        .thenReturn(Optional.of("Alimentation"));

    when(storeVerificationPort.findStoreName(STORE_UID_20))
        .thenReturn(Optional.of("Librairie Moderne"));
    when(storeVerificationPort.findStoreCategory(STORE_UID_20)).thenReturn(Optional.of("Culture"));
  }

  private LoyaltyGiftJpaEntity buildGift(
      UUID storeUid,
      String name,
      String description,
      int pointsCost,
      int stock,
      String imageUrl,
      boolean active) {
    return LoyaltyGiftJpaEntity.builder()
        .uid(UUID.randomUUID())
        .storeUid(storeUid)
        .name(name)
        .description(description)
        .pointsCost(pointsCost)
        .stock(stock)
        .imageUrl(imageUrl)
        .active(active)
        .createdAt(Instant.now())
        .updatedAt(Instant.now())
        .build();
  }

  @Test
  @DisplayName(
      "GET /v1/loyalty/gifts -> should retrieve all active gifts publicly without authentication")
  void shouldRetrieveAllActiveGiftsPubliclyWithoutAuthentication() throws Exception {
    LoyaltyGiftJpaEntity activeGift1 =
        buildGift(
            STORE_UID_10,
            "Croissant Offert",
            "Un bon croissant pur beurre",
            50,
            25,
            "https://cdn.shoploc.fr/croissant.png",
            true);

    LoyaltyGiftJpaEntity activeGift2 =
        buildGift(
            STORE_UID_20,
            "Livre de poche",
            "Roman au choix",
            120,
            10,
            "https://cdn.shoploc.fr/livre.png",
            true);

    LoyaltyGiftJpaEntity inactiveGift =
        buildGift(STORE_UID_10, "Ancien cadeau épuisé", "Non disponible", 200, 0, null, false);

    giftRepository.save(activeGift1);
    giftRepository.save(activeGift2);
    giftRepository.save(inactiveGift);

    mockMvc
        .perform(get("/v1/loyalty/gifts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].uid").isNotEmpty())
        .andExpect(jsonPath("$[0].name").value("Croissant Offert"))
        .andExpect(jsonPath("$[0].storeUid").value(STORE_UID_10.toString()))
        .andExpect(jsonPath("$[0].storeName").value("Boulangerie Paul"))
        .andExpect(jsonPath("$[0].category").value("Alimentation"))
        .andExpect(jsonPath("$[0].pointsCost").value(50))
        .andExpect(jsonPath("$[0].stock").value(25))
        .andExpect(jsonPath("$[0].imageUrl").value("https://cdn.shoploc.fr/croissant.png"))
        .andExpect(jsonPath("$[0].active").value(true))
        .andExpect(jsonPath("$[1].uid").isNotEmpty())
        .andExpect(jsonPath("$[1].name").value("Livre de poche"))
        .andExpect(jsonPath("$[1].storeUid").value(STORE_UID_20.toString()))
        .andExpect(jsonPath("$[1].storeName").value("Librairie Moderne"))
        .andExpect(jsonPath("$[1].category").value("Culture"))
        .andExpect(jsonPath("$[1].pointsCost").value(120));
  }

  @Test
  @DisplayName("GET /v1/loyalty/gifts?storeUid=... -> should filter gifts by partner store")
  void shouldFilterGiftsByStoreUid() throws Exception {
    giftRepository.save(
        buildGift(STORE_UID_10, "Croissant Offert", "Un bon croissant", 50, 10, null, true));
    giftRepository.save(
        buildGift(STORE_UID_20, "Marque-page", "Marque-page collector", 30, 50, null, true));

    mockMvc
        .perform(get("/v1/loyalty/gifts").param("storeUid", STORE_UID_10.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].storeUid").value(STORE_UID_10.toString()))
        .andExpect(jsonPath("$[0].name").value("Croissant Offert"));
  }

  @Test
  @DisplayName("GET /v1/loyalty/gifts?storeUid=not-a-uuid -> should return 400 Bad Request")
  void shouldRejectNonUuidStoreUidParam() throws Exception {
    mockMvc
        .perform(get("/v1/loyalty/gifts").param("storeUid", "not-a-uuid"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /v1/loyalty/gifts?category=Culture -> should filter gifts by merchant category")
  void shouldFilterGiftsByCategory() throws Exception {
    giftRepository.save(
        buildGift(STORE_UID_10, "Croissant Offert", "Un bon croissant", 50, 10, null, true));
    giftRepository.save(
        buildGift(STORE_UID_20, "Marque-page", "Marque-page collector", 30, 50, null, true));

    mockMvc
        .perform(get("/v1/loyalty/gifts").param("category", "Culture"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].storeUid").value(STORE_UID_20.toString()))
        .andExpect(jsonPath("$[0].category").value("Culture"))
        .andExpect(jsonPath("$[0].name").value("Marque-page"));
  }

  @Test
  @DisplayName("GET /v1/loyalty/gifts with mismatched filter -> should return empty list")
  void shouldReturnEmptyListWhenNoGiftsMatch() throws Exception {
    giftRepository.save(
        buildGift(STORE_UID_10, "Croissant Offert", "Un bon croissant", 50, 10, null, true));

    mockMvc
        .perform(get("/v1/loyalty/gifts").param("category", "Informatique"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }

  @Test
  @DisplayName(
      "GET /v1/loyalty/gifts -> should expose vfpExclusive flag on public catalog items (SLE-49)")
  void shouldExposeVfpExclusiveFlagInPublicCatalog() throws Exception {
    LoyaltyGiftJpaEntity vfpGift =
        buildGift(STORE_UID_10, "Dégustation Privée VFP", "Accès VFP", 250, 4, null, true);
    vfpGift.setVfpExclusive(true);
    giftRepository.save(vfpGift);

    mockMvc
        .perform(get("/v1/loyalty/gifts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].name").value("Dégustation Privée VFP"))
        .andExpect(jsonPath("$[0].vfpExclusive").value(true));
  }

  @Test
  @DisplayName("GET /v1/loyalty/gifts/page -> should retrieve paginated active gifts publicly")
  void shouldRetrievePaginatedActiveGiftsPublicly() throws Exception {
    LoyaltyGiftJpaEntity activeGift1 =
        buildGift(
            STORE_UID_10,
            "Croissant Offert",
            "Un bon croissant",
            50,
            25,
            "https://cdn.shoploc.fr/croissant.png",
            true);
    LoyaltyGiftJpaEntity activeGift2 =
        buildGift(
            STORE_UID_20,
            "Livre de poche",
            "Roman au choix",
            120,
            10,
            "https://cdn.shoploc.fr/livre.png",
            true);

    giftRepository.save(activeGift1);
    giftRepository.save(activeGift2);

    mockMvc
        .perform(get("/v1/loyalty/gifts/page").param("page", "0").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.page").value(0))
        .andExpect(jsonPath("$.size").value(1))
        .andExpect(jsonPath("$.totalElements").value(2))
        .andExpect(jsonPath("$.totalPages").value(2))
        .andExpect(jsonPath("$.first").value(true))
        .andExpect(jsonPath("$.last").value(false))
        .andExpect(jsonPath("$.content.length()").value(1));
  }
}
