package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.math.BigDecimal;

/**
 * Port d'entrée (Driving / Primary Port) pour la configuration du barème de fidélité commerçant.
 */
public interface ConfigureMerchantLoyaltyRateUseCase {

  MerchantLoyaltyRate configureRate(Long storeId, BigDecimal euroAmount, Integer pointsReward);
}
