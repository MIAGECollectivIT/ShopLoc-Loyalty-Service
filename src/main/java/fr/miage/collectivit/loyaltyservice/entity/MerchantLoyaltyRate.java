package fr.miage.collectivit.loyaltyservice.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entité JPA représentant le barème de fidélité d'un commerce partenaire (Story SLE-55 / SLE-145).
 * Définit la règle : pour X € dépensés (euroAmount) = Y points de fidélité accordés (pointsReward).
 */
@Entity
@Table(name = "merchant_loyalty_rates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantLoyaltyRate {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "store_id", nullable = false, unique = true)
  private Long storeId;

  @Column(name = "euro_amount", nullable = false, precision = 10, scale = 2)
  private BigDecimal euroAmount;

  @Column(name = "points_reward", nullable = false)
  private Integer pointsReward;

  @Column(name = "created_at", nullable = false, updatable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public MerchantLoyaltyRate(Long storeId, BigDecimal euroAmount, Integer pointsReward) {
    this.storeId = storeId;
    this.euroAmount = euroAmount;
    this.pointsReward = pointsReward;
  }

  @PrePersist
  protected void onCreate() {
    Instant now = Instant.now();
    if (this.createdAt == null) {
      this.createdAt = now;
    }
    if (this.updatedAt == null) {
      this.updatedAt = now;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = Instant.now();
  }
}
