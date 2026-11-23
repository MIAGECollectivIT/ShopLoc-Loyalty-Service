package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import org.springframework.stereotype.Component;

/** Mapper between domain LoyaltyGift and infrastructure JPA entity. */
@Component
public class LoyaltyGiftMapper {

  public LoyaltyGift toDomain(LoyaltyGiftJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new LoyaltyGift(
        entity.getId(),
        entity.getUid(),
        entity.getStoreUid(),
        entity.getName(),
        entity.getDescription(),
        entity.getPointsCost(),
        entity.getStock(),
        entity.getImageUrl(),
        entity.isActive(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public LoyaltyGiftJpaEntity toEntity(LoyaltyGift domain) {
    if (domain == null) {
      return null;
    }
    return LoyaltyGiftJpaEntity.builder()
        .id(domain.getId())
        .uid(domain.getUid())
        .storeUid(domain.getStoreUid())
        .name(domain.getName())
        .description(domain.getDescription())
        .pointsCost(domain.getPointsCost())
        .stock(domain.getStock())
        .imageUrl(domain.getImageUrl())
        .active(domain.isActive())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }
}
