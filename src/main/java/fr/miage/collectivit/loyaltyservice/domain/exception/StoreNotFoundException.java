package fr.miage.collectivit.loyaltyservice.domain.exception;

import java.util.UUID;

/** Exception thrown when a store is not found in the store management service. */
public class StoreNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StoreNotFoundException(UUID storeUid) {
    super("Store with identifier " + storeUid + " was not found");
  }
}
