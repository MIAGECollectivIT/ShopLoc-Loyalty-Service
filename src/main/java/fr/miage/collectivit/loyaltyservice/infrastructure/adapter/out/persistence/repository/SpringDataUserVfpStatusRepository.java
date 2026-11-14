package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserVfpStatusJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository for UserVfpStatusJpaEntity (SLE-45 / SLE-199). */
@Repository
public interface SpringDataUserVfpStatusRepository
    extends JpaRepository<UserVfpStatusJpaEntity, String> {

  Page<UserVfpStatusJpaEntity> findByIsVfpTrue(Pageable pageable);
}
