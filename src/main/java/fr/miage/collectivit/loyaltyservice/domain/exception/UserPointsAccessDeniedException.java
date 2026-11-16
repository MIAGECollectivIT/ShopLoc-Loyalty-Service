package fr.miage.collectivit.loyaltyservice.domain.exception;

/**
 * Domain exception raised when an authenticated caller is denied access to a user's loyalty points
 * balance (SLE-38 / SLE-176).
 */
public class UserPointsAccessDeniedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UserPointsAccessDeniedException(String message) {
    super(message);
  }
}
