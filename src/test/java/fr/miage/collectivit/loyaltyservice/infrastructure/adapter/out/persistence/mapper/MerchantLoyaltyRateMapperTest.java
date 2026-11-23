package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.MerchantLoyaltyRateJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Persistence Mapper - MerchantLoyaltyRateMapper Test")
class MerchantLoyaltyRateMapperTest {

  private final MerchantLoyaltyRateMapper mapper = new MerchantLoyaltyRateMapper();

  @Test
  @DisplayName("toDomain and toJpaEntity should handle null inputs cleanly")
  void shouldHandleNullInputs() {
    assertThat(mapper.toDomain(null)).isNull();
    assertThat(mapper.toJpaEntity(null)).isNull();
  }

  @Test
  @DisplayName("Should map bidirectionally between domain model and JPA entity")
  void shouldMapBidirectionally() {
    Instant now = Instant.now();
    UUID uid = UUID.fromString("550e8400-e29b-41d4-a716-446655440001");
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    MerchantLoyaltyRate domain =
        new MerchantLoyaltyRate(
            1L, uid, storeUid, new BigDecimal("2.50"), new BigDecimal("5.5"), now, now);

    MerchantLoyaltyRateJpaEntity entity = mapper.toJpaEntity(domain);
    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(1L);
    assertThat(entity.getUid()).isEqualTo(uid);
    assertThat(entity.getStoreUid()).isEqualTo(storeUid);
    assertThat(entity.getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
    assertThat(entity.getPointsReward()).isEqualByComparingTo(new BigDecimal("5.5"));

    MerchantLoyaltyRate mappedBack = mapper.toDomain(entity);
    assertThat(mappedBack).isNotNull();
    assertThat(mappedBack.getId()).isEqualTo(1L);
    assertThat(mappedBack.getUid()).isEqualTo(uid);
    assertThat(mappedBack.getStoreUid()).isEqualTo(storeUid);
    assertThat(mappedBack.getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
    assertThat(mappedBack.getPointsReward()).isEqualByComparingTo(new BigDecimal("5.5"));
  }
}
