package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsAccountJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link UserPointsAccountJpaEntity} (SLE-38 / SLE-176). */
@Repository
public interface SpringDataUserPointsAccountRepository
    extends JpaRepository<UserPointsAccountJpaEntity, Long> {

  Optional<UserPointsAccountJpaEntity> findByUserId(String userId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM UserPointsAccountJpaEntity a WHERE a.userId = :userId")
  Optional<UserPointsAccountJpaEntity> findByUserIdForUpdate(@Param("userId") String userId);

  Optional<UserPointsAccountJpaEntity> findByClientCode(String clientCode);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT a FROM UserPointsAccountJpaEntity a WHERE a.clientCode = :clientCode")
  Optional<UserPointsAccountJpaEntity> findByClientCodeForUpdate(
      @Param("clientCode") String clientCode);
}
