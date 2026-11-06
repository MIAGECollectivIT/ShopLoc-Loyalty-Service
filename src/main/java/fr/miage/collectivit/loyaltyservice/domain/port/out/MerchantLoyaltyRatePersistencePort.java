package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.util.Optional;

/**
 * Port de sortie (Driven / Secondary Port) définissant le contrat de persistance pour le barème de
 * fidélité des commerces.
 */
public interface MerchantLoyaltyRatePersistencePort {

  MerchantLoyaltyRate save(MerchantLoyaltyRate rate);

  Optional<MerchantLoyaltyRate> findByStoreId(Long storeId);

  boolean existsByStoreId(Long storeId);

  void deleteByStoreId(Long storeId);
}
