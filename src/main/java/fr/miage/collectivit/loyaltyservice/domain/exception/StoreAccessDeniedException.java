package fr.miage.collectivit.loyaltyservice.domain.exception;

/**
 * Exception thrown when an authenticated user attempts to access or modify a store without owning
 * it.
 */
public class StoreAccessDeniedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StoreAccessDeniedException(String message) {
    super(message);
  }
}
