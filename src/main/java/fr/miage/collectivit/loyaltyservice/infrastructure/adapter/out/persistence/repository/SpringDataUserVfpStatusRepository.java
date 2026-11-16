package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserVfpStatusJpaEntity;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for UserVfpStatusJpaEntity (SLE-45 / SLE-199). */
@Repository
public interface SpringDataUserVfpStatusRepository
    extends JpaRepository<UserVfpStatusJpaEntity, String> {

  Page<UserVfpStatusJpaEntity> findByIsVfpTrue(Pageable pageable);

  List<UserVfpStatusJpaEntity> findByIsVfpTrueOrderByUserIdAsc(Pageable pageable);

  List<UserVfpStatusJpaEntity> findByIsVfpTrueAndUserIdGreaterThanOrderByUserIdAsc(
      String userId, Pageable pageable);
}
