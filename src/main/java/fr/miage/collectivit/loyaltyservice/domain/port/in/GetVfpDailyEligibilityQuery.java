package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpDailyEligibility;

/** Inbound query port for checking daily VFP benefit eligibility (SLE-48 / SLE-233). */
public interface GetVfpDailyEligibilityQuery {

  VfpDailyEligibility getDailyEligibility(String userId);
}
