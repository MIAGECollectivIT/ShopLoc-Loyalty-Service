package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.io.InputStream;
import java.util.UUID;

/** Inbound port for managing (create, update, adjust stock, delete) a store's gift catalog. */
public interface ManageGiftCatalogUseCase {

  LoyaltyGift createGift(UUID storeUid, GiftCreateCommand command, UUID userId, String userRole);

  LoyaltyGift createGift(
      UUID storeUid,
      GiftCreateCommand command,
      InputStream imageStream,
      long imageSize,
      String originalFilename,
      String contentType,
      UUID userId,
      String userRole);

  LoyaltyGift updateGift(
      UUID storeUid, UUID giftUid, GiftUpdateCommand command, UUID userId, String userRole);

  LoyaltyGift updateGift(
      UUID storeUid,
      UUID giftUid,
      GiftUpdateCommand command,
      InputStream imageStream,
      long imageSize,
      String originalFilename,
      String contentType,
      UUID userId,
      String userRole);

  LoyaltyGift adjustStock(UUID storeUid, UUID giftUid, int delta, UUID userId, String userRole);

  void deleteGift(UUID storeUid, UUID giftUid, UUID userId, String userRole);
}
