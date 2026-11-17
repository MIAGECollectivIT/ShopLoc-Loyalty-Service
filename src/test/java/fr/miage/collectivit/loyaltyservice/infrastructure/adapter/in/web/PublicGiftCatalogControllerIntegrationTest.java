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

  @Autowired private WebApplicationContext webApplicationContext;
  @Autowired private SpringDataLoyaltyGiftRepository giftRepository;

  @MockitoBean private StoreVerificationPort storeVerificationPort;
  @MockitoBean private GiftImageStoragePort giftImageStoragePort;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    giftRepository.deleteAll();

    when(storeVerificationPort.findStoreName(10L)).thenReturn(Optional.of("Boulangerie Paul"));
    when(storeVerificationPort.findStoreCategory(10L)).thenReturn(Optional.of("Alimentation"));

    when(storeVerificationPort.findStoreName(20L)).thenReturn(Optional.of("Librairie Moderne"));
    when(storeVerificationPort.findStoreCategory(20L)).thenReturn(Optional.of("Culture"));
  }

  @Test
  @DisplayName(
      "GET /v1/loyalty/gifts -> should retrieve all active gifts publicly without authentication")
  void shouldRetrieveAllActiveGiftsPubliclyWithoutAuthentication() throws Exception {
    LoyaltyGiftJpaEntity activeGift1 =
        new LoyaltyGiftJpaEntity(
            null,
            10L,
            "Croissant Offert",
            "Un bon croissant pur beurre",
            50,
            25,
            "https://cdn.shoploc.fr/croissant.png",
            true,
            Instant.now(),
            Instant.now());

    LoyaltyGiftJpaEntity activeGift2 =
        new LoyaltyGiftJpaEntity(
            null,
            20L,
            "Livre de poche",
            "Roman au choix",
            120,
            10,
            "https://cdn.shoploc.fr/livre.png",
            true,
            Instant.now(),
            Instant.now());

    LoyaltyGiftJpaEntity inactiveGift =
        new LoyaltyGiftJpaEntity(
            null,
            10L,
            "Ancien cadeau épuisé",
            "Non disponible",
            200,
            0,
            null,
            false,
            Instant.now(),
            Instant.now());

    giftRepository.save(activeGift1);
    giftRepository.save(activeGift2);
    giftRepository.save(inactiveGift);

    mockMvc
        .perform(get("/v1/loyalty/gifts"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].name").value("Croissant Offert"))
        .andExpect(jsonPath("$[0].storeId").value(10))
        .andExpect(jsonPath("$[0].storeName").value("Boulangerie Paul"))
        .andExpect(jsonPath("$[0].category").value("Alimentation"))
        .andExpect(jsonPath("$[0].pointsCost").value(50))
        .andExpect(jsonPath("$[0].stock").value(25))
        .andExpect(jsonPath("$[0].imageUrl").value("https://cdn.shoploc.fr/croissant.png"))
        .andExpect(jsonPath("$[0].active").value(true))
        .andExpect(jsonPath("$[1].name").value("Livre de poche"))
        .andExpect(jsonPath("$[1].storeId").value(20))
        .andExpect(jsonPath("$[1].storeName").value("Librairie Moderne"))
        .andExpect(jsonPath("$[1].category").value("Culture"))
        .andExpect(jsonPath("$[1].pointsCost").value(120));
  }

  @Test
  @DisplayName("GET /v1/loyalty/gifts?storeId=10 -> should filter gifts by partner store")
  void shouldFilterGiftsByStoreId() throws Exception {
    LoyaltyGiftJpaEntity giftStore10 =
        new LoyaltyGiftJpaEntity(
            null,
            10L,
            "Croissant Offert",
            "Un bon croissant",
            50,
            10,
            null,
            true,
            Instant.now(),
            Instant.now());

    LoyaltyGiftJpaEntity giftStore20 =
        new LoyaltyGiftJpaEntity(
            null,
            20L,
            "Marque-page",
            "Marque-page collector",
            30,
            50,
            null,
            true,
            Instant.now(),
            Instant.now());

    giftRepository.save(giftStore10);
    giftRepository.save(giftStore20);

    mockMvc
        .perform(get("/v1/loyalty/gifts").param("storeId", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].storeId").value(10))
        .andExpect(jsonPath("$[0].name").value("Croissant Offert"));
  }

  @Test
  @DisplayName("GET /v1/loyalty/gifts?category=Culture -> should filter gifts by merchant category")
  void shouldFilterGiftsByCategory() throws Exception {
    LoyaltyGiftJpaEntity giftFood =
        new LoyaltyGiftJpaEntity(
            null,
            10L,
            "Croissant Offert",
            "Un bon croissant",
            50,
            10,
            null,
            true,
            Instant.now(),
            Instant.now());

    LoyaltyGiftJpaEntity giftCulture =
        new LoyaltyGiftJpaEntity(
            null,
            20L,
            "Marque-page",
            "Marque-page collector",
            30,
            50,
            null,
            true,
            Instant.now(),
            Instant.now());

    giftRepository.save(giftFood);
    giftRepository.save(giftCulture);

    mockMvc
        .perform(get("/v1/loyalty/gifts").param("category", "Culture"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].storeId").value(20))
        .andExpect(jsonPath("$[0].category").value("Culture"))
        .andExpect(jsonPath("$[0].name").value("Marque-page"));
  }

  @Test
  @DisplayName("GET /v1/loyalty/gifts with mismatched filter -> should return empty list")
  void shouldReturnEmptyListWhenNoGiftsMatch() throws Exception {
    LoyaltyGiftJpaEntity giftFood =
        new LoyaltyGiftJpaEntity(
            null,
            10L,
            "Croissant Offert",
            "Un bon croissant",
            50,
            10,
            null,
            true,
            Instant.now(),
            Instant.now());

    giftRepository.save(giftFood);

    mockMvc
        .perform(get("/v1/loyalty/gifts").param("category", "Informatique"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(0));
  }
}
