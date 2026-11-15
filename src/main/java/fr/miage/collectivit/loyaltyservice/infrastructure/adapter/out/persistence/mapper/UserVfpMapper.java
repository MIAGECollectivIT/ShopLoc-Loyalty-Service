package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpEventType;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpHistoryEntry;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserVfpHistoryJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserVfpStatusJpaEntity;
import org.springframework.stereotype.Component;

/** Bidirectional mapper between Domain VFP models and JPA entities (SLE-45 / SLE-199). */
@Component
@SuppressWarnings("PMD.LawOfDemeter")
public class UserVfpMapper {

  public UserVfpStatus toDomain(UserVfpStatusJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserVfpStatus(
        entity.getUserId(),
        entity.isVfp(),
        entity.getGrantedAt(),
        entity.getRevokedAt(),
        entity.getLastEvaluatedAt());
  }

  public UserVfpStatusJpaEntity toJpaEntity(UserVfpStatus domain) {
    if (domain == null) {
      return null;
    }
    return UserVfpStatusJpaEntity.builder()
        .userId(domain.getUserId())
        .isVfp(domain.isVfp())
        .grantedAt(domain.getGrantedAt())
        .revokedAt(domain.getRevokedAt())
        .lastEvaluatedAt(domain.getLastEvaluatedAt())
        .build();
  }

  public VfpHistoryEntry toDomain(UserVfpHistoryJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    VfpEventType eventType = VfpEventType.valueOf(entity.getEventType());
    return new VfpHistoryEntry(
        entity.getId(),
        entity.getUserId(),
        eventType,
        entity.getTransactionCount(),
        entity.getCreatedAt());
  }

  public UserVfpHistoryJpaEntity toJpaEntity(VfpHistoryEntry domain) {
    if (domain == null) {
      return null;
    }
    return UserVfpHistoryJpaEntity.builder()
        .id(domain.getId())
        .userId(domain.getUserId())
        .eventType(domain.getEventType().name())
        .transactionCount(domain.getTransactionCount())
        .createdAt(domain.getCreatedAt())
        .build();
  }
}
