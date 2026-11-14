package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * REST response DTO for user activity evaluation over a 15-day sliding window (SLE-44 / SLE-163).
 */
@Schema(
    description =
        "Synthèse d'activité d'achat d'un usager sur fenêtre glissante de 15 jours pour le statut VFP")
public record UserActivityEvaluationResponse(
    @Schema(description = "Identifiant de l'usager", example = "USER-12345") String userId,
    @Schema(description = "Nombre total de transactions validées sur la fenêtre", example = "7")
        int transactionCount,
    @Schema(description = "Seuil d'éligibilité pour le statut VFP", example = "10") int threshold,
    @Schema(
            description = "Indique si l'usager a atteint ou dépassé le seuil pour le statut VFP",
            example = "false")
        boolean isEligibleVfp,
    @Schema(description = "Début de la fenêtre glissante de 15 jours (UTC)") Instant windowStart,
    @Schema(description = "Fin de la fenêtre glissante de 15 jours (UTC)") Instant windowEnd) {

  public static UserActivityEvaluationResponse fromDomain(UserActivityEvaluation domain) {
    if (domain == null) {
      return null;
    }
    return new UserActivityEvaluationResponse(
        domain.getUserId(),
        domain.getTransactionCount(),
        domain.getThreshold(),
        domain.isEligibleVfp(),
        domain.getWindowStart(),
        domain.getWindowEnd());
  }
}
