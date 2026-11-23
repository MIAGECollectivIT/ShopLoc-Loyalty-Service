package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Infrastructure JPA entity mapping the {@code loyalty_points_transactions} table (SLE-38 /
 * SLE-176).
 */
@Entity
@Table(name = "loyalty_points_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPointsTransactionJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "uid", nullable = false, unique = true, updatable = false)
  private UUID uid;

  @Column(name = "account_id", nullable = false)
  private Long accountId;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "transaction_reference", nullable = false, unique = true, length = 64)
  private String transactionReference;

  @Column(name = "store_uid")
  private UUID storeUid;

  @Column(name = "store_name", length = 255)
  private String storeName;

  @Enumerated(EnumType.STRING)
  @Column(name = "type", nullable = false, length = 32)
  private PointsTransactionType type;

  @Column(name = "points", nullable = false, precision = 10, scale = 1)
  private BigDecimal points;

  @Column(name = "balance_after", nullable = false, precision = 10, scale = 1)
  private BigDecimal balanceAfter;

  @Column(name = "source_type", length = 32)
  private String sourceType;

  @Column(name = "description", length = 255)
  private String description;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @PrePersist
  protected void onCreate() {
    if (this.uid == null) {
      this.uid = UUID.randomUUID();
    }
    if (this.createdAt == null) {
      this.createdAt = Instant.now();
    }
  }
}
