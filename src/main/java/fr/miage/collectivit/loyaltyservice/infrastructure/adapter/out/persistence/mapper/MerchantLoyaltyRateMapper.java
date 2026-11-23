package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.MerchantLoyaltyRateJpaEntity;
import org.springframework.stereotype.Component;

/** Bidirectional mapper between Domain Model and infrastructure JPA Entity. */
@Component
public class MerchantLoyaltyRateMapper {

  public MerchantLoyaltyRate toDomain(MerchantLoyaltyRateJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new MerchantLoyaltyRate(
        entity.getId(),
        entity.getUid(),
        entity.getStoreUid(),
        entity.getEuroAmount(),
        entity.getPointsReward(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public MerchantLoyaltyRateJpaEntity toJpaEntity(MerchantLoyaltyRate domain) {
    if (domain == null) {
      return null;
    }
    return MerchantLoyaltyRateJpaEntity.builder()
        .id(domain.getId())
        .uid(domain.getUid())
        .storeUid(domain.getStoreUid())
        .euroAmount(domain.getEuroAmount())
        .pointsReward(domain.getPointsReward())
        .createdAt(domain.getCreatedAt())
        .updatedAt(domain.getUpdatedAt())
        .build();
  }
}
