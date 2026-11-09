package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Request DTO for configuring the euro-to-points loyalty conversion rate for a store. */
public record MerchantLoyaltyRateRequest(
    @NotNull(message = "Euro amount is mandatory")
        @DecimalMin(
            value = "0.01",
            inclusive = true,
            message = "Euro amount must be strictly greater than 0")
        @Digits(
            integer = 8,
            fraction = 2,
            message = "Euro amount must have at most 2 decimal places")
        BigDecimal euroAmount,
    @NotNull(message = "Points reward is mandatory")
        @DecimalMin(
            value = "0.5",
            inclusive = true,
            message = "Points reward must be greater than or equal to 0.5")
        @Digits(
            integer = 8,
            fraction = 1,
            message = "Points reward must have at most 1 decimal place (in steps of 0.5)")
        BigDecimal pointsReward) {}
