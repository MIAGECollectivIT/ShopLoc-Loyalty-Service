package fr.miage.collectivit.loyaltyservice.domain.exception;

import java.util.UUID;

/** Exception thrown when no loyalty rate is configured for a specific store. */
public class MerchantLoyaltyRateNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MerchantLoyaltyRateNotFoundException(UUID storeUid) {
    super("No loyalty rate found for store UID " + storeUid);
  }
}
