package fr.miage.collectivit.loyaltyservice.domain.exception;

/**
 * Exception thrown when an active VFP user has already activated a transit ticket today (SLE-47).
 */
public class TransitTicketAlreadyUsedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TransitTicketAlreadyUsedException(String message) {
    super(message);
  }

  public TransitTicketAlreadyUsedException(String message, Throwable cause) {
    super(message, cause);
  }
}
