package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when a user has already activated their daily free parking session. */
public class ParkingSessionAlreadyUsedException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ParkingSessionAlreadyUsedException(String message) {
    super(message);
  }

  public ParkingSessionAlreadyUsedException(String message, Throwable cause) {
    super(message, cause);
  }
}
