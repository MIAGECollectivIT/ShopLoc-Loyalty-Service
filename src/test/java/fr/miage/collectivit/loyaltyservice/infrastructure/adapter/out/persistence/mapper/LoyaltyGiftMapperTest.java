package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Persistence Adapter - LoyaltyGiftMapper Test")
class LoyaltyGiftMapperTest {

  private final LoyaltyGiftMapper mapper = new LoyaltyGiftMapper();

  @Test
  @DisplayName("Should map entity to domain correctly")
  void shouldMapEntityToDomain() {
    Instant now = Instant.parse("2026-11-16T12:00:00Z");
    LoyaltyGiftJpaEntity entity =
        LoyaltyGiftJpaEntity.builder()
            .id(1L)
            .storeId(10L)
            .name("Mug Collectiv'IT")
            .description("Mug en céramique")
            .pointsCost(80)
            .stock(12)
            .imageUrl("https://cdn.shoploc.fr/gifts/mug.png")
            .active(true)
            .createdAt(now)
            .updatedAt(now)
            .build();

    LoyaltyGift domain = mapper.toDomain(entity);

    assertThat(domain).isNotNull();
    assertThat(domain.getId()).isEqualTo(1L);
    assertThat(domain.getStoreId()).isEqualTo(10L);
    assertThat(domain.getName()).isEqualTo("Mug Collectiv'IT");
    assertThat(domain.getDescription()).isEqualTo("Mug en céramique");
    assertThat(domain.getPointsCost()).isEqualTo(80);
    assertThat(domain.getStock()).isEqualTo(12);
    assertThat(domain.getImageUrl()).isEqualTo("https://cdn.shoploc.fr/gifts/mug.png");
    assertThat(domain.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should map domain to entity correctly")
  void shouldMapDomainToEntity() {
    Instant now = Instant.parse("2026-11-16T12:00:00Z");
    LoyaltyGift domain =
        new LoyaltyGift(
            1L,
            10L,
            "Mug Collectiv'IT",
            "Mug en céramique",
            80,
            12,
            "https://cdn.shoploc.fr/gifts/mug.png",
            true,
            now,
            now);

    LoyaltyGiftJpaEntity entity = mapper.toEntity(domain);

    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(1L);
    assertThat(entity.getStoreId()).isEqualTo(10L);
    assertThat(entity.getName()).isEqualTo("Mug Collectiv'IT");
    assertThat(entity.getDescription()).isEqualTo("Mug en céramique");
    assertThat(entity.getPointsCost()).isEqualTo(80);
    assertThat(entity.getStock()).isEqualTo(12);
    assertThat(entity.getImageUrl()).isEqualTo("https://cdn.shoploc.fr/gifts/mug.png");
    assertThat(entity.isActive()).isTrue();
  }

  @Test
  @DisplayName("Should handle null mappings gracefully")
  void shouldHandleNull() {
    assertThat(mapper.toDomain(null)).isNull();
    assertThat(mapper.toEntity(null)).isNull();
  }
}

