package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.math.BigDecimal;
import java.util.UUID;

/** Inbound port (Driving / Primary Port) for configuring merchant loyalty conversion rates. */
public interface ConfigureMerchantLoyaltyRateUseCase {

  /**
   * Configures or updates the euro-to-points loyalty conversion rate for a given store.
   *
   * @param storeUid unique public identifier of the store
   * @param euroAmount base purchase amount in euros
   * @param pointsReward number of loyalty points rewarded
   * @param callerUserId public UUID identifier of the user performing the action
   * @param callerRole role of the authenticated caller (MERCHANT, ADMIN)
   * @return the configured and persisted merchant loyalty rate
   */
  MerchantLoyaltyRate configureRate(
      UUID storeUid,
      BigDecimal euroAmount,
      BigDecimal pointsReward,
      UUID callerUserId,
      String callerRole);
}
