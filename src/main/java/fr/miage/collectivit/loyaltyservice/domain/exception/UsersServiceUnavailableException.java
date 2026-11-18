package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when Users Service cannot be reached to verify user vehicles. */
public class UsersServiceUnavailableException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public UsersServiceUnavailableException(String message) {
    super(message);
  }

  public UsersServiceUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
