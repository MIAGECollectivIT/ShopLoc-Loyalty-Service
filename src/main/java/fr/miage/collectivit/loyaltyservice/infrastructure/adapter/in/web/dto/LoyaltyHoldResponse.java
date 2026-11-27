package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LoyaltyHoldResponse(
    UUID uid,
    String orderReference,
    String userId,
    BigDecimal totalPoints,
    String status,
    Instant expiresAt,
    List<HoldItemResponse> items) {

  public record HoldItemResponse(
      UUID storeUid, UUID giftUid, String giftName, int unitPointsCost, int quantity) {}
}
