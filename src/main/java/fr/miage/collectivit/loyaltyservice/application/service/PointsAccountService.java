package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.MerchantLoyaltyRateNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ConfigureMerchantLoyaltyRateUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetMerchantLoyaltyRateQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ProcessOrderPaidLoyaltyUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ProcessPhysicalPaymentLoyaltyUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.OrderPaidKafkaEvent;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.PhysicalPaymentCompletedEvent;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.StoreOrderBreakdown;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Application service orchestrating loyalty account and merchant rate use cases
 * (PointsAccountService component prescribed by the architecture model).
 */
@Slf4j
@Service
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public class PointsAccountService
    implements ConfigureMerchantLoyaltyRateUseCase,
        GetMerchantLoyaltyRateQuery,
        ProcessPhysicalPaymentLoyaltyUseCase,
        ProcessOrderPaidLoyaltyUseCase {

  private static final String ROLE_ADMIN = "ADMIN";
  private static final BigDecimal DEFAULT_EURO_AMOUNT = new BigDecimal("1.00");
  private static final BigDecimal DEFAULT_POINTS_REWARD = new BigDecimal("1.0");

  private final MerchantLoyaltyRatePersistencePort persistencePort;
  private final StoreVerificationPort storeVerificationPort;
  private final UserPointsPersistencePort userPointsPersistencePort;
  private final TransactionTemplate transactionTemplate;

  @Autowired
  public PointsAccountService(
      MerchantLoyaltyRatePersistencePort persistencePort,
      StoreVerificationPort storeVerificationPort,
      UserPointsPersistencePort userPointsPersistencePort,
      TransactionTemplate transactionTemplate) {
    this.persistencePort = persistencePort;
    this.storeVerificationPort = storeVerificationPort;
    this.userPointsPersistencePort = userPointsPersistencePort;
    this.transactionTemplate = transactionTemplate;
  }

  @Override
  public MerchantLoyaltyRate configureRate(
      UUID storeUid,
      BigDecimal euroAmount,
      BigDecimal pointsReward,
      UUID callerUserId,
      String callerRole) {
    // Phase 1: External validation (out-of-transaction to avoid holding DB connections during
    // network I/O)
    validateStoreOwnership(storeUid, callerUserId, callerRole);

    // Phase 2: Transactional read/write persistence with concurrency recovery
    return executeInTransaction(() -> persistRate(storeUid, euroAmount, pointsReward));
  }

  private <T> T executeInTransaction(Supplier<T> action) {
    if (transactionTemplate != null) {
      return transactionTemplate.execute(status -> action.get());
    }
    return action.get();
  }

  private MerchantLoyaltyRate persistRate(
      UUID storeUid, BigDecimal euroAmount, BigDecimal pointsReward) {
    try {
      return persistencePort
          .findByStoreUid(storeUid)
          .map(
              existing -> {
                existing.updateRates(euroAmount, pointsReward);
                return persistencePort.save(existing);
              })
          .orElseGet(
              () -> {
                MerchantLoyaltyRate newRate =
                    new MerchantLoyaltyRate(storeUid, euroAmount, pointsReward);
                return persistencePort.save(newRate);
              });
    } catch (DataIntegrityViolationException ex) {
      // Concurrency recovery: another transaction concurrently created the rate for this store
      return persistencePort
          .findByStoreUid(storeUid)
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
  public MerchantLoyaltyRate getRateByStoreUid(UUID storeUid) {
    return persistencePort
        .findByStoreUid(storeUid)
        .orElseThrow(() -> new MerchantLoyaltyRateNotFoundException(storeUid));
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

  @Override
  @Transactional
  public void processPhysicalPayment(PhysicalPaymentCompletedEvent event) {
    if (event == null || !hasText(event.transactionReference())) {
      return;
    }

    String txRef = event.transactionReference().trim();
    if (isTransactionAlreadyProcessed(txRef)) {
      return;
    }

    MerchantLoyaltyRate rate = resolveStoreRate(event.storeUid());
    BigDecimal pointsEarned =
        calculatePoints(event.amount(), rate.getEuroAmount(), rate.getPointsReward());

    String customerId = hasText(event.customerId()) ? event.customerId().trim() : null;
    String clientCode = hasText(event.clientCode()) ? event.clientCode().trim() : null;
    Optional<UserPointsAccount> existingAccountOpt = findExistingAccount(customerId, clientCode);

    Instant now = event.timestamp() != null ? event.timestamp() : Instant.now();
    UserPointsAccount accountToSave =
        buildAccountToSave(existingAccountOpt, customerId, clientCode, pointsEarned, now);

    if (userPointsPersistencePort != null) {
      UserPointsAccount savedAccount = userPointsPersistencePort.saveAccount(accountToSave);
      recordPointsCredit(savedAccount, event, rate, pointsEarned, accountToSave.balance(), now);
    }
  }

  @Override
  @Transactional
  public void processOrderPaid(OrderPaidKafkaEvent event) {
    if (event == null || !hasText(event.orderReference())) {
      return;
    }

    String orderRef = event.orderReference().trim();
    if (isTransactionAlreadyProcessed(orderRef)) {
      return;
    }

    BigDecimal totalPointsEarned = computeOrderPoints(event);
    if (totalPointsEarned.compareTo(BigDecimal.ZERO) <= 0) {
      return;
    }

    creditOrderPaidPoints(event, totalPointsEarned);
  }

  private BigDecimal computeOrderPoints(OrderPaidKafkaEvent event) {
    if (event.stores() != null && !event.stores().isEmpty()) {
      return computeStoresBreakdownPoints(event);
    }
    if (event.totalAmount() != null) {
      return calculatePoints(event.totalAmount(), DEFAULT_EURO_AMOUNT, DEFAULT_POINTS_REWARD);
    }
    return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
  }

  private BigDecimal computeStoresBreakdownPoints(OrderPaidKafkaEvent event) {
    BigDecimal total = BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
    for (StoreOrderBreakdown store : event.stores()) {
      if (store != null && store.storeUid() != null && store.amount() != null) {
        MerchantLoyaltyRate rate = resolveStoreRate(store.storeUid());
        BigDecimal points =
            calculatePoints(store.amount(), rate.getEuroAmount(), rate.getPointsReward());
        total = total.add(points);
      }
    }
    return total;
  }

  private void creditOrderPaidPoints(OrderPaidKafkaEvent event, BigDecimal totalPointsEarned) {
    String customerId = hasText(event.customerId()) ? event.customerId().trim() : null;
    String clientCode = hasText(event.clientCode()) ? event.clientCode().trim() : null;
    Optional<UserPointsAccount> existingAccountOpt = findExistingAccount(customerId, clientCode);

    Instant now = event.timestamp() != null ? event.timestamp() : Instant.now();
    UserPointsAccount accountToSave =
        buildAccountToSave(existingAccountOpt, customerId, clientCode, totalPointsEarned, now);

    if (userPointsPersistencePort != null) {
      UserPointsAccount savedAccount = userPointsPersistencePort.saveAccount(accountToSave);
      recordOrderPaidPointsCredit(
          savedAccount, event, totalPointsEarned, accountToSave.balance(), now);
    }
  }

  private void recordOrderPaidPointsCredit(
      UserPointsAccount savedAccount,
      OrderPaidKafkaEvent event,
      BigDecimal pointsEarned,
      BigDecimal newBalance,
      Instant now) {
    String orderRef = event.orderReference().trim();
    UUID storeUid = null;
    String storeName = "Multi-commerces";
    if (event.stores() != null && event.stores().size() == 1) {
      StoreOrderBreakdown singleStore = event.stores().get(0);
      storeUid = singleStore.storeUid();
      storeName =
          singleStore.storeName() != null
              ? singleStore.storeName()
              : resolveStoreName(singleStore.storeUid());
    } else if (event.stores() != null && event.stores().size() > 1) {
      storeName = "Multi-commerces (" + event.stores().size() + ")";
    }

    PointsTransaction transaction =
        new PointsTransaction(
            null,
            null,
            orderRef,
            savedAccount.userId(),
            storeUid,
            storeName,
            PointsTransactionType.EARN,
            pointsEarned,
            newBalance,
            "ORDER_CLICK_AND_COLLECT",
            "Gain de points commande Click & Collect " + orderRef + " - " + storeName,
            now);

    userPointsPersistencePort.saveTransaction(savedAccount.id(), transaction);

    if (log.isInfoEnabled()) {
      log.info(
          "Points credited for click and collect order: orderRef={}, userId={}, pointsEarned={}, newBalance={}",
          orderRef,
          savedAccount.userId(),
          pointsEarned,
          newBalance);
    }
  }

  private boolean isTransactionAlreadyProcessed(String txRef) {
    if (userPointsPersistencePort != null
        && userPointsPersistencePort.existsTransactionByReference(txRef)) {
      if (log.isInfoEnabled()) {
        log.info(
            "Physical payment transaction reference {} already processed, skipping (idempotence)",
            txRef);
      }
      return true;
    }
    return false;
  }

  private MerchantLoyaltyRate resolveStoreRate(UUID storeUid) {
    return persistencePort
        .findByStoreUid(storeUid)
        .orElseGet(
            () -> new MerchantLoyaltyRate(storeUid, DEFAULT_EURO_AMOUNT, DEFAULT_POINTS_REWARD));
  }

  private Optional<UserPointsAccount> findExistingAccount(String customerId, String clientCode) {
    if (userPointsPersistencePort == null) {
      return Optional.empty();
    }
    if (customerId != null) {
      Optional<UserPointsAccount> byUserId =
          userPointsPersistencePort.findAccountByUserIdForUpdate(customerId);
      if (byUserId.isPresent()) {
        return byUserId;
      }
    }
    if (clientCode != null) {
      return userPointsPersistencePort.findAccountByClientCodeForUpdate(clientCode);
    }
    return Optional.empty();
  }

  private UserPointsAccount buildAccountToSave(
      Optional<UserPointsAccount> existingAccountOpt,
      String customerId,
      String clientCode,
      BigDecimal pointsEarned,
      Instant now) {
    if (existingAccountOpt.isPresent()) {
      UserPointsAccount existing = existingAccountOpt.get();
      BigDecimal newBalance =
          existing.balance().add(pointsEarned).setScale(1, RoundingMode.HALF_UP);
      return new UserPointsAccount(
          existing.id(),
          existing.userId(),
          clientCode != null ? clientCode : existing.clientCode(),
          newBalance,
          existing.createdAt(),
          now);
    }
    String effectiveUserId = customerId != null ? customerId : clientCode;
    BigDecimal newBalance = pointsEarned.setScale(1, RoundingMode.HALF_UP);
    return new UserPointsAccount(null, effectiveUserId, clientCode, newBalance, now, now);
  }

  private void recordPointsCredit(
      UserPointsAccount savedAccount,
      PhysicalPaymentCompletedEvent event,
      MerchantLoyaltyRate rate,
      BigDecimal pointsEarned,
      BigDecimal newBalance,
      Instant now) {
    String txRef = event.transactionReference().trim();
    String rateApplied =
        String.format(
            Locale.ROOT, "%.2f EUR = %.1f PTS", rate.getEuroAmount(), rate.getPointsReward());
    String storeName = resolveStoreName(event.storeUid());

    PointsTransaction transaction =
        new PointsTransaction(
            null,
            null,
            txRef,
            savedAccount.userId(),
            event.storeUid(),
            storeName,
            PointsTransactionType.EARN,
            pointsEarned,
            newBalance,
            "PHYSICAL_PAYMENT",
            "Gain de points en caisse physique - " + storeName + " (" + rateApplied + ")",
            now);

    userPointsPersistencePort.saveTransaction(savedAccount.id(), transaction);

    if (log.isInfoEnabled()) {
      log.info(
          "Points credited for physical payment: txRef={}, userId={}, storeUid={}, pointsEarned={}, newBalance={}",
          txRef,
          savedAccount.userId(),
          event.storeUid(),
          pointsEarned,
          newBalance);
    }
  }

  private String resolveStoreName(UUID storeUid) {
    if (storeVerificationPort != null) {
      return storeVerificationPort
          .findStoreName(storeUid)
          .filter(this::hasText)
          .orElse("Magasin #" + storeUid);
    }
    return "Magasin #" + storeUid;
  }

  public BigDecimal calculatePoints(
      BigDecimal amount, BigDecimal euroAmount, BigDecimal pointsReward) {
    if (amount == null
        || amount.compareTo(BigDecimal.ZERO) <= 0
        || euroAmount == null
        || euroAmount.compareTo(BigDecimal.ZERO) <= 0
        || pointsReward == null
        || pointsReward.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO.setScale(1, RoundingMode.UNNECESSARY);
    }

    BigDecimal rawPoints =
        amount.multiply(pointsReward).divide(euroAmount, 4, RoundingMode.HALF_UP);
    BigDecimal halfSteps =
        rawPoints.multiply(BigDecimal.valueOf(2)).setScale(0, RoundingMode.FLOOR);
    return halfSteps.divide(BigDecimal.valueOf(2), 1, RoundingMode.UNNECESSARY);
  }

  private boolean hasText(String str) {
    return str != null && !str.isBlank();
  }
}
