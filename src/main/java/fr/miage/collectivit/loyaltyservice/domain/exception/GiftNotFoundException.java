package fr.miage.collectivit.loyaltyservice.domain.exception;

import java.util.UUID;

/** Exception thrown when a requested loyalty gift does not exist. */
public class GiftNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public GiftNotFoundException(UUID giftUid) {
    super("Loyalty gift not found for identifier: " + giftUid);
  }

  public GiftNotFoundException(UUID storeUid, UUID giftUid) {
    super(String.format("Loyalty gift %s not found for store %s", giftUid, storeUid));
  }
}
