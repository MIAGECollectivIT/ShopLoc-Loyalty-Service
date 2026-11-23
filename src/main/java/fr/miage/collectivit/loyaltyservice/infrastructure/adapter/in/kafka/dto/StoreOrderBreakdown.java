package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record StoreOrderBreakdown(
    @NotNull UUID storeUid, String storeName, @NotNull @DecimalMin("0.01") BigDecimal amount) {}
