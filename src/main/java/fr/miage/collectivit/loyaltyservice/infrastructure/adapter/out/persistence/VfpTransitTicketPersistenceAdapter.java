package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpTransitTicket;
import fr.miage.collectivit.loyaltyservice.domain.port.out.TransitTicketPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.VfpTransitTicketJpaEntity;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Outbound persistence adapter implementing TransitTicketPersistencePort. */
@Component
@RequiredArgsConstructor
public class VfpTransitTicketPersistenceAdapter implements TransitTicketPersistencePort {

  private final SpringDataVfpTransitTicketRepository repository;
  private final VfpTransitTicketMapper mapper;

  @Override
  public VfpTransitTicket save(VfpTransitTicket ticket) {
    VfpTransitTicketJpaEntity entity = mapper.toEntity(ticket);
    VfpTransitTicketJpaEntity saved = repository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public boolean existsForUserOnDate(String userId, LocalDate ticketDate) {
    return repository.existsByUserIdAndTicketDate(userId, ticketDate);
  }

  @Override
  public Optional<VfpTransitTicket> findLatestByUserAndDate(String userId, LocalDate ticketDate) {
    return repository
        .findFirstByUserIdAndTicketDateOrderByStartedAtDesc(userId, ticketDate)
        .map(mapper::toDomain);
  }
}
