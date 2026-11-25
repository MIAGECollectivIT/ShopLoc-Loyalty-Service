package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port (Driven / Secondary Port) defining the persistence contract for merchant loyalty
 * rates.
 */
public interface MerchantLoyaltyRatePersistencePort {

  MerchantLoyaltyRate save(MerchantLoyaltyRate rate);

  Optional<MerchantLoyaltyRate> findByStoreUid(UUID storeUid);

  default List<MerchantLoyaltyRate> findByStoreUidIn(Collection<UUID> storeUids) {
    return List.of();
  }

  boolean existsByStoreUid(UUID storeUid);

  void deleteByStoreUid(UUID storeUid);
}
