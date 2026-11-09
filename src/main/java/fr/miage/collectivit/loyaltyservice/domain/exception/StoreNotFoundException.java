package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception thrown when a store is not found in the store management service. */
public class StoreNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StoreNotFoundException(Long storeId) {
    super("Store with identifier " + storeId + " was not found");
  }
}
