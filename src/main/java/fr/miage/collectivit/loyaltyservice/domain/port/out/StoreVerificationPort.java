package fr.miage.collectivit.loyaltyservice.domain.port.out;

import java.util.Optional;
import java.util.UUID;

/** Outbound port for verifying store existence and validating merchant ownership. */
public interface StoreVerificationPort {

  /**
   * Retrieves the owner user identifier of the specified store, if the store exists.
   *
   * @param storeUid unique public identifier of the store
   * @return an Optional containing the owner user UUID, or empty if the store does not exist
   */
  Optional<UUID> findStoreOwnerUid(UUID storeUid);

  /**
   * Retrieves the commercial name of the specified store, if the store exists.
   *
   * @param storeUid unique public identifier of the store
   * @return an Optional containing the store name, or empty if the store does not exist
   */
  default Optional<String> findStoreName(UUID storeUid) {
    return Optional.empty();
  }

  /**
   * Retrieves the commercial category of the specified store, if the store exists.
   *
   * @param storeUid unique public identifier of the store
   * @return an Optional containing the store category, or empty if the store does not exist
   */
  default Optional<String> findStoreCategory(UUID storeUid) {
    return Optional.empty();
  }

  /** Store metadata summary (name and category) retrieved in a single lookup. */
  record StoreInfo(String name, String category) {}

  /**
   * Retrieves both store name and category in a single lookup when supported by the adapter.
   *
   * @param storeUid unique public identifier of the store
   * @return an Optional containing {@link StoreInfo}, or empty if unavailable
   */
  default Optional<StoreInfo> findStoreInfo(UUID storeUid) {
    Optional<String> name = findStoreName(storeUid);
    Optional<String> category = findStoreCategory(storeUid);
    if (name.isEmpty() && category.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(new StoreInfo(name.orElse(null), category.orElse(null)));
  }
}
