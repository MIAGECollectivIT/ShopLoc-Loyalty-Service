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
}
