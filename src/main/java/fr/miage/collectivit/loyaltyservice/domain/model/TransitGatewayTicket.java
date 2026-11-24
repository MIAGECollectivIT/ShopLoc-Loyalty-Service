package fr.miage.collectivit.loyaltyservice.domain.model;

import java.time.Instant;

/**
 * Third-party response DTO when acquiring a public transit ticket from the external transport
 * authority.
 */
public record TransitGatewayTicket(
    String ticketReference,
    String ticketCode,
    String networkName,
    int durationMinutes,
    Instant startedAt,
    Instant expiresAt,
    String status) {}
