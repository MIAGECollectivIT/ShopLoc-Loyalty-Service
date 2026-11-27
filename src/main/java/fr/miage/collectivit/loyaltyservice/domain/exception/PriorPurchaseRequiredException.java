package fr.miage.collectivit.loyaltyservice.domain.exception;

public class PriorPurchaseRequiredException extends RuntimeException {
  private static final long serialVersionUID = 1L;

  public PriorPurchaseRequiredException(String message) {
    super(message);
  }
}
