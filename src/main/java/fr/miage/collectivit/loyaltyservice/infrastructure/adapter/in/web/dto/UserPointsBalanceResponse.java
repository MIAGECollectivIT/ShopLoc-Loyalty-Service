package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsBalance;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Solde de points de fidélité et historique des mouvements associés")
@SuppressWarnings("PMD.LawOfDemeter")
public record UserPointsBalanceResponse(
    @Schema(
            description = "UUID public de l'utilisateur",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String userId,
    @Schema(description = "Code client commerçant à 7 caractères (si connu)", example = "CL95001")
        String clientCode,
    @Schema(description = "Solde actuel de points disponibles", example = "152.5")
        BigDecimal balance,
    @Schema(
            description = "Alias numérique du solde de points pour rétrocompatibilité",
            example = "152.5")
        BigDecimal points,
    @Schema(description = "Dernière date de mise à jour du solde", example = "2026-11-15T10:15:30Z")
        Instant updatedAt,
    @Schema(description = "Historique des mouvements de points, du plus récent au plus ancien")
        List<PointsTransactionResponse> transactions) {

  /** Maps a domain {@link UserPointsBalance} to a {@link UserPointsBalanceResponse}. */
  public static UserPointsBalanceResponse fromDomain(UserPointsBalance domain) {
    List<PointsTransactionResponse> txResponses =
        domain.transactions().stream().map(PointsTransactionResponse::fromDomain).toList();
    return new UserPointsBalanceResponse(
        domain.userId(),
        domain.clientCode(),
        domain.balance(),
        domain.balance(),
        domain.updatedAt(),
        txResponses);
  }
}
