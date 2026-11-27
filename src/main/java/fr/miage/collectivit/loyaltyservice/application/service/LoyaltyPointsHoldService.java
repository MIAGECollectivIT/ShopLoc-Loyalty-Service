package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.GiftNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InsufficientLoyaltyPointsException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InvalidGiftException;
import fr.miage.collectivit.loyaltyservice.domain.exception.PriorPurchaseRequiredException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserNotVfpException;
import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.SalesTransactionPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.CreateLoyaltyHoldRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.CreateLoyaltyHoldRequest.HoldItemRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.LoyaltyHoldResponse;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.LoyaltyHoldResponse.HoldItemResponse;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyPointsHoldItemJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyPointsHoldJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyPointsHoldJpaEntity.HoldStatus;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataLoyaltyPointsHoldRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages atomic loyalty points holds and gift stock reservation for Click & Collect orders (Story
 * SLE-41 / SLE-292).
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings({
  "PMD.LawOfDemeter",
  "PMD.ExcessiveImports",
  "PMD.CouplingBetweenObjects",
  "PMD.AvoidInstantiatingObjectsInLoops"
})
public class LoyaltyPointsHoldService {

  private final LoyaltyGiftPersistencePort loyaltyGiftPersistencePort;
  private final UserPointsPersistencePort userPointsPersistencePort;
  private final UserVfpPersistencePort userVfpPersistencePort;
  private final SalesTransactionPort salesTransactionPort;
  private final SpringDataLoyaltyPointsHoldRepository holdRepository;

  @Transactional
  public LoyaltyHoldResponse createHold(CreateLoyaltyHoldRequest request) {
    String orderRef = request.orderReference().trim();
    Optional<LoyaltyPointsHoldJpaEntity> existing = holdRepository.findByOrderReference(orderRef);
    if (existing.isPresent()) {
      return toResponse(existing.get());
    }

    String userId = request.userId().trim();
    Map<GiftKey, Integer> aggregated = aggregateItems(request.items());
    verifyPriorPurchases(userId, aggregated);

    UserPointsAccount account =
        userPointsPersistencePort
            .findAccountByUserIdForUpdate(userId)
            .orElseThrow(
                () ->
                    new InsufficientLoyaltyPointsException(
                        "Solde de points insuffisant : aucun compte fidélité actif."));

    BigDecimal activeHeld =
        Objects.requireNonNullElse(
            holdRepository.sumTotalPointsByUserIdAndStatus(userId, HoldStatus.ACTIVE),
            BigDecimal.ZERO);
    BigDecimal availablePoints = account.balance().subtract(activeHeld).max(BigDecimal.ZERO);
    boolean isVfp =
        userVfpPersistencePort.findByUserId(userId).map(UserVfpStatus::isVfp).orElse(false);

    List<LoyaltyPointsHoldItemJpaEntity> holdItems = new ArrayList<>();
    long totalPointsLong = reserveGifts(aggregated, isVfp, holdItems);

    BigDecimal totalPoints = BigDecimal.valueOf(totalPointsLong).setScale(1, RoundingMode.HALF_UP);
    if (availablePoints.compareTo(totalPoints) < 0) {
      throw new InsufficientLoyaltyPointsException(
          String.format(
              Locale.ROOT,
              "Solde de points insuffisant : disponible %.1f pts, requis %.1f pts.",
              availablePoints,
              totalPoints));
    }

    LoyaltyPointsHoldJpaEntity hold =
        LoyaltyPointsHoldJpaEntity.builder()
            .uid(UUID.randomUUID())
            .orderReference(orderRef)
            .userId(userId)
            .totalPoints(totalPoints)
            .status(HoldStatus.ACTIVE)
            .expiresAt(request.expiresAt())
            .items(new ArrayList<>())
            .build();
    holdItems.forEach(hold::addItem);

    LoyaltyPointsHoldJpaEntity saved = holdRepository.save(hold);
    if (log.isInfoEnabled()) {
      log.info(
          "Created loyalty hold for order={}, user={}, totalPoints={}",
          orderRef,
          userId,
          totalPoints);
    }
    return toResponse(saved);
  }

  private void verifyPriorPurchases(String userId, Map<GiftKey, Integer> aggregated) {
    Set<UUID> distinctStores =
        aggregated.keySet().stream()
            .map(GiftKey::storeUid)
            .collect(java.util.stream.Collectors.toSet());
    for (UUID storeUid : distinctStores) {
      boolean hasLocalEarn = userPointsPersistencePort.hasPriorPurchaseInStore(userId, storeUid);
      if (!hasLocalEarn && !salesTransactionPort.hasCustomerPurchasedInStore(userId, storeUid)) {
        throw new PriorPurchaseRequiredException(
            "Un achat antérieur chez ce commerçant est requis pour commander un cadeau.");
      }
    }
  }

  private long reserveGifts(
      Map<GiftKey, Integer> aggregated,
      boolean isVfp,
      List<LoyaltyPointsHoldItemJpaEntity> holdItems) {
    long totalPointsLong = 0L;
    for (Map.Entry<GiftKey, Integer> entry : aggregated.entrySet()) {
      UUID storeUid = entry.getKey().storeUid();
      UUID giftUid = entry.getKey().giftUid();
      int qty = entry.getValue();

      LoyaltyGift gift =
          loyaltyGiftPersistencePort
              .findByUidAndStoreUidForUpdate(giftUid, storeUid)
              .orElseThrow(() -> new GiftNotFoundException(storeUid, giftUid));

      if (!gift.isActive()) {
        throw new InvalidGiftException("Le cadeau \"" + gift.getName() + "\" n'est plus actif.");
      }
      if (gift.isVfpExclusive() && !isVfp) {
        throw new UserNotVfpException(
            "Le cadeau \"" + gift.getName() + "\" est réservé aux membres VFP.");
      }

      gift.adjustStock(-qty);
      loyaltyGiftPersistencePort.save(gift);

      long linePoints = Math.multiplyExact((long) gift.getPointsCost(), qty);
      totalPointsLong = Math.addExact(totalPointsLong, linePoints);

      holdItems.add(
          LoyaltyPointsHoldItemJpaEntity.builder()
              .storeUid(storeUid)
              .giftUid(giftUid)
              .giftName(gift.getName())
              .unitPointsCost(gift.getPointsCost())
              .quantity(qty)
              .build());
    }
    return totalPointsLong;
  }

  @Transactional
  public void releaseHold(String orderReference) {
    if (orderReference == null || orderReference.isBlank()) {
      return;
    }
    Optional<LoyaltyPointsHoldJpaEntity> optHold =
        holdRepository.findByOrderReferenceForUpdate(orderReference.trim());
    if (optHold.isEmpty()) {
      return;
    }
    LoyaltyPointsHoldJpaEntity hold = optHold.get();
    if (hold.getStatus() != HoldStatus.ACTIVE) {
      return;
    }
    restoreGiftStocks(hold);
    hold.setStatus(HoldStatus.RELEASED);
    hold.setUpdatedAt(Instant.now());
    holdRepository.save(hold);
    if (log.isInfoEnabled()) {
      log.info("Released loyalty hold for order={}", hold.getOrderReference());
    }
  }

  @Scheduled(fixedDelay = 60_000L)
  @Transactional
  public void expireStaleHolds() {
    Instant now = Instant.now();
    List<LoyaltyPointsHoldJpaEntity> expired =
        holdRepository.findByStatusAndExpiresAtBefore(HoldStatus.ACTIVE, now);
    for (LoyaltyPointsHoldJpaEntity hold : expired) {
      restoreGiftStocks(hold);
      hold.setStatus(HoldStatus.RELEASED);
      hold.setUpdatedAt(now);
    }
    if (!expired.isEmpty()) {
      holdRepository.saveAll(expired);
    }
  }

  private void restoreGiftStocks(LoyaltyPointsHoldJpaEntity hold) {
    for (LoyaltyPointsHoldItemJpaEntity item : hold.getItems()) {
      loyaltyGiftPersistencePort
          .findByUidAndStoreUidForUpdate(item.getGiftUid(), item.getStoreUid())
          .ifPresent(
              gift -> {
                gift.adjustStock(item.getQuantity());
                loyaltyGiftPersistencePort.save(gift);
              });
    }
  }

  private Map<GiftKey, Integer> aggregateItems(List<HoldItemRequest> items) {
    Map<GiftKey, Integer> map = new LinkedHashMap<>();
    for (HoldItemRequest item : items) {
      map.merge(new GiftKey(item.storeUid(), item.giftUid()), item.quantity(), Math::addExact);
    }
    return map;
  }

  private LoyaltyHoldResponse toResponse(LoyaltyPointsHoldJpaEntity hold) {
    List<HoldItemResponse> itemResponses =
        hold.getItems().stream()
            .map(
                i ->
                    new HoldItemResponse(
                        i.getStoreUid(),
                        i.getGiftUid(),
                        i.getGiftName(),
                        i.getUnitPointsCost(),
                        i.getQuantity()))
            .toList();
    return new LoyaltyHoldResponse(
        hold.getUid(),
        hold.getOrderReference(),
        hold.getUserId(),
        hold.getTotalPoints(),
        hold.getStatus().name(),
        hold.getExpiresAt(),
        itemResponses);
  }

  private record GiftKey(UUID storeUid, UUID giftUid) {}
}
