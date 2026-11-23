package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.util.Optional;
import java.util.UUID;

/**
 * Outbound port (Driven / Secondary Port) defining the persistence contract for merchant loyalty
 * rates.
 */
public interface MerchantLoyaltyRatePersistencePort {

  MerchantLoyaltyRate save(MerchantLoyaltyRate rate);

  Optional<MerchantLoyaltyRate> findByStoreUid(UUID storeUid);

  boolean existsByStoreUid(UUID storeUid);

  void deleteByStoreUid(UUID storeUid);
}
