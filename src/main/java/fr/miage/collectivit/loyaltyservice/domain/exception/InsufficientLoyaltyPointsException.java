package fr.miage.collectivit.loyaltyservice.domain.exception;

/**
 * Thrown when a customer does not have enough loyalty points to complete a physical gift redemption
 * (Story SLE-43).
 */
public class InsufficientLoyaltyPointsException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InsufficientLoyaltyPointsException(String message) {
    super(message);
  }
}
