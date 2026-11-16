package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.GiftNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetGiftCatalogQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftCreateCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GiftUpdateCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ManageGiftCatalogUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import java.util.List;
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

  public GiftCatalogService(
      LoyaltyGiftPersistencePort persistencePort, StoreVerificationPort storeVerificationPort) {
    this.persistencePort = persistencePort;
    this.storeVerificationPort = storeVerificationPort;
  }

  @Override
  public LoyaltyGift createGift(
      Long storeId, GiftCreateCommand command, Long userId, String userRole) {
    validateStoreOwnership(storeId, userId, userRole);
    LoyaltyGift gift =
        new LoyaltyGift(
            storeId,
            command.name(),
            command.description(),
            command.pointsCost(),
            command.stock(),
            command.imageUrl());
    return persistencePort.save(gift);
  }

  @Override
  public LoyaltyGift updateGift(
      Long storeId, Long giftId, GiftUpdateCommand command, Long userId, String userRole) {
    validateStoreOwnership(storeId, userId, userRole);
    LoyaltyGift gift =
        persistencePort
            .findByIdAndStoreId(giftId, storeId)
            .orElseThrow(() -> new GiftNotFoundException(storeId, giftId));
    gift.update(command.name(), command.description(), command.pointsCost(), command.imageUrl());
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
