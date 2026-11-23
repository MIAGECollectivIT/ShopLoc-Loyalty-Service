package fr.miage.collectivit.loyaltyservice.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidLoyaltyRateException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Domain Model - MerchantLoyaltyRate")
class MerchantLoyaltyRateTest {

  private static final UUID STORE_UID_1 = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
  private static final UUID STORE_UID_2 = UUID.fromString("550e8400-e29b-41d4-a716-446655440002");
  private static final UUID STORE_UID_50 = UUID.fromString("550e8400-e29b-41d4-a716-446655440050");

  @Test
  @DisplayName("Should create valid loyalty rate with whole or half points")
  void shouldCreateValidLoyaltyRate() {
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(STORE_UID_1, new BigDecimal("1.50"), new BigDecimal("2.5"));

    assertThat(rate.getStoreUid()).isEqualTo(STORE_UID_1);
    assertThat(rate.getEuroAmount()).isEqualByComparingTo(new BigDecimal("1.50"));
    assertThat(rate.getPointsReward()).isEqualByComparingTo(new BigDecimal("2.5"));
    assertThat(rate.getCreatedAt()).isNotNull();
    assertThat(rate.getUpdatedAt()).isNotNull();
  }

  @Test
  @DisplayName("Should create standard default rate for store (1€ = 1 pt)")
  void shouldCreateDefaultForStore() {
    MerchantLoyaltyRate defaultRate = MerchantLoyaltyRate.defaultForStore(STORE_UID_50);

    assertThat(defaultRate.getStoreUid()).isEqualTo(STORE_UID_50);
    assertThat(defaultRate.getEuroAmount()).isEqualByComparingTo(new BigDecimal("1.00"));
    assertThat(defaultRate.getPointsReward()).isEqualByComparingTo(new BigDecimal("1.0"));
    assertThat(defaultRate.getId()).isNull();
    assertThat(defaultRate.getUid()).isNull();
  }

  @Test
  @DisplayName("Should reject zero or negative euro amount")
  void shouldRejectZeroOrNegativeEuroAmount() {
    assertThatThrownBy(
            () -> new MerchantLoyaltyRate(STORE_UID_1, BigDecimal.ZERO, new BigDecimal("1.0")))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("strictly positive");

    assertThatThrownBy(
            () ->
                new MerchantLoyaltyRate(
                    STORE_UID_1, new BigDecimal("-5.00"), new BigDecimal("1.0")))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("strictly positive");
  }

  @Test
  @DisplayName("Should reject points reward below 0.5 or not multiple of 0.5")
  void shouldRejectInvalidPointsReward() {
    assertThatThrownBy(
            () -> new MerchantLoyaltyRate(STORE_UID_1, new BigDecimal("1.00"), BigDecimal.ZERO))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("greater than or equal to 0.5");

    assertThatThrownBy(
            () ->
                new MerchantLoyaltyRate(STORE_UID_1, new BigDecimal("1.00"), new BigDecimal("0.3")))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("greater than or equal to 0.5");

    assertThatThrownBy(
            () ->
                new MerchantLoyaltyRate(STORE_UID_1, new BigDecimal("1.00"), new BigDecimal("1.2")))
        .isInstanceOf(InvalidLoyaltyRateException.class)
        .hasMessageContaining("multiple of 0.5");
  }

  @ParameterizedTest(name = "Purchase of {0} € with rate 1 € = 1 pt yields {1} points")
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
  @DisplayName("SLE-55 criterion: half-point step rule for standard rate (1 € = 1 pt)")
  void shouldCalculatePointsWithStandardRate(String purchaseStr, String expectedPointsStr) {
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(STORE_UID_1, new BigDecimal("1.00"), new BigDecimal("1.0"));

    BigDecimal points = rate.calculatePointsForPurchase(new BigDecimal(purchaseStr));

    assertThat(points).isEqualByComparingTo(new BigDecimal(expectedPointsStr));
  }

  @ParameterizedTest(name = "Purchase of {0} € with rate 2 € = 1 pt yields {1} points")
  @CsvSource({"1.00, 0.5", "2.00, 1.0", "3.00, 1.5", "24.00, 12.0", "25.00, 12.5", "27.00, 13.5"})
  @DisplayName("SLE-55 criterion: 25 € purchase with 2 € = 1 pt yields 12.5 points")
  void shouldCalculateHalfPointsAccurately(String purchaseStr, String expectedPointsStr) {
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(STORE_UID_1, new BigDecimal("2.00"), new BigDecimal("1.0"));

    BigDecimal points = rate.calculatePointsForPurchase(new BigDecimal(purchaseStr));

    assertThat(points).isEqualByComparingTo(new BigDecimal(expectedPointsStr));
  }

  @ParameterizedTest(name = "Purchase of {0} € with rate 5 € = 10 pts yields {1} points")
  @CsvSource({"4.99, 9.5", "5.00, 10.0", "7.50, 15.0", "10.00, 20.0", "14.99, 29.5", "15.00, 30.0"})
  @DisplayName("SLE-55 criterion: proportional calculation for custom rate (5 € = 10 pts)")
  void shouldCalculatePointsWithCustomRate(String purchaseStr, String expectedPointsStr) {
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(STORE_UID_2, new BigDecimal("5.00"), new BigDecimal("10.0"));

    BigDecimal points = rate.calculatePointsForPurchase(new BigDecimal(purchaseStr));

    assertThat(points).isEqualByComparingTo(new BigDecimal(expectedPointsStr));
  }
}
