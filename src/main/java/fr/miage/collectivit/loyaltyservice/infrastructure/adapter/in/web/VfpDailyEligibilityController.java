package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpDailyEligibility;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetVfpDailyEligibilityQuery;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.VfpDailyEligibilityResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Primary inbound REST controller exposing daily VFP institutional benefit eligibility (SLE-48 /
 * SLE-233).
 */
@RestController
@RequestMapping({"/v1/loyalty/vfp/daily-eligibility", "/api/v1/loyalty/vfp/daily-eligibility"})
@Tag(
    name = "VFP Daily Eligibility",
    description = "Vérification d'éligibilité journalière aux avantages VFP")
@RequiredArgsConstructor
public class VfpDailyEligibilityController {

  private final GetVfpDailyEligibilityQuery getVfpDailyEligibilityQuery;
  private final CallerAccessGuard callerAccessGuard;

  @GetMapping
  @RequiresRole({"CLIENT", "ADMIN"})
  @Operation(
      summary = "Consulter l'éligibilité journalière aux avantages VFP",
      description =
          "Indique si l'usager connecté est VFP actif, s'il a déjà consommé un avantage aujourd'hui (heure de Paris) et renvoie la session de stationnement active le cas échéant.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Éligibilité journalière retournée avec succès",
            content =
                @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = VfpDailyEligibilityResponse.class))),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié (jeton Bearer manquant ou invalide)",
            content = @Content)
      })
  public ResponseEntity<VfpDailyEligibilityResponse> getDailyEligibility(
      HttpServletRequest request) {
    String userId =
        callerAccessGuard.resolveAuthenticatedUserId(
            request, () -> new IllegalArgumentException("Authenticated user identity is missing"));
    VfpDailyEligibility eligibility = getVfpDailyEligibilityQuery.getDailyEligibility(userId);
    return ResponseEntity.ok(VfpDailyEligibilityResponse.fromDomain(eligibility));
  }
}
