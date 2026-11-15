package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;

/**
 * Inbound driving port defining the use case for dynamically evaluating customer transaction
 * activity and transitioning VFP qualification status (SLE-45 / SLE-200).
 */
public interface EvaluateAndApplyVfpStatusUseCase {

  UserVfpStatus evaluateAndApply(String userId);
}
