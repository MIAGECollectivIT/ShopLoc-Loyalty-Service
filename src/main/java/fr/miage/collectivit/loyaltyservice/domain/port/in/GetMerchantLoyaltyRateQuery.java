package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.util.UUID;

/** Inbound port (Driving / Primary Port) for querying merchant loyalty conversion rates. */
public interface GetMerchantLoyaltyRateQuery {

  MerchantLoyaltyRate getRateByStoreUid(UUID storeUid);
}
