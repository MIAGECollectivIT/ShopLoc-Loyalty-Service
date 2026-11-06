package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.math.BigDecimal;
import java.time.Instant;

/** Réponse renvoyant le barème de fidélité d'un commerce. */
public record MerchantLoyaltyRateResponse(
    Long id,
    Long storeId,
    BigDecimal euroAmount,
    Integer pointsReward,
    Instant createdAt,
    Instant updatedAt) {

  public static MerchantLoyaltyRateResponse fromDomain(MerchantLoyaltyRate domain) {
    if (domain == null) {
      return null;
    }
    return new MerchantLoyaltyRateResponse(
        domain.getId(),
        domain.getStoreId(),
        domain.getEuroAmount(),
        domain.getPointsReward(),
        domain.getCreatedAt(),
        domain.getUpdatedAt());
  }

  public static MerchantLoyaltyRateResponse defaultRate(Long storeId) {
    return new MerchantLoyaltyRateResponse(
        null,
        storeId,
        MerchantLoyaltyRate.DEFAULT_EURO_AMOUNT,
        MerchantLoyaltyRate.DEFAULT_POINTS_REWARD,
        null,
        null);
  }
}
