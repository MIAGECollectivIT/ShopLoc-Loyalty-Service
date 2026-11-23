package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsAccountJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsTransactionJpaEntity;
import org.springframework.stereotype.Component;

@Component
@SuppressWarnings("PMD.LawOfDemeter")
public class UserPointsMapper {

  /** Maps a {@link UserPointsAccountJpaEntity} to a {@link UserPointsAccount} domain record. */
  public UserPointsAccount toDomain(UserPointsAccountJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new UserPointsAccount(
        entity.getId(),
        entity.getUserId(),
        entity.getClientCode(),
        entity.getBalance(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }

  /** Maps a {@link UserPointsAccount} domain record to a {@link UserPointsAccountJpaEntity}. */
  public UserPointsAccountJpaEntity toEntity(UserPointsAccount domain) {
    if (domain == null) {
      return null;
    }
    return UserPointsAccountJpaEntity.builder()
        .id(domain.id())
        .userId(domain.userId())
        .clientCode(domain.clientCode())
        .balance(domain.balance())
        .createdAt(domain.createdAt())
        .updatedAt(domain.updatedAt())
        .build();
  }

  /** Maps a {@link UserPointsTransactionJpaEntity} to a {@link PointsTransaction} domain record. */
  public PointsTransaction toDomain(UserPointsTransactionJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new PointsTransaction(
        entity.getId(),
        entity.getUid(),
        entity.getTransactionReference(),
        entity.getUserId(),
        entity.getStoreUid(),
        entity.getStoreName(),
        entity.getType(),
        entity.getPoints(),
        entity.getBalanceAfter(),
        entity.getSourceType(),
        entity.getDescription(),
        entity.getCreatedAt());
  }

  /**
   * Maps a {@link PointsTransaction} domain record to a {@link UserPointsTransactionJpaEntity}
   * linked to the owning {@code accountId}.
   */
  public UserPointsTransactionJpaEntity toEntity(Long accountId, PointsTransaction domain) {
    if (domain == null) {
      return null;
    }
    return UserPointsTransactionJpaEntity.builder()
        .id(domain.id())
        .uid(domain.uid())
        .accountId(accountId)
        .userId(domain.userId())
        .transactionReference(domain.transactionReference())
        .storeUid(domain.storeUid())
        .storeName(domain.storeName())
        .type(domain.type())
        .points(domain.points())
        .balanceAfter(domain.balanceAfter())
        .sourceType(domain.sourceType())
        .description(domain.description())
        .createdAt(domain.createdAt())
        .build();
  }
}
