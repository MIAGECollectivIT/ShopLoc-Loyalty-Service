package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when a license plate payload is missing or invalid. */
public class InvalidLicensePlateException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public InvalidLicensePlateException(String message) {
    super(message);
  }
}
