package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("Persistence Adapter - MerchantLoyaltyRatePersistenceAdapter Integration Test")
class MerchantLoyaltyRatePersistenceAdapterIntegrationTest {

  @Autowired private MerchantLoyaltyRatePersistenceAdapter persistenceAdapter;

  @Test
  @DisplayName("Should save and retrieve loyalty rate via adapter")
  void shouldSaveAndFindByStoreUidViaAdapter() {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(storeUid, new BigDecimal("2.50"), new BigDecimal("5.0"));

    MerchantLoyaltyRate saved = persistenceAdapter.save(rate);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getUid()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    Optional<MerchantLoyaltyRate> found = persistenceAdapter.findByStoreUid(storeUid);
    assertThat(found).isPresent();
    assertThat(found.get().getStoreUid()).isEqualTo(storeUid);
    assertThat(found.get().getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
    assertThat(found.get().getPointsReward()).isEqualByComparingTo(new BigDecimal("5.0"));
  }

  @Test
  @DisplayName("Should reject duplicate storeUid via adapter (database uniqueness constraint)")
  void shouldRejectDuplicateStoreUidViaAdapter() {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
    MerchantLoyaltyRate rate1 =
        new MerchantLoyaltyRate(storeUid, new BigDecimal("1.00"), new BigDecimal("1.0"));
    persistenceAdapter.save(rate1);

    MerchantLoyaltyRate rate2 =
        new MerchantLoyaltyRate(storeUid, new BigDecimal("5.00"), new BigDecimal("10.0"));

    assertThatThrownBy(() -> persistenceAdapter.save(rate2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("Should check existence by storeUid via adapter")
  void shouldCheckExistenceByStoreUidViaAdapter() {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");
    assertThat(persistenceAdapter.existsByStoreUid(storeUid)).isFalse();

    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(storeUid, new BigDecimal("3.00"), new BigDecimal("2.0"));
    persistenceAdapter.save(rate);

    assertThat(persistenceAdapter.existsByStoreUid(storeUid)).isTrue();
  }

  @Test
  @DisplayName("Should delete rate by storeUid via adapter")
  void shouldDeleteByStoreUidViaAdapter() {
    UUID storeUid = UUID.fromString("550e8400-e29b-41d4-a716-446655440100");
    MerchantLoyaltyRate rate =
        new MerchantLoyaltyRate(storeUid, new BigDecimal("1.00"), new BigDecimal("1.0"));
    persistenceAdapter.save(rate);

    persistenceAdapter.deleteByStoreUid(storeUid);

    assertThat(persistenceAdapter.findByStoreUid(storeUid)).isEmpty();
  }
}
