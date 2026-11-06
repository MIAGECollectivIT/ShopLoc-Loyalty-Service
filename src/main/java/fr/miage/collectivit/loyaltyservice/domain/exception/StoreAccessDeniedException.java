package fr.miage.collectivit.loyaltyservice.domain.exception;

/**
 * Exception levée lorsqu'un utilisateur tente d'accéder ou de modifier un magasin sans en être
 * propriétaire.
 */
public class StoreAccessDeniedException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StoreAccessDeniedException(String message) {
    super(message);
  }
}
