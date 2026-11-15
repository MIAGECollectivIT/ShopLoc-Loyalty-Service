package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsAccountJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsTransactionJpaEntity;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Persistence Mapper - UserPointsMapper Test (SLE-38 / SLE-176)")
class UserPointsMapperTest {

  private final UserPointsMapper mapper = new UserPointsMapper();

  @Test
  @DisplayName("toDomain and toEntity should handle null inputs cleanly")
  void shouldHandleNullInputs() {
    assertThat(mapper.toDomain((UserPointsAccountJpaEntity) null)).isNull();
    assertThat(mapper.toEntity((UserPointsAccount) null)).isNull();
    assertThat(mapper.toDomain((UserPointsTransactionJpaEntity) null)).isNull();
    assertThat(mapper.toEntity(1L, null)).isNull();
  }

  @Test
  @DisplayName("Should map bidirectionally between UserPointsAccount and JPA entity")
  void shouldMapAccountBidirectionally() {
    Instant createdAt = Instant.parse("2026-11-10T08:00:00Z");
    Instant updatedAt = Instant.parse("2026-11-15T10:30:00Z");
    UserPointsAccount domain =
        new UserPointsAccount(
            12L,
            "550e8400-e29b-41d4-a716-446655440000",
            "CL95001",
            new BigDecimal("142.5"),
            createdAt,
            updatedAt);

    UserPointsAccountJpaEntity entity = mapper.toEntity(domain);
    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(12L);
    assertThat(entity.getUserId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    assertThat(entity.getClientCode()).isEqualTo("CL95001");
    assertThat(entity.getBalance()).isEqualByComparingTo("142.5");
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
    assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);

    UserPointsAccount mappedBack = mapper.toDomain(entity);
    assertThat(mappedBack).isNotNull();
    assertThat(mappedBack.id()).isEqualTo(12L);
    assertThat(mappedBack.userId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    assertThat(mappedBack.clientCode()).isEqualTo("CL95001");
    assertThat(mappedBack.balance()).isEqualByComparingTo("142.5");
    assertThat(mappedBack.createdAt()).isEqualTo(createdAt);
    assertThat(mappedBack.updatedAt()).isEqualTo(updatedAt);
  }

  @Test
  @DisplayName("Should map bidirectionally between PointsTransaction and JPA entity")
  void shouldMapTransactionBidirectionally() {
    Instant createdAt = Instant.parse("2026-11-15T09:15:00Z");
    PointsTransaction domain =
        new PointsTransaction(
            42L,
            "PTS-EARN-2026-0042",
            "550e8400-e29b-41d4-a716-446655440000",
            10L,
            "Boulangerie du Centre",
            PointsTransactionType.EARN,
            new BigDecimal("12.5"),
            new BigDecimal("142.5"),
            "PURCHASE",
            "Achat en boutique",
            createdAt);

    UserPointsTransactionJpaEntity entity = mapper.toEntity(12L, domain);
    assertThat(entity).isNotNull();
    assertThat(entity.getId()).isEqualTo(42L);
    assertThat(entity.getAccountId()).isEqualTo(12L);
    assertThat(entity.getTransactionReference()).isEqualTo("PTS-EARN-2026-0042");
    assertThat(entity.getUserId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    assertThat(entity.getStoreId()).isEqualTo(10L);
    assertThat(entity.getStoreName()).isEqualTo("Boulangerie du Centre");
    assertThat(entity.getType()).isEqualTo(PointsTransactionType.EARN);
    assertThat(entity.getPoints()).isEqualByComparingTo("12.5");
    assertThat(entity.getBalanceAfter()).isEqualByComparingTo("142.5");
    assertThat(entity.getSourceType()).isEqualTo("PURCHASE");
    assertThat(entity.getDescription()).isEqualTo("Achat en boutique");
    assertThat(entity.getCreatedAt()).isEqualTo(createdAt);

    PointsTransaction mappedBack = mapper.toDomain(entity);
    assertThat(mappedBack).isNotNull();
    assertThat(mappedBack.id()).isEqualTo(42L);
    assertThat(mappedBack.transactionReference()).isEqualTo("PTS-EARN-2026-0042");
    assertThat(mappedBack.userId()).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
    assertThat(mappedBack.storeId()).isEqualTo(10L);
    assertThat(mappedBack.storeName()).isEqualTo("Boulangerie du Centre");
    assertThat(mappedBack.type()).isEqualTo(PointsTransactionType.EARN);
    assertThat(mappedBack.points()).isEqualByComparingTo("12.5");
    assertThat(mappedBack.balanceAfter()).isEqualByComparingTo("142.5");
    assertThat(mappedBack.sourceType()).isEqualTo("PURCHASE");
    assertThat(mappedBack.description()).isEqualTo("Achat en boutique");
    assertThat(mappedBack.createdAt()).isEqualTo(createdAt);
  }
}
