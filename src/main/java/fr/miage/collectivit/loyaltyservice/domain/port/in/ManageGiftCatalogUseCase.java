package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import java.io.InputStream;

/** Inbound port for managing (create, update, adjust stock, delete) a store's gift catalog. */
public interface ManageGiftCatalogUseCase {

  LoyaltyGift createGift(Long storeId, GiftCreateCommand command, Long userId, String userRole);

  LoyaltyGift createGift(
      Long storeId,
      GiftCreateCommand command,
      InputStream imageStream,
      long imageSize,
      String originalFilename,
      String contentType,
      Long userId,
      String userRole);

  LoyaltyGift updateGift(
      Long storeId, Long giftId, GiftUpdateCommand command, Long userId, String userRole);

  LoyaltyGift updateGift(
      Long storeId,
      Long giftId,
      GiftUpdateCommand command,
      InputStream imageStream,
      long imageSize,
      String originalFilename,
      String contentType,
      Long userId,
      String userRole);

  LoyaltyGift adjustStock(Long storeId, Long giftId, int delta, Long userId, String userRole);

  void deleteGift(Long storeId, Long giftId, Long userId, String userRole);
}
