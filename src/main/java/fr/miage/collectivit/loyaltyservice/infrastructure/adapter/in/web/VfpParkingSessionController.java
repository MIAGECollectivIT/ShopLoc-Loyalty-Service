package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpParkingSession;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ActivateParkingSessionUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.ParkingSessionRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.ParkingSessionResponse;
import fr.miage.collectivit.loyaltyservice.security.CallerAccessGuard;
import fr.miage.collectivit.loyaltyservice.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Primary inbound REST controller handling VFP free parking session activation and acknowledgment
 * (SLE-78 / SLE-261 / SLE-227).
 */
@Slf4j
@Validated
@RestController
@RequestMapping({"/v1/loyalty/vfp/parking-sessions", "/api/v1/loyalty/vfp/parking-sessions"})
@Tag(
    name = "VFP Parking Sessions",
    description = "Activation et acquittement des sessions de stationnement gratuit VFP (20 min)")
@RequiredArgsConstructor
public class VfpParkingSessionController {

  private final ActivateParkingSessionUseCase activateParkingSessionUseCase;
  private final CallerAccessGuard callerAccessGuard;

  @PostMapping
  @RequiresRole({"CLIENT", "ADMIN"})
  @Operation(
      summary = "Activer et acquitter une session de stationnement gratuit VFP",
      description =
          "Valide le statut VFP de l'usager connecté, vérifie la plaque d'immatriculation, interroge la passerelle tiers de stationnement et retourne l'acquittement structuré de la session de 20 minutes.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Session de stationnement prise en compte et acquittée",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ParkingSessionResponse.class))),
        @ApiResponse(
            responseCode = "400",
            description = "Plaque d'immatriculation invalide ou non enregistrée pour cet usager",
            content = @Content),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié (jeton Bearer manquant ou invalide)",
            content = @Content),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé (statut VFP inactif ou rôle insuffisant)",
            content = @Content),
        @ApiResponse(
            responseCode = "409",
            description = "Avantage stationnement déjà consommé aujourd'hui",
            content = @Content),
        @ApiResponse(
            responseCode = "502",
            description = "Service tiers de stationnement ou Users Service indisponible",
            content = @Content)
      })
  public ResponseEntity<ParkingSessionResponse> activateParkingSession(
      @Valid @RequestBody ParkingSessionRequest requestDto, HttpServletRequest request) {
    String userId =
        callerAccessGuard.resolveAuthenticatedUserId(
            request, () -> new IllegalArgumentException("Authenticated user identity is missing"));
    VfpParkingSession session =
        activateParkingSessionUseCase.activateSession(userId, requestDto.licensePlate());
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ParkingSessionResponse.fromDomain(session));
  }
}
