package fr.miage.collectivit.loyaltyservice.domain.exception;

public class InvalidLoyaltyRateException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public InvalidLoyaltyRateException(String message) {
    super(message);
  }
}
