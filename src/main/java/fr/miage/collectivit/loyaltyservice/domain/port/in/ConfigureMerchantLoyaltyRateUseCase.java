package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.math.BigDecimal;

/** Inbound port (Driving / Primary Port) for configuring merchant loyalty conversion rates. */
public interface ConfigureMerchantLoyaltyRateUseCase {

  /**
   * Configures or updates the euro-to-points loyalty conversion rate for a given store.
   *
   * @param storeId unique identifier of the store
   * @param euroAmount base purchase amount in euros
   * @param pointsReward number of loyalty points rewarded
   * @param callerUserId internal identifier of the user performing the action (dbId)
   * @param callerRole role of the authenticated caller (MERCHANT, ADMIN)
   * @return the configured and persisted merchant loyalty rate
   */
  MerchantLoyaltyRate configureRate(
      Long storeId,
      BigDecimal euroAmount,
      BigDecimal pointsReward,
      Long callerUserId,
      String callerRole);

  /**
   * Convenience overload for rate configuration without caller authorization check (system/admin
   * mode).
   */
  default MerchantLoyaltyRate configureRate(
      Long storeId, BigDecimal euroAmount, BigDecimal pointsReward) {
    return configureRate(storeId, euroAmount, pointsReward, null, "ADMIN");
  }
}
