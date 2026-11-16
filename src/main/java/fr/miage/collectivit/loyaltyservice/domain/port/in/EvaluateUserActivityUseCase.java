package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;

/**
 * Inbound hexagonal use case for evaluating customer purchasing activity over a 15-day sliding
 * window.
 */
public interface EvaluateUserActivityUseCase {

  /**
   * Calculates the user's activity on the sliding window [now - 15 days, now] and checks VFP
   * threshold.
   *
   * @param userId unique user identifier
   * @return evaluation result with transaction count, window bounds, and eligibility flag
   */
  UserActivityEvaluation evaluateActivity(String userId);
}
