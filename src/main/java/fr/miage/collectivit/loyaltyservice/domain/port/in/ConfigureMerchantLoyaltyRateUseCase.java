package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import java.math.BigDecimal;

/**
 * Port d'entrée (Driving / Primary Port) pour la configuration du barème de fidélité commerçant.
 */
public interface ConfigureMerchantLoyaltyRateUseCase {

  /**
   * Configure ou met à jour le barème de conversion euro/points pour un commerce donné.
   *
   * @param storeId identifiant unique du commerce
   * @param euroAmount montant en euros de base
   * @param pointsReward nombre de points accordés
   * @param callerUserId identifiant de l'utilisateur effectuant l'action (dbId)
   * @param callerRole rôle de l'utilisateur (MERCHANT, ADMIN)
   * @return le barème de fidélité configuré et persisté
   */
  MerchantLoyaltyRate configureRate(
      Long storeId,
      BigDecimal euroAmount,
      Integer pointsReward,
      Long callerUserId,
      String callerRole);

  /**
   * Méthode de commodité pour la configuration sans contrôle d'accès utilisateur (mode
   * système/admin).
   */
  default MerchantLoyaltyRate configureRate(
      Long storeId, BigDecimal euroAmount, Integer pointsReward) {
    return configureRate(storeId, euroAmount, pointsReward, null, "ADMIN");
  }
}
