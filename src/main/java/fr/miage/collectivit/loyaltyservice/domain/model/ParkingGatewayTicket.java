package fr.miage.collectivit.loyaltyservice.domain.model;

import java.time.Instant;

/**
 * Value object representing the acknowledgment returned by the third-party parking ticket gateway
 * (SLE-78 / SLE-259).
 */
public record ParkingGatewayTicket(
    String status,
    String ticketReference,
    String licensePlate,
    int durationMinutes,
    Instant startedAt,
    Instant expiresAt) {}
