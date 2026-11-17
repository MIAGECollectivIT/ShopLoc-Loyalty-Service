package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when the requested license plate is not registered on the user's profile. */
public class VehicleNotRegisteredException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public VehicleNotRegisteredException(String message) {
    super(message);
  }
}
