package fr.miage.collectivit.loyaltyservice.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Pure domain projection representing a loyalty gift item enriched with merchant store information
 * for public catalog consumption (SLE-39 / SLE-223).
 */
public record CatalogGiftItem(
    UUID uid,
    UUID storeUid,
    String storeName,
    String category,
    String name,
    String description,
    Integer pointsCost,
    Integer stock,
    String imageUrl,
    boolean active,
    Instant createdAt,
    Instant updatedAt) {

  public static CatalogGiftItem of(LoyaltyGift gift, String storeName, String category) {
    if (gift == null) {
      return null;
    }
    return new CatalogGiftItem(
        gift.getUid(),
        gift.getStoreUid(),
        storeName,
        category,
        gift.getName(),
        gift.getDescription(),
        gift.getPointsCost(),
        gift.getStock(),
        gift.getImageUrl(),
        gift.isActive(),
        gift.getCreatedAt(),
        gift.getUpdatedAt());
  }
}
