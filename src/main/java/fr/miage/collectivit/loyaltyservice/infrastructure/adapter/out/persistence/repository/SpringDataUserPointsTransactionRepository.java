package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsTransactionJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for {@link UserPointsTransactionJpaEntity} (SLE-38 / SLE-176). */
@Repository
public interface SpringDataUserPointsTransactionRepository
    extends JpaRepository<UserPointsTransactionJpaEntity, Long> {

  List<UserPointsTransactionJpaEntity> findByUserIdOrderByCreatedAtDescIdDesc(String userId);

  boolean existsByTransactionReference(String transactionReference);
}
