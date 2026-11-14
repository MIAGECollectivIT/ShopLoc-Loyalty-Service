package fr.miage.collectivit.loyaltyservice.domain.exception;

public class SalesServiceUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SalesServiceUnavailableException(String message) {
    super(message);
  }

  public SalesServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
