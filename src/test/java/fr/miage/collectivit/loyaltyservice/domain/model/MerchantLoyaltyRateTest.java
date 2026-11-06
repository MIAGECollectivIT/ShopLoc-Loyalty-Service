package fr.miage.collectivit.loyaltyservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidLoyaltyRateException;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Domain Model - MerchantLoyaltyRate")
class MerchantLoyaltyRateTest {

  @Test
  @DisplayName("Devrait créer un barème de fidélité valide")
  void shouldCreateValidLoyaltyRate() {
    MerchantLoyaltyRate rate = new MerchantLoyaltyRate(1L, new BigDecimal("1.50"), 2);

    assertThat(rate.getStoreId()).isEqualTo(1L);
    assertThat(rate.getEuroAmount()).isEqualByComparingTo(new BigDecimal("1.50"));
    assertThat(rate.getPointsReward()).isEqualTo(2);
    assertThat(rate.getCreatedAt()).isNotNull();
    assertThat(rate.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Devrait refuser un montant en euro négatif ou nul")
  void shouldRejectZeroOrNegativeEuroAmount() {
    assertThatThrownBy(() -> new MerchantLoyaltyRate(1L, BigDecimal.ZERO, 1))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("strictement positif");

    assertThatThrownBy(() -> new MerchantLoyaltyRate(1L, new BigDecimal("-5.00"), 1))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("strictement positif");
  }

  @Test
  @DisplayName("Devrait refuser un nombre de points négatif ou nul")
  void shouldRejectZeroOrNegativePointsReward() {
    assertThatThrownBy(() -> new MerchantLoyaltyRate(1L, new BigDecimal("1.00"), 0))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("entier strictement positif");

    assertThatThrownBy(() -> new MerchantLoyaltyRate(1L, new BigDecimal("1.00"), -3))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("entier strictement positif");
  }

  @ParameterizedTest(name = "Achat de {0} € avec barème 1 € = 1 pt donne {1} points")
  @CsvSource({
    "0.00, 0",
    "0.50, 0",
    "0.99, 0",
    "1.00, 1",
    "1.50, 1",
    "2.00, 2",
    "9.99, 9",
    "10.00, 10"
  })
  @DisplayName("Critère SLE-55 : règle du plancher minimal pour barème standard (1 € = 1 pt)")
  void shouldCalculatePointsWithStandardRate(String purchaseStr, int expectedPoints) {
    MerchantLoyaltyRate rate = new MerchantLoyaltyRate(1L, new BigDecimal("1.00"), 1);

    int points = rate.calculatePointsForPurchase(new BigDecimal(purchaseStr));

    assertThat(points).isEqualTo(expectedPoints);
  }

  @ParameterizedTest(name = "Achat de {0} € avec barème 5 € = 10 pts donne {1} points")
  @CsvSource({"4.99, 0", "5.00, 10", "7.50, 10", "10.00, 20", "14.99, 20", "15.00, 30"})
  @DisplayName("Critère SLE-55 : règle du plancher minimal pour barème personnalisé (5 € = 10 pts)")
  void shouldCalculatePointsWithCustomRate(String purchaseStr, int expectedPoints) {
    MerchantLoyaltyRate rate = new MerchantLoyaltyRate(2L, new BigDecimal("5.00"), 10);

    int points = rate.calculatePointsForPurchase(new BigDecimal(purchaseStr));

    assertThat(points).isEqualTo(expectedPoints);
  }
}
