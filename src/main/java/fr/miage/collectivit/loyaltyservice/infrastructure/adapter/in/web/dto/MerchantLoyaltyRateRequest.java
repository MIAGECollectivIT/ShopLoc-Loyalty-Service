package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Requête de configuration du barème de conversion euro/points pour un commerce. */
public record MerchantLoyaltyRateRequest(
    @NotNull(message = "Le montant en euros est obligatoire")
        @DecimalMin(
            value = "0.01",
            inclusive = true,
            message = "Le montant en euros doit être strictement supérieur à 0")
        @Digits(
            integer = 8,
            fraction = 2,
            message = "Le montant en euros doit comporter au maximum 2 décimales")
        BigDecimal euroAmount,
    @NotNull(message = "Le nombre de points attribués est obligatoire")
        @DecimalMin(
            value = "0.5",
            inclusive = true,
            message = "Le nombre de points doit être supérieur ou égal à 0.5")
        @Digits(
            integer = 8,
            fraction = 1,
            message = "Le nombre de points doit comporter au maximum 1 décimale (par pas de 0.5)")
        BigDecimal pointsReward) {}
