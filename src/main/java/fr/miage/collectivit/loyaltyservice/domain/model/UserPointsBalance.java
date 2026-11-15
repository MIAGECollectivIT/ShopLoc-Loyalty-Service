package fr.miage.collectivit.loyaltyservice.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("PMD.UnusedAssignment")
public record UserPointsBalance(
    String userId,
    String clientCode,
    BigDecimal balance,
    Instant updatedAt,
    List<PointsTransaction> transactions) {

  public UserPointsBalance {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank");
    }
    if (balance == null || balance.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("balance must not be negative");
    }
    userId = userId.trim();
    balance = balance.setScale(1, RoundingMode.HALF_UP);
    if (updatedAt == null) {
      updatedAt = Instant.now();
    }
    transactions = transactions != null ? List.copyOf(transactions) : Collections.emptyList();
  }
}
