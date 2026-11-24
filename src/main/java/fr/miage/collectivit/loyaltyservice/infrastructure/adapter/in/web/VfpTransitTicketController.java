package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpTransitTicket;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ActivateTransitTicketUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.TransitTicketResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Primary inbound REST controller handling VFP public transit ticket activation (SLE-47). */
@Slf4j
@Validated
@RestController
@RequestMapping({"/v1/loyalty/vfp/transit-tickets", "/api/v1/loyalty/vfp/transit-tickets"})
@Tag(
    name = "VFP Transit Tickets",
    description =
        "Activation et délivrance des titres de transport en commun gratuit pour usagers VFP")
@RequiredArgsConstructor
public class VfpTransitTicketController {

  private final ActivateTransitTicketUseCase activateTransitTicketUseCase;
  private final CallerAccessGuard callerAccessGuard;

  @PostMapping
  @RequiresRole({"CLIENT", "ADMIN"})
  @Operation(
      summary = "Activer et obtenir un titre de transport gratuit VFP",
      description =
          "Valide le statut VFP de l'usager connecté, vérifie la règle de non-cumul journalier, contacte la passerelle de transport et délivre le titre numérique avec code de contrôle.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "201",
            description = "Titre de transport gratuit généré avec succès",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = TransitTicketResponse.class))),
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
            description =
                "Avantage institutionnel déjà consommé aujourd'hui (stationnement ou transport)",
            content = @Content),
        @ApiResponse(
            responseCode = "502",
            description = "Service tiers de transport indisponible",
            content = @Content)
      })
  public ResponseEntity<TransitTicketResponse> activateTransitTicket(HttpServletRequest request) {
    String userId =
        callerAccessGuard.resolveAuthenticatedUserId(
            request, () -> new IllegalArgumentException("Authenticated user identity is missing"));
    VfpTransitTicket ticket = activateTransitTicketUseCase.activateTicket(userId);
    return ResponseEntity.status(HttpStatus.CREATED).body(TransitTicketResponse.fromDomain(ticket));
  }
}
