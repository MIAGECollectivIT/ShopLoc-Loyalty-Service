package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when a requested loyalty gift does not exist. */
public class GiftNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public GiftNotFoundException(Long giftId) {
    super("Loyalty gift not found for identifier: " + giftId);
  }

  public GiftNotFoundException(Long storeId, Long giftId) {
    super(String.format("Loyalty gift %d not found for store %d", giftId, storeId));
  }
}
