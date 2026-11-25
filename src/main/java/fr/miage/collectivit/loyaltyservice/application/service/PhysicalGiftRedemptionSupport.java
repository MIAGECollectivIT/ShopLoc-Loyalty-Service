package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.GiftNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidGiftException;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.model.PhysicalGiftRedemptionResult.RedeemedGiftItem;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.port.in.RedeemPhysicalGiftsCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.RedeemPhysicalGiftsCommand.GiftSelectionItem;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Helper encapsulating validation, aggregation, reference generation, and summary formatting for
 * physical gift redemptions (Story SLE-43).
 */
@SuppressWarnings("PMD.LawOfDemeter")
final class PhysicalGiftRedemptionSupport {

  private static final int MAX_DESCRIPTION_LENGTH = 255;
  private static final String TX_REF_PREFIX = "GIFT-PHYS-";

  private PhysicalGiftRedemptionSupport() {}

  static void validateArguments(UUID storeUid, RedeemPhysicalGiftsCommand command) {
    if (storeUid == null) {
      throw new IllegalArgumentException("storeUid must not be null");
    }
    if (command == null) {
      throw new IllegalArgumentException("Redemption command must not be null");
    }
    if (command.items() == null || command.items().isEmpty()) {
      throw new IllegalArgumentException("At least one gift item must be selected");
    }
    validateCustomerIdentifier(command);
  }

  private static void validateCustomerIdentifier(RedeemPhysicalGiftsCommand command) {
    boolean hasCustomerId = trimToNull(command.customerId()) != null;
    boolean hasClientCode = trimToNull(command.clientCode()) != null;
    if (!hasCustomerId && !hasClientCode) {
      throw new IllegalArgumentException("Either customerId or clientCode must be provided");
    }
  }

  static Map<UUID, Integer> aggregateQuantities(List<GiftSelectionItem> items) {
    Map<UUID, Integer> aggregated = new LinkedHashMap<>();
    for (GiftSelectionItem item : items) {
      validateItem(item);
      aggregated.merge(item.giftUid(), item.quantity(), Math::addExact);
    }
    return aggregated;
  }

  private static void validateItem(GiftSelectionItem item) {
    if (item == null || item.giftUid() == null) {
      throw new IllegalArgumentException("Each selected gift must specify a valid giftUid");
    }
    if (item.quantity() <= 0) {
      throw new IllegalArgumentException("Selected gift quantity must be at least 1");
    }
  }

  record ResolvedRedemption(List<RedeemedGiftItem> items, boolean hasVfpExclusive) {}

  static ResolvedRedemption resolveRedeemedItems(
      UUID storeUid,
      Map<UUID, Integer> aggregatedQuantities,
      LoyaltyGiftPersistencePort giftPersistencePort) {
    boolean[] hasVfpExclusive = {false};
    List<RedeemedGiftItem> items =
        aggregatedQuantities.entrySet().stream()
            .map(
                entry -> {
                  LoyaltyGift gift =
                      loadActiveGift(storeUid, entry.getKey(), giftPersistencePort);
                  if (gift.isVfpExclusive()) {
                    hasVfpExclusive[0] = true;
                  }
                  int linePoints = Math.multiplyExact(gift.getPointsCost(), entry.getValue());
                  return new RedeemedGiftItem(
                      gift.getUid(),
                      gift.getName(),
                      gift.getPointsCost(),
                      entry.getValue(),
                      linePoints);
                })
            .toList();
    return new ResolvedRedemption(items, hasVfpExclusive[0]);
  }

  private static LoyaltyGift loadActiveGift(
      UUID storeUid, UUID giftUid, LoyaltyGiftPersistencePort giftPersistencePort) {
    LoyaltyGift gift =
        giftPersistencePort
            .findByUidAndStoreUid(giftUid, storeUid)
            .orElseThrow(() -> new GiftNotFoundException(storeUid, giftUid));

    if (!gift.isActive()) {
      throw new InvalidGiftException(
          "Le cadeau '" + gift.getName() + "' n'est plus actif dans ce commerce.");
    }
    return gift;
  }

  static String resolveConsistentClientCode(UserPointsAccount account, String requestedClientCode) {
    String existingCode = trimToNull(account.clientCode());
    if (existingCode != null
        && requestedClientCode != null
        && !existingCode.equalsIgnoreCase(requestedClientCode)) {
      throw new IllegalArgumentException(
          "Le code fidélité fourni ne correspond pas au compte client identifié.");
    }
    return existingCode != null ? existingCode : requestedClientCode;
  }

  static String buildTransactionReference(String idempotencyKey) {
    if (idempotencyKey != null) {
      String sanitized = idempotencyKey.replaceAll("[^A-Za-z0-9_-]", "");
      if (sanitized.isEmpty()) {
        throw new IllegalArgumentException("idempotencyKey contains no valid characters");
      }
      int maxSuffix = 64 - TX_REF_PREFIX.length();
      String suffix =
          sanitized.length() > maxSuffix ? sanitized.substring(0, maxSuffix) : sanitized;
      return TX_REF_PREFIX + suffix.toUpperCase(Locale.ROOT);
    }
    return TX_REF_PREFIX
        + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase(Locale.ROOT);
  }

  static String buildDescription(List<RedeemedGiftItem> items, String storeName) {
    String summary =
        items.stream()
            .map(item -> item.quantity() + "x " + item.name())
            .collect(Collectors.joining(", "));
    String full = "Remise physique : " + summary + " - " + storeName;
    if (full.length() <= MAX_DESCRIPTION_LENGTH) {
      return full;
    }
    return full.substring(0, MAX_DESCRIPTION_LENGTH - 3) + "...";
  }

  static String trimToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
