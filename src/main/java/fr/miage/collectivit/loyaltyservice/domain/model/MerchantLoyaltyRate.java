package fr.miage.collectivit.loyaltyservice.domain.model;

import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidLoyaltyRateException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Pure Domain Model representing a merchant's loyalty rate (Story SLE-55 / SLE-145). Supports
 * half-points (0.5 point steps).
 */
public class MerchantLoyaltyRate {

  public static final BigDecimal DEFAULT_EURO_AMOUNT = new BigDecimal("1.00");
  public static final BigDecimal DEFAULT_POINTS_REWARD = new BigDecimal("1.0");
  public static final BigDecimal MIN_POINTS_STEP = new BigDecimal("0.5");

  private Long id;
  private final Long storeId;
  private BigDecimal euroAmount;
  private BigDecimal pointsReward;
  private Instant createdAt;
  private Instant updatedAt;

  public MerchantLoyaltyRate(Long storeId, BigDecimal euroAmount, BigDecimal pointsReward) {
    this(null, storeId, euroAmount, pointsReward, Instant.now(), Instant.now());
  }

  public MerchantLoyaltyRate(
      Long id,
      Long storeId,
      BigDecimal euroAmount,
      BigDecimal pointsReward,
      Instant createdAt,
      Instant updatedAt) {
    this.storeId = Objects.requireNonNull(storeId, "Store identifier is mandatory.");
    validateAndSetRates(euroAmount, pointsReward);
    this.id = id;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public static MerchantLoyaltyRate defaultForStore(Long storeId) {
    return new MerchantLoyaltyRate(storeId, DEFAULT_EURO_AMOUNT, DEFAULT_POINTS_REWARD);
  }

  /**
   * Validates rate acceptance criteria: - Minimum purchase euro amount strictly positive (> 0). -
   * Points reward >= 0.5 and incremented by steps of 0.5 (half-points).
   */
  public void updateRates(BigDecimal newEuroAmount, BigDecimal newPointsReward) {
    validateAndSetRates(newEuroAmount, newPointsReward);
    this.updatedAt = Instant.now();
  }

  private void validateAndSetRates(BigDecimal amount, BigDecimal points) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidLoyaltyRateException(
          "Minimum purchase euro amount must be strictly positive.");
    }
    if (points == null || points.compareTo(MIN_POINTS_STEP) < 0) {
      throw new InvalidLoyaltyRateException("Points reward must be greater than or equal to 0.5.");
    }

    // Verify that points reward is a multiple of 0.5 (e.g., 0.5, 1.0, 1.5, 2.0...)
    BigDecimal doubled = points.multiply(BigDecimal.valueOf(2));
    if (doubled.stripTrailingZeros().scale() > 0) {
      throw new InvalidLoyaltyRateException(
          "Points reward must be a multiple of 0.5 (e.g., 0.5, 1, 1.5, 2).");
    }

    this.euroAmount = amount.setScale(2, RoundingMode.HALF_UP);
    this.pointsReward = points.setScale(1, RoundingMode.HALF_UP);
  }

  /**
   * Core business rule: calculates loyalty points awarded for a given purchase amount. Computed
   * with 0.5 half-point precision (floor rounded). E.g., for 2.00 € = 1 pt, a 25 € purchase yields
   * 12.5 points.
   */
  public BigDecimal calculatePointsForPurchase(BigDecimal purchaseAmount) {
    if (purchaseAmount == null || purchaseAmount.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY);
    }
    BigDecimal rawPoints =
        purchaseAmount.multiply(this.pointsReward).divide(this.euroAmount, 4, RoundingMode.HALF_UP);
    BigDecimal halfSteps =
        rawPoints.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.FLOOR);
    return halfSteps.divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);
  }

  public Long getId() {
    return id;
  }

  public Long getStoreId() {
    return storeId;
  }

  public BigDecimal getEuroAmount() {
    return euroAmount;
  }

  public BigDecimal getPointsReward() {
    return pointsReward;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
