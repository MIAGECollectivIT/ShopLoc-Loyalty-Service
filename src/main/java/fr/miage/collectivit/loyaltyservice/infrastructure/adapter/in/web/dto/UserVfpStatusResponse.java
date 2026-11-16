package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/**
 * REST response DTO representing the VFP qualification status and metrics for a user (SLE-45 /
 * SLE-203).
 */
@Schema(description = "Représentation du statut Very Faithful Person (VFP) d'un usager")
public record UserVfpStatusResponse(
    @Schema(description = "Identifiant de l'usager", example = "USER-12345") String userId,
    @Schema(description = "Indique si l'usager possède le statut actif VFP", example = "true")
        boolean isVfp,
    @Schema(description = "Horodatage d'octroi du statut VFP", example = "2026-11-01T10:00:00Z")
        Instant grantedAt,
    @Schema(description = "Horodatage de révocation du statut VFP", example = "null")
        Instant revokedAt,
    @Schema(
            description =
                "Nombre de transactions validées sur la fenêtre glissante des 15 derniers jours",
            example = "12")
        int currentTransactionCount,
    @Schema(description = "Seuil requis pour être qualifié VFP", example = "10") int threshold,
    @Schema(
            description = "Horodatage de la dernière évaluation métier",
            example = "2026-11-14T15:30:00Z")
        Instant lastEvaluatedAt) {

  public static UserVfpStatusResponse fromDomain(
      UserVfpStatus status, int currentTransactionCount) {
    return new UserVfpStatusResponse(
        status.getUserId(),
        status.isVfp(),
        status.getGrantedAt(),
        status.getRevokedAt(),
        currentTransactionCount,
        UserActivityEvaluation.VFP_ELIGIBILITY_THRESHOLD,
        status.getLastEvaluatedAt());
  }
}
