package fr.miage.collectivit.loyaltyservice.domain.exception;

/**
 * Thrown when a physical gift redemption request with the same idempotency key has already been
 * processed (Story SLE-43).
 */
public class DuplicatePhysicalRedemptionException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public DuplicatePhysicalRedemptionException(String message) {
    super(message);
  }
}
