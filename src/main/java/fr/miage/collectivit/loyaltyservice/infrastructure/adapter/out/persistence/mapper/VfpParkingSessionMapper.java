package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpParkingSession;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.VfpParkingSessionJpaEntity;
import org.springframework.stereotype.Component;

/** Mapper between {@link VfpParkingSession} domain model and {@link VfpParkingSessionJpaEntity}. */
@Component
public class VfpParkingSessionMapper {

  public VfpParkingSession toDomain(VfpParkingSessionJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    return new VfpParkingSession(
        entity.getId(),
        entity.getUserId(),
        entity.getLicensePlate(),
        entity.getTicketReference(),
        entity.getStatus(),
        entity.getStartedAt(),
        entity.getExpiresAt(),
        entity.getSessionDate(),
        entity.getCreatedAt());
  }

  public VfpParkingSessionJpaEntity toJpaEntity(VfpParkingSession domain) {
    if (domain == null) {
      return null;
    }
    return VfpParkingSessionJpaEntity.builder()
        .id(domain.getId())
        .userId(domain.getUserId())
        .licensePlate(domain.getLicensePlate())
        .ticketReference(domain.getTicketReference())
        .status(domain.getStatus())
        .startedAt(domain.getStartedAt())
        .expiresAt(domain.getExpiresAt())
        .sessionDate(domain.getSessionDate())
        .createdAt(domain.getCreatedAt())
        .build();
  }
}
