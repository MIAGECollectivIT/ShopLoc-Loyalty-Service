package fr.miage.collectivit.loyaltyservice.domain.model;

import java.time.Instant;
import java.util.Objects;
import lombok.Getter;

/**
 * Domain model representing the current Very Faithful Person (VFP) status of a user (SLE-45 /
 * SLE-199).
 */
@Getter
public class UserVfpStatus {

  private final String userId;
  private final boolean isVfp;
  private final Instant grantedAt;
  private final Instant revokedAt;
  private final Instant lastEvaluatedAt;

  public UserVfpStatus(
      String userId, boolean isVfp, Instant grantedAt, Instant revokedAt, Instant lastEvaluatedAt) {
    this.userId = Objects.requireNonNull(userId, "userId must not be null").trim();
    this.isVfp = isVfp;
    this.grantedAt = grantedAt;
    this.revokedAt = revokedAt;
    this.lastEvaluatedAt = lastEvaluatedAt != null ? lastEvaluatedAt : Instant.now();
  }

  public static UserVfpStatus initial(String userId, Instant now) {
    return new UserVfpStatus(userId, false, null, null, now);
  }

  public UserVfpStatus grant(Instant now) {
    return new UserVfpStatus(this.userId, true, now, null, now);
  }

  public UserVfpStatus revoke(Instant now) {
    return new UserVfpStatus(this.userId, false, this.grantedAt, now, now);
  }

  public UserVfpStatus maintain(Instant now) {
    return new UserVfpStatus(this.userId, this.isVfp, this.grantedAt, this.revokedAt, now);
  }
}
