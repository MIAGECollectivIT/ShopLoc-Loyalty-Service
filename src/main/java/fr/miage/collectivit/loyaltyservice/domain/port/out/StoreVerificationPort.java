package fr.miage.collectivit.loyaltyservice.domain.port.out;

import java.util.Optional;

/** Outbound port for verifying store existence and validating merchant ownership. */
public interface StoreVerificationPort {

  /**
   * Retrieves the owner user identifier of the specified store, if the store exists.
   *
   * @param storeId unique identifier of the store
   * @return an Optional containing the owner user ID (dbId), or empty if the store does not exist
   */
  Optional<Long> findStoreOwnerId(Long storeId);

  /**
   * Retrieves the commercial name of the specified store, if the store exists.
   *
   * @param storeId unique identifier of the store
   * @return an Optional containing the store name, or empty if the store does not exist
   */
  default Optional<String> findStoreName(Long storeId) {
    return Optional.empty();
  }
}
