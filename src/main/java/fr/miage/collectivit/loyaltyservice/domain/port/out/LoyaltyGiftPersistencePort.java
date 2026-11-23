package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Output port for persistence operations on loyalty gifts catalog (SLE-60 / SLE-191). */
public interface LoyaltyGiftPersistencePort {

  LoyaltyGift save(LoyaltyGift gift);

  Optional<LoyaltyGift> findByUid(UUID uid);

  Optional<LoyaltyGift> findByUidAndStoreUid(UUID uid, UUID storeUid);

  default Optional<LoyaltyGift> findByUidAndStoreUidForUpdate(UUID uid, UUID storeUid) {
    return findByUidAndStoreUid(uid, storeUid);
  }

  List<LoyaltyGift> findByStoreUid(UUID storeUid);

  List<LoyaltyGift> findActiveByStoreUid(UUID storeUid);

  List<LoyaltyGift> findAllActive();

  void deleteByUid(UUID uid);
}
