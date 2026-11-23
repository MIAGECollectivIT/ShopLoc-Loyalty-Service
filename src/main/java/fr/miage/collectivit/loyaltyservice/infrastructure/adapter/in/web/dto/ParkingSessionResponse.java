package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpParkingSession;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * Structured acknowledgment DTO returned to the client upon parking session confirmation (SLE-78 /
 * SLE-261).
 */
@Schema(description = "Acquittement de prise en compte de la session de stationnement gratuit")
public record ParkingSessionResponse(
    @Schema(description = "Statut de confirmation de la session", example = "CONFIRMED")
        String status,
    @Schema(
            description = "Référence unique du ticket d'acquittement",
            example = "PARK-123e4567-e89b-12d3-a456-426614174000")
        String ticketReference,
    @Schema(description = "Plaque d'immatriculation couverte", example = "AA-123-BB")
        String licensePlate,
    @Schema(
            description = "Horodatage de début de prise en compte",
            example = "2026-11-17T14:30:00Z")
        Instant startedAt,
    @Schema(description = "Horodatage d'expiration (+20 minutes)", example = "2026-11-17T14:50:00Z")
        Instant expiresAt) {

  public static ParkingSessionResponse fromDomain(VfpParkingSession session) {
    return new ParkingSessionResponse(
        session.getStatus(),
        session.getTicketReference(),
        session.getLicensePlate(),
        session.getStartedAt(),
        session.getExpiresAt());
  }
}
