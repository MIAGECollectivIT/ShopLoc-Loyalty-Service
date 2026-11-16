package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Request DTO for adjusting gift stock (+/- delta) (SLE-193). */
public record GiftStockAdjustRequest(
    @NotNull(message = "Delta is mandatory")
        @Min(value = -100_000, message = "Delta cannot be less than -100,000")
        @Max(value = 100_000, message = "Delta cannot exceed 100,000")
        Integer delta) {}
