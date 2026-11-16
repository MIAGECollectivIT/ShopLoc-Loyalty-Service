package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpProgress;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * REST response DTO representing VFP progression metrics, maintenance requirements, and countdown
 * to status expiration (SLE-51 / SLE-238).
 */
@Schema(
    description =
        "Indicateurs de progression, de maintien et compte à rebours avant expiration du statut VFP")
public record UserVfpProgressResponse(
    @Schema(description = "Identifiant de l'usager", example = "USER-12345") String userId,
    @Schema(
            description = "Indique si l'usager possède actuellement le statut VFP actif",
            example = "true")
        boolean isVfp,
    @Schema(
            description = "Nombre de transactions validées sur les 15 derniers jours",
            example = "8")
        int currentTransactionCount,
    @Schema(description = "Seuil d'éligibilité pour le statut VFP", example = "10") int threshold,
    @Schema(
            description =
                "Nombre d'achats restants pour atteindre le statut VFP (pour usager non-VFP)",
            example = "2")
        int remainingTransactionsToQualify,
    @Schema(
            description =
                "Nombre d'achats à effectuer pour maintenir le statut lors des prochains jours (pour VFP actif)",
            example = "1")
        int remainingTransactionsToMaintain,
    @Schema(
            description =
                "Nombre de jours restants avant la perte potentielle du statut sans nouvel achat",
            example = "4")
        Long daysRemainingBeforeRevocation,
    @Schema(
            description =
                "Nombre d'heures restantes avant la perte potentielle du statut sans nouvel achat",
            example = "12")
        Long hoursRemainingBeforeRevocation,
    @Schema(
            description = "Date estimée de révocation du statut si aucun achat n'est réalisé",
            example = "2026-11-20T14:30:00Z")
        Instant estimatedRevocationDate,
    @Schema(
            description =
                "Nombre de transactions sortant de la fenêtre glissante à la date de révocation",
            example = "3")
        int expiringTransactionsCount,
    @Schema(description = "Début de la fenêtre glissante de 15 jours") Instant windowStart,
    @Schema(description = "Fin de la fenêtre glissante de 15 jours") Instant windowEnd,
    @Schema(description = "Horodatage du calcul de progression") Instant evaluatedAt) {

  public static UserVfpProgressResponse fromDomain(VfpProgress progress) {
    if (progress == null) {
      return null;
    }
    return new UserVfpProgressResponse(
        progress.getUserId(),
        progress.isVfp(),
        progress.getCurrentTransactionCount(),
        progress.getThreshold(),
        progress.getRemainingTransactionsToQualify(),
        progress.getRemainingTransactionsToMaintain(),
        progress.getDaysRemainingBeforeRevocation(),
        progress.getHoursRemainingBeforeRevocation(),
        progress.getEstimatedRevocationDate(),
        progress.getExpiringTransactionsCount(),
        progress.getWindowStart(),
        progress.getWindowEnd(),
        progress.getEvaluatedAt());
  }
}
