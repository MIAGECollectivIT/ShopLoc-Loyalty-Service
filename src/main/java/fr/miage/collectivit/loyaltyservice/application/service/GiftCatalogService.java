package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.GiftNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.GiftImageResource;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetGiftCatalogQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftCreateCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftUpdateCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ManageGiftCatalogUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.GiftImageStoragePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service implementing use cases for managing and querying the gift catalog (Story
 * SLE-60).
 */
@Service
@Transactional
public class GiftCatalogService implements ManageGiftCatalogUseCase, GetGiftCatalogQuery {

  private static final String ROLE_ADMIN = "ADMIN";

  private final LoyaltyGiftPersistencePort persistencePort;
  private final StoreVerificationPort storeVerificationPort;
  private final GiftImageStoragePort giftImageStoragePort;

  public GiftCatalogService(
      LoyaltyGiftPersistencePort persistencePort,
      StoreVerificationPort storeVerificationPort,
      GiftImageStoragePort giftImageStoragePort) {
    this.persistencePort = persistencePort;
    this.storeVerificationPort = storeVerificationPort;
    this.giftImageStoragePort = giftImageStoragePort;
  }

  @Override
  public LoyaltyGift createGift(
      Long storeId, GiftCreateCommand command, Long userId, String userRole) {
    return createGift(storeId, command, null, 0, null, null, userId, userRole);
  }

  @Override
  public LoyaltyGift createGift(
      Long storeId,
      GiftCreateCommand command,
      InputStream imageStream,
      long imageSize,
      String originalFilename,
      String contentType,
      Long userId,
      String userRole) {
    validateStoreOwnership(storeId, userId, userRole);

    String finalImageUrl = command.imageUrl();
    if (imageStream != null && imageSize > 0) {
      finalImageUrl =
          giftImageStoragePort.storeImage(imageStream, imageSize, originalFilename, contentType);
    }

    LoyaltyGift gift =
        new LoyaltyGift(
            storeId,
            command.name(),
            command.description(),
            command.pointsCost(),
            command.stock(),
            finalImageUrl);
    return persistencePort.save(gift);
  }

  @Override
  public LoyaltyGift updateGift(
      Long storeId, Long giftId, GiftUpdateCommand command, Long userId, String userRole) {
    return updateGift(storeId, giftId, command, null, 0, null, null, userId, userRole);
  }

  @Override
  public LoyaltyGift updateGift(
      Long storeId,
      Long giftId,
      GiftUpdateCommand command,
      InputStream imageStream,
      long imageSize,
      String originalFilename,
      String contentType,
      Long userId,
      String userRole) {
    validateStoreOwnership(storeId, userId, userRole);
    LoyaltyGift gift =
        persistencePort
            .findByIdAndStoreId(giftId, storeId)
            .orElseThrow(() -> new GiftNotFoundException(storeId, giftId));

    String finalImageUrl = command.imageUrl() != null ? command.imageUrl() : gift.getImageUrl();
    if (imageStream != null && imageSize > 0) {
      if (gift.getImageUrl() != null && !gift.getImageUrl().isBlank()) {
        giftImageStoragePort.deleteImage(gift.getImageUrl());
      }
      finalImageUrl =
          giftImageStoragePort.storeImage(imageStream, imageSize, originalFilename, contentType);
    }

    gift.update(command.name(), command.description(), command.pointsCost(), finalImageUrl);
    return persistencePort.save(gift);
  }

  @Override
  public LoyaltyGift adjustStock(
      Long storeId, Long giftId, int delta, Long userId, String userRole) {
    validateStoreOwnership(storeId, userId, userRole);
    LoyaltyGift gift =
        persistencePort
            .findByIdAndStoreId(giftId, storeId)
            .orElseThrow(() -> new GiftNotFoundException(storeId, giftId));
    gift.adjustStock(delta);
    return persistencePort.save(gift);
  }

  @Override
  public void deleteGift(Long storeId, Long giftId, Long userId, String userRole) {
    validateStoreOwnership(storeId, userId, userRole);
    LoyaltyGift gift =
        persistencePort
            .findByIdAndStoreId(giftId, storeId)
            .orElseThrow(() -> new GiftNotFoundException(storeId, giftId));
    persistencePort.deleteById(gift.getId());
  }

  @Override
  @Transactional(readOnly = true)
  public List<LoyaltyGift> getGiftsByStore(Long storeId, boolean includeInactive) {
    if (includeInactive) {
      return persistencePort.findByStoreId(storeId);
    }
    return persistencePort.findActiveByStoreId(storeId);
  }

  @Override
  @Transactional(readOnly = true)
  public LoyaltyGift getGiftById(Long storeId, Long giftId) {
    return persistencePort
        .findByIdAndStoreId(giftId, storeId)
        .orElseThrow(() -> new GiftNotFoundException(storeId, giftId));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<GiftImageResource> getGiftImage(Long storeId, Long giftId) {
    LoyaltyGift gift = getGiftById(storeId, giftId);
    if (gift.getImageUrl() == null || gift.getImageUrl().isBlank()) {
      return Optional.empty();
    }
    return giftImageStoragePort.downloadImage(gift.getImageUrl());
  }

  private void validateStoreOwnership(Long storeId, Long callerUserId, String callerRole) {
    if (ROLE_ADMIN.equalsIgnoreCase(callerRole)) {
      return;
    }
    if (callerUserId == null) {
      throw new StoreAccessDeniedException(
          "Missing user identifier for modifying store " + storeId);
    }
    Long ownerId =
        storeVerificationPort
            .findStoreOwnerId(storeId)
            .orElseThrow(() -> new StoreNotFoundException(storeId));

    if (!ownerId.equals(callerUserId)) {
      throw new StoreAccessDeniedException(
          "Access denied: you are not the owner of store " + storeId);
    }
  }
}
