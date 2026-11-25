package fr.miage.collectivit.loyaltyservice.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Domain result representing an atomic in-store physical gift redemption (Story SLE-43). */
public record PhysicalGiftRedemptionResult(
    String transactionReference,
    UUID storeUid,
    String storeName,
    String customerId,
    String clientCode,
    BigDecimal pointsDeducted,
    BigDecimal remainingBalance,
    Instant redeemedAt,
    List<RedeemedGiftItem> items) {

  /** Redeemed gift line item with quantity and point subtotal. */
  public record RedeemedGiftItem(
      UUID giftUid, String name, int unitPointsCost, int quantity, int totalPointsCost) {}
}
