package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when loyalty rate configuration values violate business validation rules. */
public class InvalidLoyaltyRateException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidLoyaltyRateException(String message) {
    super(message);
  }
}
