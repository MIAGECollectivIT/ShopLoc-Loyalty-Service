package fr.miage.collectivit.loyaltyservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Builder;
import lombok.Getter;

/**
 * Domain model representing a customer's progress towards obtaining or maintaining Very Faithful
 * Person (VFP) status, including sliding window metrics and revocation forecast (SLE-51 / SLE-238).
 */
@Getter
@Builder
public class VfpProgress {

  public static final int VFP_THRESHOLD = 10;
  public static final int SLIDING_WINDOW_DAYS = 15;

  private final String userId;
  private final boolean isVfp;
  private final int currentTransactionCount;
  private final int threshold;
  private final int remainingTransactionsToQualify;
  private final int remainingTransactionsToMaintain;
  private final Long daysRemainingBeforeRevocation;
  private final Long hoursRemainingBeforeRevocation;
  private final Instant estimatedRevocationDate;
  private final int expiringTransactionsCount;
  private final Instant windowStart;
  private final Instant windowEnd;
  private final Instant evaluatedAt;

  @SuppressWarnings("PMD.ExcessiveParameterList")
  public VfpProgress(
      String userId,
      boolean isVfp,
      int currentTransactionCount,
      int threshold,
      int remainingTransactionsToQualify,
      int remainingTransactionsToMaintain,
      Long daysRemainingBeforeRevocation,
      Long hoursRemainingBeforeRevocation,
      Instant estimatedRevocationDate,
      int expiringTransactionsCount,
      Instant windowStart,
      Instant windowEnd,
      Instant evaluatedAt) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null").trim();
    this.isVfp = isVfp;
    this.currentTransactionCount = Math.max(0, currentTransactionCount);
    this.threshold = threshold;
    this.remainingTransactionsToQualify = Math.max(0, remainingTransactionsToQualify);
    this.remainingTransactionsToMaintain = Math.max(0, remainingTransactionsToMaintain);
    this.daysRemainingBeforeRevocation = daysRemainingBeforeRevocation;
    this.hoursRemainingBeforeRevocation = hoursRemainingBeforeRevocation;
    this.estimatedRevocationDate = estimatedRevocationDate;
    this.expiringTransactionsCount = Math.max(0, expiringTransactionsCount);
    this.windowStart = Objects.requireNonNull(windowStart, "windowStart must not be null");
    this.windowEnd = Objects.requireNonNull(windowEnd, "windowEnd must not be null");
    this.evaluatedAt = evaluatedAt != null ? evaluatedAt : Instant.now();
  }
}
