package fr.miage.collectivit.loyaltyservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.loyaltyservice.domain.exception.InsufficientGiftStockException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidGiftException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Domain Model - LoyaltyGift")
class LoyaltyGiftTest {

  @Test
  @DisplayName("Should successfully instantiate a valid loyalty gift")
  void shouldCreateValidLoyaltyGift() {
    Instant now = Instant.parse("2026-11-16T10:00:00Z");
    LoyaltyGift gift =
        new LoyaltyGift(
            1L,
            10L,
            "Tote bag ShopLoc",
            "Sac en coton bio aux couleurs des commerces locaux",
            50,
            25,
            "https://cdn.shoploc.fr/gifts/totebag.jpg",
            true,
            now,
            now);

    assertThat(gift.getId()).isEqualTo(1L);
    assertThat(gift.getStoreId()).isEqualTo(10L);
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
  @DisplayName("Should reject instantiation when storeId is null or non-positive")
  void shouldRejectInvalidStoreId() {
    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    null, "Tote bag", "Description", 50, 10, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Store ID is mandatory");

    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    -1L, "Tote bag", "Description", 50, 10, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Store ID must be strictly positive");
  }

  @Test
  @DisplayName("Should reject instantiation when name is blank")
  void shouldRejectBlankName() {
    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    10L, "   ", "Description", 50, 10, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Gift name is mandatory");
  }

  @Test
  @DisplayName("Should reject instantiation when pointsCost is not strictly positive")
  void shouldRejectNonPositivePointsCost() {
    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    10L, "Tote bag", "Description", 0, 10, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Points cost must be strictly positive");

    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    10L, "Tote bag", "Description", -10, 10, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Points cost must be strictly positive");
  }

  @Test
  @DisplayName("Should reject instantiation when initial stock is negative")
  void shouldRejectNegativeStock() {
    assertThatThrownBy(
            () ->
                new LoyaltyGift(
                    10L, "Tote bag", "Description", 50, -1, "https://cdn.shoploc.fr/img.png"))
        .isInstanceOf(InvalidGiftException.class)
        .hasMessageContaining("Stock cannot be negative");
  }

  @Test
  @DisplayName("Should update gift attributes with valid inputs")
  void shouldUpdateGiftAttributes() {
    LoyaltyGift gift =
        new LoyaltyGift(
            10L, "Tote bag", "Description initiale", 50, 10, "https://cdn.shoploc.fr/img.png");

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
    LoyaltyGift gift = new LoyaltyGift(10L, "Tote bag", "Description", 50, 10, null);

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
    LoyaltyGift gift = new LoyaltyGift(10L, "Tote bag", "Description", 50, 5, null);

    assertThatThrownBy(() -> gift.adjustStock(-10))
        .isInstanceOf(InsufficientGiftStockException.class)
        .hasMessageContaining("Insufficient gift stock");
  }

  @Test
  @DisplayName("Should toggle active status")
  void shouldToggleActiveStatus() {
    LoyaltyGift gift = new LoyaltyGift(10L, "Tote bag", "Description", 50, 10, null);

    assertThat(gift.isActive()).isTrue();

    gift.deactivate();
    assertThat(gift.isActive()).isFalse();

    gift.activate();
    assertThat(gift.isActive()).isTrue();
  }
}
