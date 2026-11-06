package fr.miage.collectivit.loyaltyservice.domain.exception;

public class MerchantLoyaltyRateNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MerchantLoyaltyRateNotFoundException(Long storeId) {
    super("Aucun barème de fidélité trouvé pour le commerce ID " + storeId);
  }
}
