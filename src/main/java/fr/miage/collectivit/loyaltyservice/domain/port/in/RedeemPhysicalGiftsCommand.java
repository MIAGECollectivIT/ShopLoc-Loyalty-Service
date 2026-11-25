package fr.miage.collectivit.loyaltyservice.domain.port.in;

import java.util.List;
import java.util.UUID;

/** Command encapsulating an in-store physical gift redemption request (Story SLE-43). */
public record RedeemPhysicalGiftsCommand(
    String customerId, String clientCode, List<GiftSelectionItem> items, String idempotencyKey) {

  public RedeemPhysicalGiftsCommand(
      String customerId, String clientCode, List<GiftSelectionItem> items) {
    this(customerId, clientCode, items, null);
  }

  /** Selected gift entry and requested quantity. */
  public record GiftSelectionItem(UUID giftUid, int quantity) {}
}
