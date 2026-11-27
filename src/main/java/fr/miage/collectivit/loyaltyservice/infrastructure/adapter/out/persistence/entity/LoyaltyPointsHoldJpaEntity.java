package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loyalty_points_holds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPointsHoldJpaEntity {

  public enum HoldStatus {
    ACTIVE,
    COMMITTED,
    RELEASED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "uid", nullable = false, unique = true, updatable = false)
  private UUID uid;

  @Column(name = "order_reference", nullable = false, unique = true, length = 64)
  private String orderReference;

  @Column(name = "user_id", nullable = false, length = 64)
  private String userId;

  @Column(name = "total_points", nullable = false, precision = 10, scale = 1)
  private BigDecimal totalPoints;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 32)
  private HoldStatus status;

  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @OneToMany(
      mappedBy = "hold",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @Builder.Default
  private List<LoyaltyPointsHoldItemJpaEntity> items = new ArrayList<>();

  public void addItem(LoyaltyPointsHoldItemJpaEntity item) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(item);
    item.setHold(this);
  }

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    if (this.uid == null) {
      this.uid = UUID.randomUUID();
    }
    if (this.createdAt == null) {
      this.createdAt = now;
    }
    if (this.updatedAt == null) {
      this.updatedAt = now;
    }
    if (this.status == null) {
      this.status = HoldStatus.ACTIVE;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
