package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.GiftNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.CatalogGiftItem;
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
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Application service implementing use cases for managing and querying the gift catalog (Story
 * SLE-60).
 */
@Service
@SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.TooManyMethods"})
public class GiftCatalogService implements ManageGiftCatalogUseCase, GetGiftCatalogQuery {

  private static final String ROLE_ADMIN = "ADMIN";

  private final LoyaltyGiftPersistencePort persistencePort;
  private final StoreVerificationPort storeVerificationPort;
  private final GiftImageStoragePort giftImageStoragePort;
  private final TransactionTemplate transactionTemplate;

  public GiftCatalogService(
      LoyaltyGiftPersistencePort persistencePort,
      StoreVerificationPort storeVerificationPort,
      GiftImageStoragePort giftImageStoragePort,
      TransactionTemplate transactionTemplate) {
    this.transactionTemplate = java.util.Objects.requireNonNull(transactionTemplate);
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
    // Network I/O (store-service, S3) is kept outside of the DB transaction.
    validateStoreOwnership(storeId, userId, userRole);

    boolean uploaded = imageStream != null && imageSize > 0;
    String finalImageUrl =
        uploaded
            ? giftImageStoragePort.storeImage(imageStream, imageSize, originalFilename, contentType)
            : command.imageUrl();

    try {
      return transactionTemplate.execute(
          status ->
              persistencePort.save(
                  new LoyaltyGift(
                      storeId,
                      command.name(),
                      command.description(),
                      command.pointsCost(),
                      command.stock(),
                      finalImageUrl)));
    } catch (RuntimeException ex) {
      if (uploaded) {
        deleteImageQuietly(finalImageUrl);
      }
      throw ex;
    }
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

    boolean uploaded = imageStream != null && imageSize > 0;
    String newImageUrl =
        uploaded
            ? giftImageStoragePort.storeImage(imageStream, imageSize, originalFilename, contentType)
            : null;

    String[] previousImageUrl = new String[1];
    LoyaltyGift saved;
    try {
      saved =
          transactionTemplate.execute(
              status -> {
                LoyaltyGift gift =
                    persistencePort
                        .findByIdAndStoreIdForUpdate(giftId, storeId)
                        .orElseThrow(() -> new GiftNotFoundException(storeId, giftId));
                previousImageUrl[0] = gift.getImageUrl();
                String finalImageUrl =
                    newImageUrl != null
                        ? newImageUrl
                        : (command.imageUrl() != null ? command.imageUrl() : gift.getImageUrl());
                gift.update(
                    command.name(), command.description(), command.pointsCost(), finalImageUrl);
                return persistencePort.save(gift);
              });
    } catch (RuntimeException ex) {
      if (uploaded) {
        deleteImageQuietly(newImageUrl);
      }
      throw ex;
    }

    // Old image is removed only once the new state is committed.
    if (uploaded
        && previousImageUrl[0] != null
        && !previousImageUrl[0].isBlank()
        && !previousImageUrl[0].equals(newImageUrl)) {
      deleteImageQuietly(previousImageUrl[0]);
    }
    return saved;
  }

  @Override
  public LoyaltyGift adjustStock(
      Long storeId, Long giftId, int delta, Long userId, String userRole) {
    validateStoreOwnership(storeId, userId, userRole);
    return transactionTemplate.execute(
        status -> {
          LoyaltyGift gift =
              persistencePort
                  .findByIdAndStoreIdForUpdate(giftId, storeId)
                  .orElseThrow(() -> new GiftNotFoundException(storeId, giftId));
          gift.adjustStock(delta);
          return persistencePort.save(gift);
        });
  }

  @Override
  public void deleteGift(Long storeId, Long giftId, Long userId, String userRole) {
    validateStoreOwnership(storeId, userId, userRole);
    transactionTemplate.executeWithoutResult(
        status -> {
          LoyaltyGift gift =
              persistencePort
                  .findByIdAndStoreIdForUpdate(giftId, storeId)
                  .orElseThrow(() -> new GiftNotFoundException(storeId, giftId));
          persistencePort.deleteById(gift.getId());
        });
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
  public Optional<GiftImageResource> getGiftImage(Long storeId, Long giftId) {
    LoyaltyGift gift = getGiftById(storeId, giftId);
    if (gift.getImageUrl() == null || gift.getImageUrl().isBlank()) {
      return Optional.empty();
    }
    return giftImageStoragePort.downloadImage(gift.getImageUrl());
  }

  @Override
  @Transactional(readOnly = true)
  public List<CatalogGiftItem> getPublicCatalog(Long storeId, String category) {
    List<LoyaltyGift> activeGifts =
        storeId != null
            ? persistencePort.findActiveByStoreId(storeId)
            : persistencePort.findAllActive();

    java.util.Map<Long, String> storeNames = new java.util.HashMap<>();
    java.util.Map<Long, String> storeCategories = new java.util.HashMap<>();

    return activeGifts.stream()
        .map(
            gift -> {
              Long sId = gift.getStoreId();
              String sName =
                  storeNames.computeIfAbsent(
                      sId, id -> storeVerificationPort.findStoreName(id).orElse(null));
              String sCat =
                  storeCategories.computeIfAbsent(
                      sId, id -> storeVerificationPort.findStoreCategory(id).orElse(null));
              return CatalogGiftItem.of(gift, sName, sCat);
            })
        .filter(
            item -> {
              if (category == null || category.isBlank()) {
                return true;
              }
              return item.category() != null
                  && item.category().trim().equalsIgnoreCase(category.trim());
            })
        .toList();
  }

  private void deleteImageQuietly(String imageUrl) {
    try {
      giftImageStoragePort.deleteImage(imageUrl);
    } catch (RuntimeException ex) {
      // Best-effort cleanup: an orphaned object must never fail a committed business operation.
      org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GiftCatalogService.class);
      if (log.isWarnEnabled()) {
        log.warn("Could not delete gift image {}: {}", imageUrl, ex.getMessage());
      }
    }
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
