package fr.miage.collectivit.loyaltyservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * Domain model representing a customer's dynamic activity evaluation over a 15-day sliding window
 * for determining Very Faithful Person (VFP) qualification (SLE-44 / SLE-162).
 */
@Getter
public class UserActivityEvaluation {

  public static final int VFP_ELIGIBILITY_THRESHOLD = 10;
  public static final int SLIDING_WINDOW_DAYS = 15;

  private final String userId;
  private final int transactionCount;
  private final int threshold;
  private final boolean isEligibleVfp;
  private final Instant windowStart;
  private final Instant windowEnd;

  public UserActivityEvaluation(
      String userId,
      int transactionCount,
      int threshold,
      boolean isEligibleVfp,
      Instant windowStart,
      Instant windowEnd) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null");
    this.transactionCount = Math.max(0, transactionCount);
    this.threshold = threshold;
    this.isEligibleVfp = isEligibleVfp;
    this.windowStart = Objects.requireNonNull(windowStart, "windowStart must not be null");
    this.windowEnd = Objects.requireNonNull(windowEnd, "windowEnd must not be null");
  }

  public static UserActivityEvaluation evaluate(
      String userId, int transactionCount, Instant windowStart, Instant windowEnd) {
    int threshold = VFP_ELIGIBILITY_THRESHOLD;
    boolean eligible = transactionCount >= threshold;
    return new UserActivityEvaluation(
        userId, transactionCount, threshold, eligible, windowStart, windowEnd);
  }
}
