package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when no loyalty rate is configured for a specific store. */
public class MerchantLoyaltyRateNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MerchantLoyaltyRateNotFoundException(Long storeId) {
    super("No loyalty rate found for store ID " + storeId);
  }
}
