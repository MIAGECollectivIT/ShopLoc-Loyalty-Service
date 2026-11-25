package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import fr.miage.collectivit.loyaltyservice.domain.port.in.RedeemPhysicalGiftsCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.RedeemPhysicalGiftsCommand.GiftSelectionItem;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

@Schema(description = "Requête de remise physique de cadeaux fidélité contre des points (SLE-43)")
public record PhysicalGiftRedemptionRequest(
    @Size(max = 64, message = "customerId must not exceed 64 characters")
        @Schema(
            description = "Identifiant public UUID du client",
            example = "550e8400-e29b-41d4-a716-446655440000")
        String customerId,
    @Size(max = 32, message = "clientCode must not exceed 32 characters")
        @Schema(description = "Code fidélité client à 7 caractères", example = "CL95001")
        String clientCode,
    @NotEmpty(message = "At least one gift item must be selected")
        @Size(max = 100, message = "At most 100 distinct gift items can be redeemed at once")
        @Schema(description = "Liste des lots remis avec leur quantité")
        List<@Valid PhysicalGiftRedemptionItemRequest> items,
    @Size(max = 50, message = "idempotencyKey must not exceed 50 characters")
        @Schema(
            description = "Clé d'idempotence optionnelle pour éviter les doubles débits",
            example = "550e8400-e29b-41d4-a716-446655440099")
        String idempotencyKey) {

  public PhysicalGiftRedemptionRequest(
      String customerId, String clientCode, List<PhysicalGiftRedemptionItemRequest> items) {
    this(customerId, clientCode, items, null);
  }

  @AssertTrue(message = "Either customerId or clientCode must be provided")
  public boolean hasCustomerIdentifier() {
    return (customerId != null && !customerId.isBlank())
        || (clientCode != null && !clientCode.isBlank());
  }

  public RedeemPhysicalGiftsCommand toCommand() {
    List<GiftSelectionItem> domainItems =
        items.stream().map(i -> new GiftSelectionItem(i.giftUid(), i.quantity())).toList();
    return new RedeemPhysicalGiftsCommand(customerId, clientCode, domainItems, idempotencyKey);
  }

  @Schema(description = "Ligne d'article cadeau sélectionné lors de la remise en boutique")
  public record PhysicalGiftRedemptionItemRequest(
      @NotNull(message = "giftUid is required")
          @Schema(
              description = "Identifiant UUID du cadeau",
              example = "550e8400-e29b-41d4-a716-446655440001")
          UUID giftUid,
      @NotNull(message = "quantity is required")
          @Min(value = 1, message = "quantity must be at least 1")
          @Max(value = 100, message = "quantity must not exceed 100")
          @Schema(description = "Quantité remise au client", example = "1")
          Integer quantity) {}
}
