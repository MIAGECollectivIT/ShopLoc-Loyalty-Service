package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.CatalogGiftItem;
import fr.miage.collectivit.loyaltyservice.domain.model.GiftImageResource;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Inbound query port for retrieving loyalty gifts. */
public interface GetGiftCatalogQuery {

  List<LoyaltyGift> getGiftsByStore(UUID storeUid, boolean includeInactive);

  LoyaltyGift getGiftByUid(UUID storeUid, UUID giftUid);

  Optional<GiftImageResource> getGiftImage(UUID storeUid, UUID giftUid);

  List<CatalogGiftItem> getPublicCatalog(UUID storeUid, String category);
}
