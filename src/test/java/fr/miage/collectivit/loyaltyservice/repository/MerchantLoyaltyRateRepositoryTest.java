package fr.miage.collectivit.loyaltyservice.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import fr.miage.collectivit.loyaltyservice.entity.MerchantLoyaltyRate;
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
class MerchantLoyaltyRateRepositoryTest {

  @Autowired private MerchantLoyaltyRateRepository repository;

  @Test
  @DisplayName("Devrait sauvegarder et retrouver le barème de fidélité par storeId")
  void shouldSaveAndFindByStoreId() {
    MerchantLoyaltyRate rate = new MerchantLoyaltyRate(10L, new BigDecimal("2.50"), 5);

    MerchantLoyaltyRate saved = repository.save(rate);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedAt()).isNotNull();
    assertThat(saved.getUpdatedAt()).isNotNull();

    Optional<MerchantLoyaltyRate> found = repository.findByStoreId(10L);
    assertThat(found).isPresent();
    assertThat(found.get().getStoreId()).isEqualTo(10L);
    assertThat(found.get().getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
    assertThat(found.get().getPointsReward()).isEqualTo(5);
  }

  @Test
  @DisplayName("Devrait refuser deux barèmes pour un même storeId (contrainte d'unicité)")
  void shouldRejectDuplicateStoreId() {
    MerchantLoyaltyRate rate1 = new MerchantLoyaltyRate(42L, new BigDecimal("1.00"), 1);
    repository.saveAndFlush(rate1);

    MerchantLoyaltyRate rate2 = new MerchantLoyaltyRate(42L, new BigDecimal("5.00"), 10);

    assertThatThrownBy(() -> repository.saveAndFlush(rate2))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName("Devrait vérifier l'existence d'un barème par storeId")
  void shouldCheckExistenceByStoreId() {
    assertThat(repository.existsByStoreId(99L)).isFalse();

    MerchantLoyaltyRate rate = new MerchantLoyaltyRate(99L, new BigDecimal("3.00"), 2);
    repository.save(rate);

    assertThat(repository.existsByStoreId(99L)).isTrue();
  }

  @Test
  @DisplayName("Devrait supprimer le barème par storeId")
  void shouldDeleteByStoreId() {
    MerchantLoyaltyRate rate = new MerchantLoyaltyRate(100L, new BigDecimal("1.00"), 1);
    repository.save(rate);

    repository.deleteByStoreId(100L);

    assertThat(repository.findByStoreId(100L)).isEmpty();
  }
}
