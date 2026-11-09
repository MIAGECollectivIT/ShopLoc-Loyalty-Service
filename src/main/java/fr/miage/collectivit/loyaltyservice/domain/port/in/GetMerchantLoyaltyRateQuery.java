package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;

/** Inbound port (Driving / Primary Port) for querying merchant loyalty conversion rates. */
public interface GetMerchantLoyaltyRateQuery {

  MerchantLoyaltyRate getRateByStoreId(Long storeId);
}
