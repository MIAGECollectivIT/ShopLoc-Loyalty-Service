package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsAccountJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link UserPointsAccountJpaEntity} (SLE-38 / SLE-176). */
@Repository
public interface SpringDataUserPointsAccountRepository
    extends JpaRepository<UserPointsAccountJpaEntity, Long> {

  Optional<UserPointsAccountJpaEntity> findByUserId(String userId);

  Optional<UserPointsAccountJpaEntity> findByClientCode(String clientCode);
}
