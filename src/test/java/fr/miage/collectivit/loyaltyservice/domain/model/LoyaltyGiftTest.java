package fr.miage.collectivit.loyaltyservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.loyaltyservice.domain.exception.InsufficientGiftStockException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidGiftException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Domain Model - LoyaltyGift")
class LoyaltyGiftTest {

  private static final UUID STORE_UID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

  @Test
  @DisplayName("Should successfully instantiate a valid loyalty gift")
  void shouldCreateValidLoyaltyGift() {
    Instant now = Instant.parse("2026-11-16T10:00:00Z");
    UUID giftUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    LoyaltyGift gift =
        new LoyaltyGift(
            1L,
            giftUid,
            STORE_UID,
            "Tote bag ShopLoc",
            "Sac en coton bio aux couleurs des commerces locaux",
            50,
            25,
            "https://cdn.shoploc.fr/gifts/totebag.jpg",
            true,
            now,
            now);

    assertThat(gift.getId()).isEqualTo(1L);
    assertThat(gift.getUid()).isEqualTo(giftUid);
    assertThat(gift.getStoreUid()).isEqualTo(STORE_UID);
    assertThat(gift.getName()).isEqualTo("Tote bag ShopLoc");
    assertThat(gift.getDescription())
        .isEqualTo("Sac en coton bio aux couleurs des commerces locaux");
    assertThat(gift.getPointsCost()).isEqualTo(50);
    assertThat(gift.getStock()).isEqualTo(25);
    assertThat(gift.getImageUrl()).isEqualTo("https://cdn.shoploc.fr/gifts/totebag.jpg");
    assertThat(gift.isActive()).isTrue();
    assertThat(gift.hasAvailableStock()).isTrue();
  }

  @Test
  @DisplayName("Should reject instantiation when storeUid is null")
  void shouldRejectInvalidStoreUid() {
    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    null, "Tote bag", "Description", 50, 10, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Store UID is mandatory");
  }

  @Test
  @DisplayName("Should reject instantiation when name is blank")
  void shouldRejectBlankName() {
    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    STORE_UID, "   ", "Description", 50, 10, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Gift name is mandatory");
  }

  @Test
  @DisplayName("Should reject instantiation when pointsCost is not strictly positive")
  void shouldRejectNonPositivePointsCost() {
    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    STORE_UID, "Tote bag", "Description", 0, 10, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Points cost must be strictly positive");

    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    STORE_UID,
                    "Tote bag",
                    "Description",
                    -10,
                    10,
                    "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Points cost must be strictly positive");
  }

  @Test
  @DisplayName("Should reject instantiation when initial stock is negative")
  void shouldRejectNegativeStock() {
    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    STORE_UID, "Tote bag", "Description", 50, -1, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Stock cannot be negative");
  }

  @Test
  @DisplayName("Should update gift attributes with valid inputs")
  void shouldUpdateGiftAttributes() {
    LoyaltyGift gift =
        new LoyaltyGift(
            STORE_UID,
            "Tote bag",
            "Description initiale",
            50,
            10,
            "https://cdn.shoploc.fr/img.png");

    gift.update(
        "Tote bag Édition Spéciale",
        "Nouvelle description",
        75,
        "https://cdn.shoploc.fr/new-img.png");

    assertThat(gift.getName()).isEqualTo("Tote bag Édition Spéciale");
    assertThat(gift.getDescription()).isEqualTo("Nouvelle description");
    assertThat(gift.getPointsCost()).isEqualTo(75);
    assertThat(gift.getImageUrl()).isEqualTo("https://cdn.shoploc.fr/new-img.png");
  }

  @Test
  @DisplayName("Should adjust stock positively and negatively")
  void shouldAdjustStock() {
    LoyaltyGift gift = new LoyaltyGift(STORE_UID, "Tote bag", "Description", 50, 10, null);

    gift.adjustStock(5);
    assertThat(gift.getStock()).isEqualTo(15);

    gift.adjustStock(-8);
    assertThat(gift.getStock()).isEqualTo(7);

    gift.adjustStock(-7);
    assertThat(gift.getStock()).isEqualTo(0);
    assertThat(gift.hasAvailableStock()).isFalse();
  }

  @Test
  @DisplayName("Should throw InsufficientGiftStockException when deduction exceeds available stock")
  void shouldThrowWhenDeductionExceedsStock() {
    LoyaltyGift gift = new LoyaltyGift(STORE_UID, "Tote bag", "Description", 50, 5, null);

    assertThatThrownBy(() -> gift.adjustStock(-10))
        .isInstanceOf(InsufficientGiftStockException.class)
        .hasMessageContaining("Insufficient gift stock");
  }

  @Test
  @DisplayName("Should toggle active status")
  void shouldToggleActiveStatus() {
    LoyaltyGift gift = new LoyaltyGift(STORE_UID, "Tote bag", "Description", 50, 10, null);

    assertThat(gift.isActive()).isTrue();

    gift.deactivate();
    assertThat(gift.isActive()).isFalse();

    gift.activate();
    assertThat(gift.isActive()).isTrue();
  }

  @Test
  @DisplayName(
      "Should default vfpExclusive to false and support explicit VFP-exclusive flag and update (SLE-49)")
  void shouldManageVfpExclusiveFlag() {
    LoyaltyGift standardGift = new LoyaltyGift(STORE_UID, "Tote bag", "Description", 50, 10, null);
    assertThat(standardGift.isVfpExclusive()).isFalse();

    LoyaltyGift vfpGift =
        new LoyaltyGift(STORE_UID, "Coffret VIP", "Réservé VFP", 200, 5, null, true);
    assertThat(vfpGift.isVfpExclusive()).isTrue();

    vfpGift.update("Coffret Standard", "Ouvert à tous", 150, null, false);
    assertThat(vfpGift.isVfpExclusive()).isFalse();

    standardGift.update("Tote bag VIP", "Maintenant VFP", 80, null, true);
    assertThat(standardGift.isVfpExclusive()).isTrue();
  }
}
