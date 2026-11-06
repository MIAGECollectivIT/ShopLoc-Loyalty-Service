package fr.miage.collectivit.loyaltyservice.domain.exception;

/** Exception levée lorsqu'un commerce est introuvable auprès du service de gestion des magasins. */
public class StoreNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public StoreNotFoundException(Long storeId) {
    super("Le commerce avec l'identifiant " + storeId + " est introuvable");
  }
}
