package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.math.BigDecimal;
import java.util.Optional;
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
  @DisplayName("Devrait sauvegarder et retrouver le barème de fidélité via l'adaptateur")
  void shouldSaveAndFindByStoreIdViaAdapter() {
    MerchantLoyaltyRate rate = new MerchantLoyaltyRate(10L, new BigDecimal("2.50"), 5);

    MerchantLoyaltyRate saved = persistenceAdapter.save(rate);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    Optional<MerchantLoyaltyRate> found = persistenceAdapter.findByStoreId(10L);
    assertThat(found).isPresent();
    assertThat(found.get().getStoreId()).isEqualTo(10L);
    assertThat(found.get().getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
    assertThat(found.get().getPointsReward()).isEqualTo(5);
  }

  @Test
  @DisplayName("Devrait refuser deux barèmes pour un même storeId (contrainte d'unicité en base)")
  void shouldRejectDuplicateStoreIdViaAdapter() {
    MerchantLoyaltyRate rate1 = new MerchantLoyaltyRate(42L, new BigDecimal("1.00"), 1);
    persistenceAdapter.save(rate1);

    MerchantLoyaltyRate rate2 = new MerchantLoyaltyRate(42L, new BigDecimal("5.00"), 10);

    assertThatThrownBy(() -> persistenceAdapter.save(rate2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("Devrait vérifier l'existence d'un barème par storeId via l'adaptateur")
  void shouldCheckExistenceByStoreIdViaAdapter() {
    assertThat(persistenceAdapter.existsByStoreId(99L)).isFalse();

    MerchantLoyaltyRate rate = new MerchantLoyaltyRate(99L, new BigDecimal("3.00"), 2);
    persistenceAdapter.save(rate);

    assertThat(persistenceAdapter.existsByStoreId(99L)).isTrue();
  }

  @Test
  @DisplayName("Devrait supprimer le barème par storeId via l'adaptateur")
  void shouldDeleteByStoreIdViaAdapter() {
    MerchantLoyaltyRate rate = new MerchantLoyaltyRate(100L, new BigDecimal("1.00"), 1);
    persistenceAdapter.save(rate);

    persistenceAdapter.deleteByStoreId(100L);

    assertThat(persistenceAdapter.findByStoreId(100L)).isEmpty();
  }
}
