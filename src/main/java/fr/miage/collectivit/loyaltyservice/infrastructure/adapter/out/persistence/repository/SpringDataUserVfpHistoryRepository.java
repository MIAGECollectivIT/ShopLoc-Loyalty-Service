package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserVfpHistoryJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for UserVfpHistoryJpaEntity audit entries (SLE-45 / SLE-199). */
@Repository
public interface SpringDataUserVfpHistoryRepository
    extends JpaRepository<UserVfpHistoryJpaEntity, Long> {

  List<UserVfpHistoryJpaEntity> findByUserIdOrderByCreatedAtDesc(String userId);
}
