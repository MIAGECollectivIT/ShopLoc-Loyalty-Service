package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto;

/** Outbound request payload sent to the external parking ticket gateway mock. */
public record ParkingGatewayRequest(String userId, String licensePlate, int durationMinutes) {}
