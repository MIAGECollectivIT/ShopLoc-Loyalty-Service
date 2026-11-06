package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;

/** Port d'entrée (Driving / Primary Port) pour la consultation du barème de fidélité commerçant. */
public interface GetMerchantLoyaltyRateQuery {

  MerchantLoyaltyRate getRateByStoreId(Long storeId);
}
