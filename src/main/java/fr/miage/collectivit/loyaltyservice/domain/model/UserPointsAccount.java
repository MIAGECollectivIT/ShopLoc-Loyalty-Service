package fr.miage.collectivit.loyaltyservice.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

public record UserPointsAccount(
    Long id,
    String userId,
    String clientCode,
    BigDecimal balance,
    Instant createdAt,
    Instant updatedAt) {

  public UserPointsAccount {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
    if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("balance must not be negative");
    }
    userId = userId.trim();
    balance = balance.setScale(1, RoundingMode.HALF_UP);
    Instant fallbackNow = Instant.now();
    if (createdAt == null) {
      createdAt = fallbackNow;
    }
    if (updatedAt == null) {
      updatedAt = createdAt;
    }
  }

  /**
   * Creates a default zero-balance domain representation for a user without an existing account.
   */
  public static UserPointsAccount empty(String userId, Instant now) {
    Instant effectiveNow = now != null ? now : Instant.now();
    return new UserPointsAccount(
        null,
        userId,
        null,
        BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY),
        effectiveNow,
        effectiveNow);
  }
}
