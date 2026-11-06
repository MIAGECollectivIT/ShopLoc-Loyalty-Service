package fr.miage.collectivit.loyaltyservice.domain.port.out;

/** Port sortant pour vérifier l'existence d'un commerce et valider la propriété d'un commerçant. */
public interface StoreVerificationPort {

  /**
   * Vérifie si l'utilisateur donné est le propriétaire du commerce.
   *
   * @param storeId identifiant unique du commerce
   * @param userId identifiant interne de l'utilisateur commerçant (dbId)
   * @return true si le commerce existe et appartient à cet utilisateur, false sinon
   */
  boolean isStoreOwnedBy(Long storeId, Long userId);

  /**
   * Vérifie si le commerce existe.
   *
   * @param storeId identifiant unique du commerce
   * @return true si le commerce existe, false sinon
   */
  boolean storeExists(Long storeId);
}
