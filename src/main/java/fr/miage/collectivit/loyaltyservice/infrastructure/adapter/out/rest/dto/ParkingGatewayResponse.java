package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto;

import java.time.Instant;

/** Inbound response payload received from the external parking ticket gateway mock. */
public record ParkingGatewayResponse(
    String status,
    String ticketReference,
    String licensePlate,
    int durationMinutes,
    Instant startedAt,
    Instant expiresAt) {}
