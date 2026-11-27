package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "loyalty_points_hold_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPointsHoldItemJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "hold_id", nullable = false)
  private LoyaltyPointsHoldJpaEntity hold;

  @Column(name = "store_uid", nullable = false)
  private UUID storeUid;

  @Column(name = "gift_uid", nullable = false)
  private UUID giftUid;

  @Column(name = "gift_name", nullable = false)
  private String giftName;

  @Column(name = "unit_points_cost", nullable = false)
  private Integer unitPointsCost;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;
}
