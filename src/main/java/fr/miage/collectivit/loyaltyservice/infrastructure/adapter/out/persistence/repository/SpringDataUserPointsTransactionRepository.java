package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsTransactionJpaEntity;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link UserPointsTransactionJpaEntity} (SLE-38 / SLE-176). */
@Repository
public interface SpringDataUserPointsTransactionRepository
    extends JpaRepository<UserPointsTransactionJpaEntity, Long> {

  List<UserPointsTransactionJpaEntity> findByUserIdOrderByCreatedAtDescIdDesc(String userId);

  boolean existsByTransactionReference(String transactionReference);

  boolean existsByUserIdAndStoreUidAndType(
      String userId, UUID storeUid, PointsTransactionType type);

  @Query(
      "SELECT DISTINCT t.storeUid FROM UserPointsTransactionJpaEntity t "
          + "WHERE t.userId = :userId AND t.type = :type AND t.storeUid IS NOT NULL")
  Set<UUID> findDistinctStoreUidsByUserIdAndType(
      @Param("userId") String userId, @Param("type") PointsTransactionType type);
}
