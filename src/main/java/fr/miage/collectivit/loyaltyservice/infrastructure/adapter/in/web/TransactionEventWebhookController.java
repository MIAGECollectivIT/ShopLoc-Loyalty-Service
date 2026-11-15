package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateUserActivityUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.TransactionApprovedEventRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.UserVfpStatusResponse;
import fr.miage.collectivit.loyaltyservice.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound webhook controller triggering immediate VFP status re-evaluation upon transaction
 * completion (SLE-45 / SLE-201).
 */
@Slf4j
@Validated
@RestController
@RequestMapping({"/v1/loyalty/events", "/api/v1/loyalty/events"})
@Tag(
    name = "Loyalty Events",
    description = "Réception et déclenchement d'événements de fidélité et transactions")
@RequiredArgsConstructor
public class TransactionEventWebhookController {

  private final EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase;
  private final EvaluateUserActivityUseCase evaluateUserActivityUseCase;

  @PostMapping("/transaction-approved")
  @RequiresRole({"CLIENT", "MERCHANT", "ADMIN"})
  @Operation(
      summary = "Notifier une transaction approuvée pour réévaluer le statut VFP",
      description =
          "Déclenche le recalcul immédiat de l'activité sur 15 jours glissants et applique les transitions VFP (octroi, maintien, révocation).",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Réévaluation effectuée et statut VFP mis à jour"),
        @ApiResponse(responseCode = "400", description = "Données d'événement invalides"),
        @ApiResponse(responseCode = "401", description = "Non authentifié (jeton JWT requis)")
      })
  public ResponseEntity<UserVfpStatusResponse> onTransactionApproved(
      @Valid @RequestBody TransactionApprovedEventRequest request) {
    String userId = request.userId().trim();
    if (log.isInfoEnabled()) {
      log.info(
          "Triggering VFP re-evaluation via webhook event for user {} (transactionId={})",
          userId,
          request.transactionId());
    }

    UserVfpStatus status = evaluateAndApplyVfpStatusUseCase.evaluateAndApply(userId);
    UserActivityEvaluation evaluation = evaluateUserActivityUseCase.evaluateActivity(userId);

    return ResponseEntity.ok(
        UserVfpStatusResponse.fromDomain(status, evaluation.getTransactionCount()));
  }
}
