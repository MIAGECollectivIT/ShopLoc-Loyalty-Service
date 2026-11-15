package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserActivityAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateUserActivityUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.UserActivityEvaluationResponse;
import fr.miage.collectivit.loyaltyservice.security.CallerAccessGuard;
import fr.miage.collectivit.loyaltyservice.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Primary inbound web adapter exposing dynamic activity evaluation over a 15-day sliding window for
 * VFP status eligibility detection (SLE-44 / SLE-163).
 */
@Validated
@RestController
@RequestMapping({"/v1/loyalty/users", "/api/v1/loyalty/users"})
@Tag(
    name = "Loyalty Activity Evaluation",
    description =
        "Évaluation dynamique de l'activité d'achat des usagers sur fenêtre glissante pour le statut VFP")
@RequiredArgsConstructor
public class UserActivityEvaluationController {

  private final EvaluateUserActivityUseCase evaluateUserActivityUseCase;
  private final CallerAccessGuard callerAccessGuard;

  @GetMapping("/{userId}/activity-evaluation")
  @RequiresRole({"CLIENT", "ADMIN"})
  @Operation(
      summary = "Consulter l'évaluation d'activité sur fenêtre glissante",
      description =
          "Analyse les transactions validées sur les 15 derniers jours glissants (J-15 à J) sans plafond journalier pour déterminer l'éligibilité au statut VFP (seuil >= 10 transactions).",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Évaluation d'activité calculée et retournée avec succès"),
        @ApiResponse(responseCode = "400", description = "Paramètre userId invalide ou manquant"),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié (jeton Bearer manquant ou invalide)"),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé (l'usager ne peut consulter que sa propre activité)"),
        @ApiResponse(
            responseCode = "502",
            description = "Service Sales distant indisponible ou en erreur")
      })
  public ResponseEntity<UserActivityEvaluationResponse> getActivityEvaluation(
      @Parameter(description = "Identifiant de l'usager", example = "USER-12345")
          @PathVariable
          @NotBlank(message = "userId must not be blank")
          String userId,
      HttpServletRequest request) {
    callerAccessGuard.verifyUserAccess(
        userId,
        request,
        () ->
            new UserActivityAccessDeniedException(
                "Access denied: You can only view your own activity evaluation."));

    UserActivityEvaluation evaluation = evaluateUserActivityUseCase.evaluateActivity(userId);
    return ResponseEntity.ok(UserActivityEvaluationResponse.fromDomain(evaluation));
  }
}
