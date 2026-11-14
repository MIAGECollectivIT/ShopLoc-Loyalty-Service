package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** JPA entity representing the user_vfp_status table in PostgreSQL (SLE-45 / SLE-199). */
@Entity
@Table(name = "user_vfp_status")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserVfpStatusJpaEntity {

  @Id
  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "is_vfp", nullable = false)
  private boolean isVfp;

  @Column(name = "granted_at")
  private Instant grantedAt;

  @Column(name = "revoked_at")
  private Instant revokedAt;

  @Column(name = "last_evaluated_at", nullable = false)
  private Instant lastEvaluatedAt;
}
