package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpTransitTicket;
import java.time.LocalDate;
import java.util.Optional;

/** Outbound port for persisting and querying VFP transit tickets (SLE-47). */
public interface TransitTicketPersistencePort {

  VfpTransitTicket save(VfpTransitTicket ticket);

  boolean existsForUserOnDate(String userId, LocalDate ticketDate);

  Optional<VfpTransitTicket> findLatestByUserAndDate(String userId, LocalDate ticketDate);
}
