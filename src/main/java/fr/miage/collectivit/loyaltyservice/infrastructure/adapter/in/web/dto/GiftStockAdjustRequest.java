package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.NotNull;

/** Request DTO for adjusting gift stock (+/- delta) (SLE-193). */
public record GiftStockAdjustRequest(@NotNull(message = "Delta is mandatory") Integer delta) {}
