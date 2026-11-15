package fr.miage.collectivit.loyaltyservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * Domain entity capturing an immutable audit log entry for a VFP status transition (SLE-45 /
 * SLE-199).
 */
@Getter
public class VfpHistoryEntry {

  private final Long id;
  private final String userId;
  private final VfpEventType eventType;
  private final int transactionCount;
  private final Instant createdAt;

  public VfpHistoryEntry(
      Long id, String userId, VfpEventType eventType, int transactionCount, Instant createdAt) {
    this.id = id;
    this.userId = Objects.requireNonNull(userId, "userId must not be null").trim();
    this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
    this.transactionCount = Math.max(0, transactionCount);
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
  }

  public static VfpHistoryEntry of(
      String userId, VfpEventType eventType, int transactionCount, Instant createdAt) {
    return new VfpHistoryEntry(null, userId, eventType, transactionCount, createdAt);
  }
}
