package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.VfpParkingSessionJpaEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for VFP parking sessions. */
public interface SpringDataVfpParkingSessionRepository
    extends JpaRepository<VfpParkingSessionJpaEntity, Long> {

  boolean existsByUserIdAndSessionDate(String userId, LocalDate sessionDate);

  Optional<VfpParkingSessionJpaEntity> findFirstByUserIdAndSessionDateOrderByCreatedAtDesc(
      String userId, LocalDate sessionDate);
}
