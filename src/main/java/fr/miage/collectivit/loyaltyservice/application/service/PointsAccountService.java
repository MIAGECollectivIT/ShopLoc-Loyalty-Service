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
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Application service orchestrating loyalty account and merchant rate use cases
 * (PointsAccountService component prescribed by the architecture model).
 */
@Service
public class PointsAccountService
    implements ConfigureMerchantLoyaltyRateUseCase, GetMerchantLoyaltyRateQuery {

  private static final String ROLE_ADMIN = "ADMIN";

  private final MerchantLoyaltyRatePersistencePort persistencePort;
  private final StoreVerificationPort storeVerificationPort;
  private final TransactionTemplate transactionTemplate;

  @Autowired
  public PointsAccountService(
      MerchantLoyaltyRatePersistencePort persistencePort,
      StoreVerificationPort storeVerificationPort,
      TransactionTemplate transactionTemplate) {
    this.persistencePort = persistencePort;
    this.storeVerificationPort = storeVerificationPort;
    this.transactionTemplate = transactionTemplate;
  }

  public PointsAccountService(
      MerchantLoyaltyRatePersistencePort persistencePort,
      StoreVerificationPort storeVerificationPort) {
    this(persistencePort, storeVerificationPort, null);
  }

  @Override
  public MerchantLoyaltyRate configureRate(
      Long storeId,
      BigDecimal euroAmount,
      BigDecimal pointsReward,
      Long callerUserId,
      String callerRole) {
    // Phase 1: External validation (out-of-transaction to avoid holding DB connections during
    // network I/O)
    // Phase 1: External validation (out-of-transaction to avoid holding DB connections during network I/O)
    validateStoreOwnership(storeId, callerUserId, callerRole);

    // Phase 2: Transactional read/write persistence with concurrency recovery
    return executeInTransaction(() -> persistRate(storeId, euroAmount, pointsReward));
  }

  private <T> T executeInTransaction(Supplier<T> action) {
    if (transactionTemplate != null) {
      return transactionTemplate.execute(status -> action.get());
    }
    return action.get();
  }

  private MerchantLoyaltyRate persistRate(
      Long storeId, BigDecimal euroAmount, BigDecimal pointsReward) {
    try {
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
    } catch (DataIntegrityViolationException ex) {
      // Concurrency recovery: another transaction concurrently created the rate for this store
      return persistencePort
          .findByStoreId(storeId)
          .map(
              existing -> {
                existing.updateRates(euroAmount, pointsReward);
                return persistencePort.save(existing);
              })
          .orElseThrow(() -> ex);
    }
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
