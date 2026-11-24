package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpTransitTicket;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.VfpTransitTicketJpaEntity;
import org.springframework.stereotype.Component;

/** Mapper translating between domain VfpTransitTicket and JPA entity. */
@Component
public class VfpTransitTicketMapper {

  public VfpTransitTicket toDomain(VfpTransitTicketJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new VfpTransitTicket(
        entity.getId(),
        entity.getUid(),
        entity.getUserId(),
        entity.getTicketReference(),
        entity.getTicketCode(),
        entity.getNetworkName(),
        entity.getStatus(),
        entity.getStartedAt(),
        entity.getExpiresAt(),
        entity.getTicketDate(),
        entity.getCreatedAt());
  }

  public VfpTransitTicketJpaEntity toEntity(VfpTransitTicket domain) {
    if (domain == null) {
      return null;
    }
    return new VfpTransitTicketJpaEntity(
        domain.getId(),
        domain.getUid(),
        domain.getUserId(),
        domain.getTicketReference(),
        domain.getTicketCode(),
        domain.getNetworkName(),
        domain.getStatus(),
        domain.getStartedAt(),
        domain.getExpiresAt(),
        domain.getTicketDate(),
        domain.getCreatedAt());
  }
}
