package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Response DTO containing the loyalty rate configuration for a store. */
public record MerchantLoyaltyRateResponse(
    UUID uid,
    UUID storeUid,
    BigDecimal euroAmount,
    BigDecimal pointsReward,
    Instant createdAt,
    Instant updatedAt) {

  public static MerchantLoyaltyRateResponse fromDomain(MerchantLoyaltyRate domain) {
    if (domain == null) {
      return null;
    }
    return new MerchantLoyaltyRateResponse(
        domain.getUid(),
        domain.getStoreUid(),
        domain.getEuroAmount(),
        domain.getPointsReward(),
        domain.getCreatedAt(),
        domain.getUpdatedAt());
  }

  public static MerchantLoyaltyRateResponse defaultRate(UUID storeUid) {
    return fromDomain(MerchantLoyaltyRate.defaultForStore(storeUid));
  }
}
