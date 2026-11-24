package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpTransitTicket;
import java.time.Instant;
import java.util.UUID;

public record TransitTicketResponse(
    UUID uid,
    String userId,
    String ticketReference,
    String ticketCode,
    String networkName,
    String status,
    Instant startedAt,
    Instant expiresAt) {

  public static TransitTicketResponse fromDomain(VfpTransitTicket domain) {
    return new TransitTicketResponse(
        domain.getUid(),
        domain.getUserId(),
        domain.getTicketReference(),
        domain.getTicketCode(),
        domain.getNetworkName(),
        domain.getStatus(),
        domain.getStartedAt(),
        domain.getExpiresAt());
  }
}
