package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserActivityAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateUserActivityUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserVfpStatusQuery;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.UserVfpStatusResponse;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Primary inbound REST controller exposing consultation of a user's VFP qualification status and
 * metrics (SLE-45 / SLE-203).
 */
@Slf4j
@Validated
@RestController
@RequestMapping({"/v1/loyalty/users", "/api/v1/loyalty/users"})
@Tag(
    name = "User VFP Status",
    description = "Consultation et suivi du statut Very Faithful Person (VFP) des usagers")
@RequiredArgsConstructor
public class UserVfpStatusController {

  private final GetUserVfpStatusQuery getUserVfpStatusQuery;
  private final EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase;
  private final EvaluateUserActivityUseCase evaluateUserActivityUseCase;
  private final CallerAccessGuard callerAccessGuard;

  @GetMapping("/{userId}/vfp-status")
  @RequiresRole({"CLIENT", "ADMIN"})
  @Operation(
      summary = "Consulter le statut VFP actif d'un utilisateur",
      description =
          "Renvoie le statut Very Faithful Person (VFP) actif, la date d'octroi, la date de révocation éventuelle, et le nombre de transactions retenues sur la fenêtre glissante des 15 derniers jours.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Statut VFP retourné avec succès"),
        @ApiResponse(responseCode = "400", description = "Identifiant userId invalide ou manquant"),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié (jeton Bearer manquant ou invalide)"),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé (l'usager ne peut consulter que son propre statut VFP)"),
        @ApiResponse(
            responseCode = "502",
            description = "Service Sales distant indisponible ou en erreur")
      })
  public ResponseEntity<UserVfpStatusResponse> getVfpStatus(
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
                "Access denied: You can only view your own VFP status."));

    String cleanUserId = userId.trim();
    UserActivityEvaluation evaluation = evaluateUserActivityUseCase.evaluateActivity(cleanUserId);

    UserVfpStatus status =
        getUserVfpStatusQuery
            .getStatus(cleanUserId)
            .orElseGet(() -> evaluateAndApplyVfpStatusUseCase.evaluateAndApply(cleanUserId));

    return ResponseEntity.ok(
        UserVfpStatusResponse.fromDomain(status, evaluation.getTransactionCount()));
  }
}
