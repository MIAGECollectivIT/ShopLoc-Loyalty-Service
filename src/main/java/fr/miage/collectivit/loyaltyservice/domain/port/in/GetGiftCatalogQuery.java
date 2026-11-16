package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.GiftImageResource;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.util.List;
import java.util.Optional;

/** Inbound query port for retrieving loyalty gifts. */
public interface GetGiftCatalogQuery {

  List<LoyaltyGift> getGiftsByStore(Long storeId, boolean includeInactive);

  LoyaltyGift getGiftById(Long storeId, Long giftId);

  Optional<GiftImageResource> getGiftImage(Long storeId, Long giftId);
}
