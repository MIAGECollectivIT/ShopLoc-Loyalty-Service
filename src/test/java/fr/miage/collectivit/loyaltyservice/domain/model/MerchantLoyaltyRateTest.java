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
  @DisplayName("Devrait créer un barème de fidélité valide avec points entiers ou demi-points")
  void shouldCreateValidLoyaltyRate() {
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(1L, new BigDecimal("1.50"), new BigDecimal("2.5"));

    assertThat(rate.getStoreId()).isEqualTo(1L);
    assertThat(rate.getEuroAmount()).isEqualByComparingTo(new BigDecimal("1.50"));
    assertThat(rate.getPointsReward()).isEqualByComparingTo(new BigDecimal("2.5"));
    assertThat(rate.getCreatedAt()).isNotNull();
    assertThat(rate.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Devrait refuser un montant en euro négatif ou nul")
  void shouldRejectZeroOrNegativeEuroAmount() {
    assertThatThrownBy(() -> new MerchantLoyaltyRate(1L, BigDecimal.ZERO, new BigDecimal("1.0")))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("strictement positif");

    assertThatThrownBy(
            () -> new MerchantLoyaltyRate(1L, new BigDecimal("-5.00"), new BigDecimal("1.0")))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("strictement positif");
  }

  @Test
  @DisplayName("Devrait refuser un nombre de points inférieur à 0.5 ou non multiple de 0.5")
  void shouldRejectInvalidPointsReward() {
    assertThatThrownBy(() -> new MerchantLoyaltyRate(1L, new BigDecimal("1.00"), BigDecimal.ZERO))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("supérieur ou égal à 0.5");

    assertThatThrownBy(
            () -> new MerchantLoyaltyRate(1L, new BigDecimal("1.00"), new BigDecimal("0.3")))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("supérieur ou égal à 0.5");

    assertThatThrownBy(
            () -> new MerchantLoyaltyRate(1L, new BigDecimal("1.00"), new BigDecimal("1.2")))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("multiple de 0.5");
  }

  @ParameterizedTest(name = "Achat de {0} € avec barème 1 € = 1 pt donne {1} points")
  @CsvSource({
    "0.00, 0.0",
    "0.40, 0.0",
    "0.50, 0.5",
    "0.99, 0.5",
    "1.00, 1.0",
    "1.50, 1.5",
    "2.00, 2.0",
    "9.99, 9.5",
    "10.00, 10.0"
  })
  @DisplayName("Critère SLE-55 : règle par pas de demi-point pour barème standard (1 € = 1 pt)")
  void shouldCalculatePointsWithStandardRate(String purchaseStr, String expectedPointsStr) {
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(1L, new BigDecimal("1.00"), new BigDecimal("1.0"));

    BigDecimal points = rate.calculatePointsForPurchase(new BigDecimal(purchaseStr));

    assertThat(points).isEqualByComparingTo(new BigDecimal(expectedPointsStr));
  }

  @ParameterizedTest(name = "Achat de {0} € avec barème 2 € = 1 pt donne {1} points")
  @CsvSource({"1.00, 0.5", "2.00, 1.0", "3.00, 1.5", "24.00, 12.0", "25.00, 12.5", "27.00, 13.5"})
  @DisplayName("Critère SLE-55 : achat de 25 € avec 2 € = 1 pt donne bien 12.5 points")
  void shouldCalculateHalfPointsAccurately(String purchaseStr, String expectedPointsStr) {
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(1L, new BigDecimal("2.00"), new BigDecimal("1.0"));

    BigDecimal points = rate.calculatePointsForPurchase(new BigDecimal(purchaseStr));

    assertThat(points).isEqualByComparingTo(new BigDecimal(expectedPointsStr));
  }

  @ParameterizedTest(name = "Achat de {0} € avec barème 5 € = 10 pts donne {1} points")
  @CsvSource({"4.99, 9.5", "5.00, 10.0", "7.50, 15.0", "10.00, 20.0", "14.99, 29.5", "15.00, 30.0"})
  @DisplayName("Critère SLE-55 : calcul proportionnel pour barème personnalisé (5 € = 10 pts)")
  void shouldCalculatePointsWithCustomRate(String purchaseStr, String expectedPointsStr) {
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(2L, new BigDecimal("5.00"), new BigDecimal("10.0"));

    BigDecimal points = rate.calculatePointsForPurchase(new BigDecimal(purchaseStr));

    assertThat(points).isEqualByComparingTo(new BigDecimal(expectedPointsStr));
  }
}
