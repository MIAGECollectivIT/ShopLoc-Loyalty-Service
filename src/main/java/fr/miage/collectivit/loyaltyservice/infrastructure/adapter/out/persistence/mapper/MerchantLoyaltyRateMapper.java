package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.MerchantLoyaltyRateJpaEntity;
import org.springframework.stereotype.Component;

/** Mapper bidirectionnel entre le Domain Model et la JPA Entity d'infrastructure. */
@Component
public class MerchantLoyaltyRateMapper {

  public MerchantLoyaltyRate toDomain(MerchantLoyaltyRateJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new MerchantLoyaltyRate(
        entity.getId(),
        entity.getStoreId(),
        entity.getEuroAmount(),
        entity.getPointsReward(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  public MerchantLoyaltyRateJpaEntity toJpaEntity(MerchantLoyaltyRate domain) {
    if (domain == null) {
      return null;
    }
    return new MerchantLoyaltyRateJpaEntity(
        domain.getId(),
        domain.getStoreId(),
        domain.getEuroAmount(),
        domain.getPointsReward(),
        domain.getCreatedAt(),
        domain.getUpdatedAt());
  }
}
