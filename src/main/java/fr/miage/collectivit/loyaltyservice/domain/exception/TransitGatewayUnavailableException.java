package fr.miage.collectivit.loyaltyservice.domain.exception;

/**
 * Exception thrown when the external transit gateway service is unavailable or times out (SLE-47).
 */
public class TransitGatewayUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public TransitGatewayUnavailableException(String message) {
    super(message);
  }

  public TransitGatewayUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
