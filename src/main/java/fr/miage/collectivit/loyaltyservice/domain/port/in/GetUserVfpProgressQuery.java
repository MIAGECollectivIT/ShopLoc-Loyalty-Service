package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpProgress;

/**
 * Inbound port to retrieve a customer's VFP progression metrics and revocation forecast (SLE-51 /
 * SLE-238).
 */
public interface GetUserVfpProgressQuery {

  VfpProgress getProgress(String userId);
}
