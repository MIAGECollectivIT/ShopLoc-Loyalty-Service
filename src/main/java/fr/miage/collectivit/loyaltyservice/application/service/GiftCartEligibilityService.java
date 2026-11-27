package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.port.out.SalesTransactionPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.GiftCartEligibilityResponse;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyPointsHoldJpaEntity.HoldStatus;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataLoyaltyPointsHoldRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Computes customer eligibility for adding loyalty gifts to Click & Collect cart (SLE-292). */
@Service
@RequiredArgsConstructor
@SuppressWarnings("PMD.LawOfDemeter")
public class GiftCartEligibilityService {

  private final UserPointsPersistencePort userPointsPersistencePort;
  private final UserVfpPersistencePort userVfpPersistencePort;
  private final SpringDataLoyaltyPointsHoldRepository holdRepository;
  private final SalesTransactionPort salesTransactionPort;

  @Transactional(readOnly = true)
  public GiftCartEligibilityResponse getEligibility(String userId, UUID storeUid) {
    if (userId == null || userId.isBlank()) {
      return new GiftCartEligibilityResponse(
          BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false, Set.of());
    }
    String normalizedUserId = userId.trim();

    BigDecimal balance =
        userPointsPersistencePort
            .findAccountByUserId(normalizedUserId)
            .map(UserPointsAccount::balance)
            .orElse(BigDecimal.ZERO)
            .setScale(1, RoundingMode.HALF_UP);

    BigDecimal heldPoints =
        Objects.requireNonNullElse(
                holdRepository.sumTotalPointsByUserIdAndStatus(normalizedUserId, HoldStatus.ACTIVE),
                BigDecimal.ZERO)
            .setScale(1, RoundingMode.HALF_UP);

    BigDecimal availablePoints = balance.subtract(heldPoints).max(BigDecimal.ZERO);

    boolean isVfp =
        userVfpPersistencePort
            .findByUserId(normalizedUserId)
            .map(UserVfpStatus::isVfp)
            .orElse(false);

    Set<UUID> eligibleStoreUids =
        new HashSet<>(userPointsPersistencePort.findEligibleStoreUidsForUser(normalizedUserId));
    if (storeUid != null) {
      if (!eligibleStoreUids.contains(storeUid)
          && salesTransactionPort.hasCustomerPurchasedInStore(normalizedUserId, storeUid)) {
        eligibleStoreUids.add(storeUid);
      }
    } else {
      eligibleStoreUids.addAll(
          salesTransactionPort.getCustomerPurchasedStoreUids(normalizedUserId));
    }

    return new GiftCartEligibilityResponse(
        balance, heldPoints, availablePoints, isVfp, Set.copyOf(eligibleStoreUids));
  }
}
