package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Schema(description = "Mouvement d'historique sur le solde de points de fidélité")
public record PointsTransactionResponse(
    @Schema(
            description = "Identifiant public UUID de la transaction",
            example = "550e8400-e29b-41d4-a716-446655440000")
        UUID uid,
    @Schema(description = "Référence unique de la transaction", example = "PTS-EARN-2026-0001")
        String transactionReference,
    @Schema(
            description = "Identifiant UUID du commerce associé",
            example = "550e8400-e29b-41d4-a716-446655440001")
        UUID storeUid,
    @Schema(description = "Nom du commerce associé", example = "Boulangerie du Centre")
        String storeName,
    @Schema(description = "Type de mouvement (EARN = gain, REDEEM = dépense)", example = "EARN")
        PointsTransactionType type,
    @Schema(
            description = "Nombre de points crédités ou débités (toujours positif)",
            example = "12.5")
        BigDecimal points,
    @Schema(description = "Solde de points après l'opération", example = "152.5")
        BigDecimal balanceAfter,
    @Schema(
            description = "Origine métier de l'opération (PURCHASE, CHECKOUT, etc.)",
            example = "PURCHASE")
        String sourceType,
    @Schema(description = "Libellé descriptif de l'opération", example = "Achat en boutique")
        String description,
    @Schema(description = "Date de comptabilisation", example = "2026-11-15T10:15:30Z")
        Instant createdAt) {

  /** Maps a domain {@link PointsTransaction} to a {@link PointsTransactionResponse}. */
  public static PointsTransactionResponse fromDomain(PointsTransaction tx) {
    return new PointsTransactionResponse(
        tx.uid(),
        tx.transactionReference(),
        tx.storeUid(),
        tx.storeName(),
        tx.type(),
        tx.points(),
        tx.balanceAfter(),
        tx.sourceType(),
        tx.description(),
        tx.createdAt());
  }
}
