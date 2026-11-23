package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** Request payload to activate a 20-minute VFP free parking session (SLE-78 / SLE-46). */
@Schema(description = "Demande d'activation d'une session de stationnement gratuit VFP")
public record ParkingSessionRequest(
    @Schema(description = "Plaque d'immatriculation du véhicule sélectionné", example = "AA-123-BB")
        @NotBlank(message = "licensePlate must not be blank")
        String licensePlate) {}
