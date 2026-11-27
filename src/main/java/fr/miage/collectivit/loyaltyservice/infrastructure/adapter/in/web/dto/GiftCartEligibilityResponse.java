package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record GiftCartEligibilityResponse(
    BigDecimal balance,
    BigDecimal heldPoints,
    BigDecimal availablePoints,
    boolean isVfp,
    Set<UUID> eligibleStoreUids) {}
