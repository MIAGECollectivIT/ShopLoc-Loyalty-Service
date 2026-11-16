package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when gift attributes violate domain validation rules. */
public class InvalidGiftException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidGiftException(String message) {
    super(message);
  }
}
