package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CreateLoyaltyHoldRequest(
    @NotBlank(message = "orderReference is required") String orderReference,
    @NotBlank(message = "userId is required") String userId,
    @NotNull(message = "expiresAt is required") Instant expiresAt,
    @NotEmpty(message = "items must not be empty") List<@Valid HoldItemRequest> items) {

  public record HoldItemRequest(
      @NotNull(message = "storeUid is required") UUID storeUid,
      @NotNull(message = "giftUid is required") UUID giftUid,
      @NotNull(message = "quantity is required") @Min(1) Integer quantity) {}
}
