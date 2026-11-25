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
import java.util.UUID;
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
      UUID storeUid, GiftCreateCommand command, UUID userId, String userRole) {
    return createGift(storeUid, command, null, 0, null, null, userId, userRole);
  }

  @Override
  public LoyaltyGift createGift(
      UUID storeUid,
      GiftCreateCommand command,
      InputStream imageStream,
      long imageSize,
      String originalFilename,
      String contentType,
      UUID userId,
      String userRole) {
    // Network I/O (store-service, S3) is kept outside of the DB transaction.
    validateStoreOwnership(storeUid, userId, userRole);

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
                      storeUid,
                      command.name(),
                      command.description(),
                      command.pointsCost(),
                      command.stock(),
                      finalImageUrl,
                      command.vfpExclusive())));
    } catch (RuntimeException ex) {
      if (uploaded) {
        deleteImageQuietly(finalImageUrl);
      }
      throw ex;
    }
  }

  @Override
  public LoyaltyGift updateGift(
      UUID storeUid, UUID giftUid, GiftUpdateCommand command, UUID userId, String userRole) {
    return updateGift(storeUid, giftUid, command, null, 0, null, null, userId, userRole);
  }

  @Override
  public LoyaltyGift updateGift(
      UUID storeUid,
      UUID giftUid,
      GiftUpdateCommand command,
      InputStream imageStream,
      long imageSize,
      String originalFilename,
      String contentType,
      UUID userId,
      String userRole) {
    validateStoreOwnership(storeUid, userId, userRole);

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
                        .findByUidAndStoreUidForUpdate(giftUid, storeUid)
                        .orElseThrow(() -> new GiftNotFoundException(storeUid, giftUid));
                previousImageUrl[0] = gift.getImageUrl();
                String finalImageUrl =
                    newImageUrl != null
                        ? newImageUrl
                        : (command.imageUrl() != null ? command.imageUrl() : gift.getImageUrl());
                gift.update(
                    command.name(),
                    command.description(),
                    command.pointsCost(),
                    finalImageUrl,
                    command.vfpExclusive());
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
      UUID storeUid, UUID giftUid, int delta, UUID userId, String userRole) {
    validateStoreOwnership(storeUid, userId, userRole);
    return transactionTemplate.execute(
        status -> {
          LoyaltyGift gift =
              persistencePort
                  .findByUidAndStoreUidForUpdate(giftUid, storeUid)
                  .orElseThrow(() -> new GiftNotFoundException(storeUid, giftUid));
          gift.adjustStock(delta);
          return persistencePort.save(gift);
        });
  }

  @Override
  public void deleteGift(UUID storeUid, UUID giftUid, UUID userId, String userRole) {
    validateStoreOwnership(storeUid, userId, userRole);
    transactionTemplate.executeWithoutResult(
        status -> {
          LoyaltyGift gift =
              persistencePort
                  .findByUidAndStoreUidForUpdate(giftUid, storeUid)
                  .orElseThrow(() -> new GiftNotFoundException(storeUid, giftUid));
          persistencePort.deleteByUid(gift.getUid());
        });
  }

  @Override
  @Transactional(readOnly = true)
  public List<LoyaltyGift> getGiftsByStore(UUID storeUid, boolean includeInactive) {
    if (includeInactive) {
      return persistencePort.findByStoreUid(storeUid);
    }
    return persistencePort.findActiveByStoreUid(storeUid);
  }

  @Override
  @Transactional(readOnly = true)
  public LoyaltyGift getGiftByUid(UUID storeUid, UUID giftUid) {
    return persistencePort
        .findByUidAndStoreUid(giftUid, storeUid)
        .orElseThrow(() -> new GiftNotFoundException(storeUid, giftUid));
  }

  @Override
  public Optional<GiftImageResource> getGiftImage(UUID storeUid, UUID giftUid) {
    LoyaltyGift gift = getGiftByUid(storeUid, giftUid);
    if (gift.getImageUrl() == null || gift.getImageUrl().isBlank()) {
      return Optional.empty();
    }
    return giftImageStoragePort.downloadImage(gift.getImageUrl());
  }

  @Override
  public List<CatalogGiftItem> getPublicCatalog(UUID storeUid, String category) {
    List<LoyaltyGift> activeGifts =
        storeUid != null
            ? persistencePort.findActiveByStoreUid(storeUid)
            : persistencePort.findAllActive();

    java.util.Map<UUID, StoreVerificationPort.StoreInfo> storeInfos = new java.util.HashMap<>();

    return activeGifts.stream()
        .map(
            gift -> {
              UUID currentStoreUid = gift.getStoreUid();
              StoreVerificationPort.StoreInfo info =
                  storeInfos.computeIfAbsent(
                      currentStoreUid,
                      uid ->
                          storeVerificationPort
                              .findStoreInfo(uid)
                              .orElseGet(
                                  () ->
                                      new StoreVerificationPort.StoreInfo(
                                          storeVerificationPort.findStoreName(uid).orElse(null),
                                          storeVerificationPort
                                              .findStoreCategory(uid)
                                              .orElse(null))));
              return CatalogGiftItem.of(gift, info.name(), info.category());
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

  private void validateStoreOwnership(UUID storeUid, UUID callerUserId, String callerRole) {
    if (ROLE_ADMIN.equalsIgnoreCase(callerRole)) {
      return;
    }
    if (callerUserId == null) {
      throw new StoreAccessDeniedException(
          "Missing user identifier for modifying store " + storeUid);
    }
    UUID ownerUid =
        storeVerificationPort
            .findStoreOwnerUid(storeUid)
            .orElseThrow(() -> new StoreNotFoundException(storeUid));

    if (!ownerUid.equals(callerUserId)) {
      throw new StoreAccessDeniedException(
          "Access denied: you are not the owner of store " + storeUid);
    }
  }
}
