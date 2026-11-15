package fr.miage.collectivit.loyaltyservice.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;

public record PointsTransaction(
    Long id,
    String transactionReference,
    String userId,
    Long storeId,
    String storeName,
    PointsTransactionType type,
    BigDecimal points,
    BigDecimal balanceAfter,
    String sourceType,
    String description,
    Instant createdAt) {

  public PointsTransaction {
    if (transactionReference == null || transactionReference.isBlank()) {
      throw new IllegalArgumentException("transactionReference must not be blank");
    }
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
    Objects.requireNonNull(type, "type must not be null");
    if (points == null || points.compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("points must be strictly positive");
    }
    if (balanceAfter == null || balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("balanceAfter must not be negative");
    }
    transactionReference = transactionReference.trim();
    userId = userId.trim();
    points = points.setScale(1, RoundingMode.HALF_UP);
    balanceAfter = balanceAfter.setScale(1, RoundingMode.HALF_UP);
    if (createdAt == null) {
      createdAt = Instant.now();
    }
  }
}
