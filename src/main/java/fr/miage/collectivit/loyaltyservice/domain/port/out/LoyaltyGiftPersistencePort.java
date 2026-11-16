package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.util.List;
import java.util.Optional;

/** Output port for persistence operations on loyalty gifts catalog (SLE-60 / SLE-191). */
public interface LoyaltyGiftPersistencePort {

  LoyaltyGift save(LoyaltyGift gift);

  Optional<LoyaltyGift> findById(Long id);

  Optional<LoyaltyGift> findByIdAndStoreId(Long id, Long storeId);

  List<LoyaltyGift> findByStoreId(Long storeId);

  List<LoyaltyGift> findActiveByStoreId(Long storeId);

  void deleteById(Long id);
}

