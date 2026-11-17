package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when a non-VFP user attempts to activate a VFP parking session. */
public class UserNotVfpException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public UserNotVfpException(String message) {
    super(message);
  }
}
