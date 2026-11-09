package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.util.Optional;

/**
 * Outbound port (Driven / Secondary Port) defining the persistence contract for merchant loyalty
 * rates.
 */
public interface MerchantLoyaltyRatePersistencePort {

  MerchantLoyaltyRate save(MerchantLoyaltyRate rate);

  Optional<MerchantLoyaltyRate> findByStoreId(Long storeId);

  boolean existsByStoreId(Long storeId);

  void deleteByStoreId(Long storeId);
}
