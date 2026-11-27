package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyPointsHoldJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyPointsHoldJpaEntity.HoldStatus;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataLoyaltyPointsHoldRepository
    extends JpaRepository<LoyaltyPointsHoldJpaEntity, Long> {

  Optional<LoyaltyPointsHoldJpaEntity> findByOrderReference(String orderReference);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT h FROM LoyaltyPointsHoldJpaEntity h WHERE h.orderReference = :orderReference")
  Optional<LoyaltyPointsHoldJpaEntity> findByOrderReferenceForUpdate(
      @Param("orderReference") String orderReference);

  @Query(
      "SELECT COALESCE(SUM(h.totalPoints), 0) FROM LoyaltyPointsHoldJpaEntity h "
          + "WHERE h.userId = :userId AND h.status = :status")
  BigDecimal sumTotalPointsByUserIdAndStatus(
      @Param("userId") String userId, @Param("status") HoldStatus status);

  List<LoyaltyPointsHoldJpaEntity> findByStatusAndExpiresAtBefore(
      HoldStatus status, Instant expiresAt);
}
