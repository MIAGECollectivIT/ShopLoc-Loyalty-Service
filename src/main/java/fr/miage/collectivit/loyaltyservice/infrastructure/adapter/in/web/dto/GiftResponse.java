package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.CatalogGiftItem;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.time.Instant;

/** Response DTO representing a loyalty gift item. */
public record GiftResponse(
    Long id,
    Long storeId,
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

  public GiftResponse(
      Long id,
      Long storeId,
      String name,
      String description,
      Integer pointsCost,
      Integer stock,
      String imageUrl,
      boolean active,
      Instant createdAt,
      Instant updatedAt) {
    this(
        id,
        storeId,
        null,
        null,
        name,
        description,
        pointsCost,
        stock,
        imageUrl,
        active,
        createdAt,
        updatedAt);
  }

  public static GiftResponse fromDomain(LoyaltyGift domain) {
    return fromDomain(domain, null, null);
  }

  public static GiftResponse fromDomain(LoyaltyGift domain, String storeName, String category) {
    if (domain == null) {
      return null;
    }
    return new GiftResponse(
        domain.getId(),
        domain.getStoreId(),
        storeName,
        category,
        domain.getName(),
        domain.getDescription(),
        domain.getPointsCost(),
        domain.getStock(),
        domain.getImageUrl(),
        domain.isActive(),
        domain.getCreatedAt(),
        domain.getUpdatedAt());
  }

  public static GiftResponse fromCatalogItem(CatalogGiftItem item) {
    if (item == null) {
      return null;
    }
    return new GiftResponse(
        item.id(),
        item.storeId(),
        item.storeName(),
        item.category(),
        item.name(),
        item.description(),
        item.pointsCost(),
        item.stock(),
        item.imageUrl(),
        item.active(),
        item.createdAt(),
        item.updatedAt());
  }
}
