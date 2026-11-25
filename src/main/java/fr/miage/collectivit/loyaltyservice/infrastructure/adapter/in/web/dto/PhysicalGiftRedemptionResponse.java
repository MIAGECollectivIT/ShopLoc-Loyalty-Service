package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.model.PhysicalGiftRedemptionResult;
import fr.miage.collectivit.loyaltyservice.domain.model.PhysicalGiftRedemptionResult.RedeemedGiftItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Reçu de remise physique de cadeaux fidélité (SLE-43)")
public record PhysicalGiftRedemptionResponse(
    @Schema(description = "Référence unique de la transaction de débit")
        String transactionReference,
    @Schema(description = "Identifiant UUID du commerce") UUID storeUid,
    @Schema(description = "Nom du commerce") String storeName,
    @Schema(description = "Identifiant client débité") String customerId,
    @Schema(description = "Code fidélité du client") String clientCode,
    @Schema(description = "Total de points débités") BigDecimal pointsDeducted,
    @Schema(description = "Solde de points restant après débit") BigDecimal remainingBalance,
    @Schema(description = "Horodatage de l'opération") Instant redeemedAt,
    @Schema(description = "Détail des cadeaux remis") List<RedeemedGiftItemResponse> items) {

  public static PhysicalGiftRedemptionResponse fromDomain(PhysicalGiftRedemptionResult result) {
    List<RedeemedGiftItemResponse> itemResponses =
        result.items().stream().map(RedeemedGiftItemResponse::fromDomain).toList();
    return new PhysicalGiftRedemptionResponse(
        result.transactionReference(),
        result.storeUid(),
        result.storeName(),
        result.customerId(),
        result.clientCode(),
        result.pointsDeducted(),
        result.remainingBalance(),
        result.redeemedAt(),
        itemResponses);
  }

  @Schema(description = "Détail d'un lot cadeau remis")
  public record RedeemedGiftItemResponse(
      UUID giftUid, String name, int unitPointsCost, int quantity, int totalPointsCost) {

    public static RedeemedGiftItemResponse fromDomain(RedeemedGiftItem item) {
      return new RedeemedGiftItemResponse(
          item.giftUid(),
          item.name(),
          item.unitPointsCost(),
          item.quantity(),
          item.totalPointsCost());
    }
  }
}
