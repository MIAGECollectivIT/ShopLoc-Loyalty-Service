package fr.miage.collectivit.loyaltyservice.domain.model;

import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidLoyaltyRateException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

/**
 * Objet métier pur (Domain Model) représentant le barème de fidélité d'un commerce (Story SLE-55 /
 * SLE-145). Supporte les demi-points (pas de 0.5 point).
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
    this.storeId = Objects.requireNonNull(storeId, "L'identifiant du commerce est obligatoire.");
    validateAndSetRates(euroAmount, pointsReward);
    this.id = id;
    this.createdAt = createdAt != null ? createdAt : Instant.now();
    this.updatedAt = updatedAt != null ? updatedAt : Instant.now();
  }

  public static MerchantLoyaltyRate defaultForStore(Long storeId) {
    return new MerchantLoyaltyRate(storeId, DEFAULT_EURO_AMOUNT, DEFAULT_POINTS_REWARD);
  }

  /**
   * Valide les critères d'acceptation du barème : - Montant minimal en euro strictement positif (>
   * 0). - Nombre de points >= 0.5 et par pas de 0.5 (demi-points).
   */
  public void updateRates(BigDecimal newEuroAmount, BigDecimal newPointsReward) {
    validateAndSetRates(newEuroAmount, newPointsReward);
    this.updatedAt = Instant.now();
  }

  private void validateAndSetRates(BigDecimal amount, BigDecimal points) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new InvalidLoyaltyRateException(
          "Le montant d'achat minimal en euro doit être strictement positif.");
    }
    if (points == null || points.compareTo(MIN_POINTS_STEP) < 0) {
      throw new InvalidLoyaltyRateException(
          "Le nombre de points attribués doit être supérieur ou égal à 0.5.");
    }

    // Vérifier que le nombre de points est un multiple de 0.5 (ex: 0.5, 1.0, 1.5, 2.0...)
    BigDecimal doubled = points.multiply(BigDecimal.valueOf(2));
    if (doubled.stripTrailingZeros().scale() > 0) {
      throw new InvalidLoyaltyRateException(
          "Le nombre de points attribués doit être un multiple de 0.5 (ex: 0.5, 1, 1.5, 2).");
    }

    this.euroAmount = amount.setScale(2, RoundingMode.HALF_UP);
    this.pointsReward = points.setScale(1, RoundingMode.HALF_UP);
  }

  /**
   * Règle métier centrale : calcule les points accordés pour un montant d'achat donné. Calcul au
   * demi-point près (pas de 0.5 pt). Ex: pour 2.00 € = 1 pt, un achat de 25 € donne 12.5 points.
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

  public void setId(Long id) {
    this.id = id;
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
