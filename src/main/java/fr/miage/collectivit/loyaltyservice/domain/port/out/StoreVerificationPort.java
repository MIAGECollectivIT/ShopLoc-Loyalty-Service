package fr.miage.collectivit.loyaltyservice.domain.port.out;

/** Outbound port for verifying store existence and validating merchant ownership. */
public interface StoreVerificationPort {

  /**
   * Verifies whether the specified user is the legitimate owner of the store.
   *
   * @param storeId unique identifier of the store
   * @param userId internal identifier of the merchant user (dbId)
   * @return true if the store exists and belongs to this user, false otherwise
   */
  boolean isStoreOwnedBy(Long storeId, Long userId);

  /**
   * Verifies whether the store exists.
   *
   * @param storeId unique identifier of the store
   * @return true if the store exists, false otherwise
   */
  boolean storeExists(Long storeId);
}
