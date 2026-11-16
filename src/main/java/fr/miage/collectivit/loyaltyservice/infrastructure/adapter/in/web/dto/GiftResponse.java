package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.time.Instant;

/** Response DTO representing a loyalty gift item. */
public record GiftResponse(
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

  public static GiftResponse fromDomain(LoyaltyGift domain) {
    if (domain == null) {
      return null;
    }
    return new GiftResponse(
        domain.getId(),
        domain.getStoreId(),
        domain.getName(),
        domain.getDescription(),
        domain.getPointsCost(),
        domain.getStock(),
        domain.getImageUrl(),
        domain.isActive(),
        domain.getCreatedAt(),
        domain.getUpdatedAt());
  }
}
