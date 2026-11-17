package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when the external parking ticket gateway is unavailable or rejects the call. */
public class ParkingGatewayUnavailableException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public ParkingGatewayUnavailableException(String message) {
    super(message);
  }

  public ParkingGatewayUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
