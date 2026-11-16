package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.util.List;

/** Inbound query port for retrieving loyalty gifts. */
public interface GetGiftCatalogQuery {

  List<LoyaltyGift> getGiftsByStore(Long storeId, boolean includeInactive);

  LoyaltyGift getGiftById(Long storeId, Long giftId);
}
