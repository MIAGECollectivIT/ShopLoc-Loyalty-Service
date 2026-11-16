package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when requested gift stock deduction exceeds available inventory. */
public class InsufficientGiftStockException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InsufficientGiftStockException(String message) {
    super(message);
  }
}
