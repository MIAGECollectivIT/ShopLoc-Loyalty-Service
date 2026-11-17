package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("Persistence Adapter - LoyaltyGiftPersistenceAdapter Integration Test (SLE-191)")
class LoyaltyGiftPersistenceAdapterIntegrationTest {

  @Autowired private LoyaltyGiftPersistenceAdapter persistenceAdapter;

  @Test
  @DisplayName("Should save, find by id and storeId, update and delete a loyalty gift")
  void shouldPerformCrudOperationsOnLoyaltyGift() {
    LoyaltyGift gift =
        new LoyaltyGift(
            15L,
            "Gourde Inox ShopLoc",
            "Gourde 500ml isotherme",
            120,
            15,
            "https://cdn.shoploc.fr/gifts/gourde.png");

    LoyaltyGift saved = persistenceAdapter.save(gift);

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getStoreId()).isEqualTo(15L);
    assertThat(saved.getName()).isEqualTo("Gourde Inox ShopLoc");
    assertThat(saved.getPointsCost()).isEqualTo(120);
    assertThat(saved.getStock()).isEqualTo(15);
    assertThat(saved.isActive()).isTrue();

    Optional<LoyaltyGift> found = persistenceAdapter.findByIdAndStoreId(saved.getId(), 15L);
    assertThat(found).isPresent();
    assertThat(found.get().getName()).isEqualTo("Gourde Inox ShopLoc");

    // Update
    LoyaltyGift toUpdate = found.get();
    toUpdate.update(
        "Gourde Inox 750ml",
        "Nouvelle contenance",
        150,
        "https://cdn.shoploc.fr/gifts/gourde-750.png");
    toUpdate.adjustStock(5);
    LoyaltyGift updated = persistenceAdapter.save(toUpdate);

    assertThat(updated.getName()).isEqualTo("Gourde Inox 750ml");
    assertThat(updated.getPointsCost()).isEqualTo(150);
    assertThat(updated.getStock()).isEqualTo(20);

    // List by store
    List<LoyaltyGift> storeGifts = persistenceAdapter.findByStoreId(15L);
    assertThat(storeGifts).hasSize(1);
    assertThat(persistenceAdapter.findAllActive()).hasSize(1);

    // Soft delete / deactivate
    toUpdate.deactivate();
    persistenceAdapter.save(toUpdate);
    assertThat(persistenceAdapter.findActiveByStoreId(15L)).isEmpty();
    assertThat(persistenceAdapter.findAllActive()).isEmpty();
    assertThat(persistenceAdapter.findByStoreId(15L)).hasSize(1);

    // Physical delete
    persistenceAdapter.deleteById(saved.getId());
    assertThat(persistenceAdapter.findById(saved.getId())).isEmpty();
  }
}
