package fr.miage.collectivit.loyaltyservice.domain.exception;

public class UserActivityAccessDeniedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public UserActivityAccessDeniedException(String message) {
    super(message);
  }
}
