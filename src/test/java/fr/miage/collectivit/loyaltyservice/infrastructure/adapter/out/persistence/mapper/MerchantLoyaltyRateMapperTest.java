package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.MerchantLoyaltyRateJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Persistence Mapper - MerchantLoyaltyRateMapper Test")
class MerchantLoyaltyRateMapperTest {

  private final MerchantLoyaltyRateMapper mapper = new MerchantLoyaltyRateMapper();

  @Test
  @DisplayName("toDomain et toJpaEntity doivent gérer les entrées nulles proprement")
  void shouldHandleNullInputs() {
    assertThat(mapper.toDomain(null)).isNull();
    assertThat(mapper.toJpaEntity(null)).isNull();
  }

  @Test
  @DisplayName("Devrait mapper fidèlement un domain model vers une entité JPA et inversement")
  void shouldMapBidirectionally() {
    Instant now = Instant.now();
    MerchantLoyaltyRate domain =
        new MerchantLoyaltyRate(1L, 10L, new BigDecimal("2.50"), new BigDecimal("5.5"), now, now);

    MerchantLoyaltyRateJpaEntity entity = mapper.toJpaEntity(domain);
    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(1L);
    assertThat(entity.getStoreId()).isEqualTo(10L);
    assertThat(entity.getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
    assertThat(entity.getPointsReward()).isEqualByComparingTo(new BigDecimal("5.5"));

    MerchantLoyaltyRate mappedBack = mapper.toDomain(entity);
    assertThat(mappedBack).isNotNull();
    assertThat(mappedBack.getId()).isEqualTo(1L);
    assertThat(mappedBack.getStoreId()).isEqualTo(10L);
    assertThat(mappedBack.getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
    assertThat(mappedBack.getPointsReward()).isEqualByComparingTo(new BigDecimal("5.5"));
  }
}
