package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto;

import java.time.Instant;

public record TransitGatewayResponse(
    String status,
    String ticketReference,
    String ticketCode,
    String networkName,
    Integer durationMinutes,
    Instant startedAt,
    Instant expiresAt) {}
