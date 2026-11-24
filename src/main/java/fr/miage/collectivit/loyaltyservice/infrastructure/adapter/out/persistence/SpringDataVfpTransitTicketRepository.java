package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.VfpTransitTicketJpaEntity;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data repository for VfpTransitTicketJpaEntity. */
@Repository
public interface SpringDataVfpTransitTicketRepository
    extends JpaRepository<VfpTransitTicketJpaEntity, Long> {

  boolean existsByUserIdAndTicketDate(String userId, LocalDate ticketDate);

  Optional<VfpTransitTicketJpaEntity> findFirstByUserIdAndTicketDateOrderByStartedAtDesc(
      String userId, LocalDate ticketDate);
}
