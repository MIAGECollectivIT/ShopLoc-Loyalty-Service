package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.event;

import java.time.Instant;

/**
 * Event representing a successful payment / sale transaction triggering VFP re-evaluation (SLE-45 /
 * SLE-201).
 */
public record TransactionApprovedEvent(
    String userId,
    String transactionId,
    String reference,
    Double amount,
    String currency,
    Instant timestamp) {

  public TransactionApprovedEvent {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("userId must not be blank in TransactionApprovedEvent");
    }
    if (timestamp == null) {
      timestamp = Instant.now();
    }
  }
}
