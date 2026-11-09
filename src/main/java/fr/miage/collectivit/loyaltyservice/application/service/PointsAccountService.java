package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.MerchantLoyaltyRateNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ConfigureMerchantLoyaltyRateUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetMerchantLoyaltyRateQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service orchestrating loyalty account and merchant rate use cases
 * (PointsAccountService component prescribed by the architecture model).
 */
@Service
@RequiredArgsConstructor
public class PointsAccountService
    implements ConfigureMerchantLoyaltyRateUseCase, GetMerchantLoyaltyRateQuery {

  private static final String ROLE_ADMIN = "ADMIN";

  private final MerchantLoyaltyRatePersistencePort persistencePort;
  private final StoreVerificationPort storeVerificationPort;

  @Override
  @Transactional
  public MerchantLoyaltyRate configureRate(
      Long storeId,
      BigDecimal euroAmount,
      BigDecimal pointsReward,
      Long callerUserId,
      String callerRole) {
    validateStoreOwnership(storeId, callerUserId, callerRole);

    return persistencePort
        .findByStoreId(storeId)
        .map(
            existing -> {
              existing.updateRates(euroAmount, pointsReward);
              return persistencePort.save(existing);
            })
        .orElseGet(
            () -> {
              MerchantLoyaltyRate newRate =
                  new MerchantLoyaltyRate(storeId, euroAmount, pointsReward);
              return persistencePort.save(newRate);
            });
  }

  @Override
  @Transactional(readOnly = true)
  public MerchantLoyaltyRate getRateByStoreId(Long storeId) {
    return persistencePort
        .findByStoreId(storeId)
        .orElseThrow(() -> new MerchantLoyaltyRateNotFoundException(storeId));
  }

  private void validateStoreOwnership(Long storeId, Long callerUserId, String callerRole) {
    if (ROLE_ADMIN.equalsIgnoreCase(callerRole)) {
      return;
    }
    if (callerUserId == null) {
      throw new StoreAccessDeniedException(
          "Missing user identifier for modifying store " + storeId);
    }
    if (!storeVerificationPort.storeExists(storeId)) {
      throw new StoreNotFoundException(storeId);
    }
    if (!storeVerificationPort.isStoreOwnedBy(storeId, callerUserId)) {
      throw new StoreAccessDeniedException(
          "Access denied: you are not the owner of store " + storeId);
    }
  }
}
