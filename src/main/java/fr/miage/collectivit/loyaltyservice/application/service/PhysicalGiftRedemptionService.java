package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.exception.DuplicatePhysicalRedemptionException;
import fr.miage.collectivit.loyaltyservice.domain.exception.InsufficientLoyaltyPointsException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.UserNotVfpException;
import fr.miage.collectivit.loyaltyservice.domain.model.PhysicalGiftRedemptionResult;
import fr.miage.collectivit.loyaltyservice.domain.model.PhysicalGiftRedemptionResult.RedeemedGiftItem;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.port.in.RedeemPhysicalGiftsCommand;
import fr.miage.collectivit.loyaltyservice.domain.port.in.RedeemPhysicalGiftsUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Application service orchestrating atomic in-store physical gift redemption against a customer's
 * loyalty points balance (Story SLE-43). Stock is intentionally not checked or decremented as
 * physical redemptions are fulfilled directly from on-site merchant inventory.
 */
@Slf4j
@Service
@SuppressWarnings({"PMD.LawOfDemeter", "PMD.ExcessiveImports"})
public class PhysicalGiftRedemptionService implements RedeemPhysicalGiftsUseCase {

  private static final String ROLE_ADMIN = "ADMIN";
  private static final String SOURCE_TYPE_PHYSICAL_GIFT = "GIFT_REDEEM_PHYSICAL";

  private final LoyaltyGiftPersistencePort loyaltyGiftPersistencePort;
  private final UserPointsPersistencePort userPointsPersistencePort;
  private final StoreVerificationPort storeVerificationPort;
  private final UserVfpPersistencePort userVfpPersistencePort;
  private final TransactionTemplate transactionTemplate;
  private final Clock clock;

  @Autowired
  public PhysicalGiftRedemptionService(
      LoyaltyGiftPersistencePort loyaltyGiftPersistencePort,
      UserPointsPersistencePort userPointsPersistencePort,
      StoreVerificationPort storeVerificationPort,
      UserVfpPersistencePort userVfpPersistencePort,
      TransactionTemplate transactionTemplate) {
    this(
        loyaltyGiftPersistencePort,
        userPointsPersistencePort,
        storeVerificationPort,
        userVfpPersistencePort,
        transactionTemplate,
        Clock.systemUTC());
  }

  public PhysicalGiftRedemptionService(
      LoyaltyGiftPersistencePort loyaltyGiftPersistencePort,
      UserPointsPersistencePort userPointsPersistencePort,
      StoreVerificationPort storeVerificationPort,
      TransactionTemplate transactionTemplate,
      Clock clock) {
    this(
        loyaltyGiftPersistencePort,
        userPointsPersistencePort,
        storeVerificationPort,
        null,
        transactionTemplate,
        clock);
  }

  public PhysicalGiftRedemptionService(
      LoyaltyGiftPersistencePort loyaltyGiftPersistencePort,
      UserPointsPersistencePort userPointsPersistencePort,
      StoreVerificationPort storeVerificationPort,
      UserVfpPersistencePort userVfpPersistencePort,
      TransactionTemplate transactionTemplate,
      Clock clock) {
    this.loyaltyGiftPersistencePort = loyaltyGiftPersistencePort;
    this.userPointsPersistencePort = userPointsPersistencePort;
    this.storeVerificationPort = storeVerificationPort;
    this.userVfpPersistencePort = userVfpPersistencePort;
    this.transactionTemplate = transactionTemplate;
    this.clock = clock;
  }

  @Override
  public PhysicalGiftRedemptionResult redeemPhysicalGifts(
      UUID storeUid, RedeemPhysicalGiftsCommand command, UUID callerUserId, String callerRole) {
    PhysicalGiftRedemptionSupport.validateArguments(storeUid, command);
    Map<UUID, Integer> aggregatedQuantities =
        PhysicalGiftRedemptionSupport.aggregateQuantities(command.items());

    // Keep network I/O outside the DB transaction.
    validateStoreOwnership(storeUid, callerUserId, callerRole);
    String storeName = resolveStoreName(storeUid);

    String customerId = PhysicalGiftRedemptionSupport.trimToNull(command.customerId());
    String clientCode = PhysicalGiftRedemptionSupport.trimToNull(command.clientCode());
    String idempotencyKey = PhysicalGiftRedemptionSupport.trimToNull(command.idempotencyKey());
    String txRef = PhysicalGiftRedemptionSupport.buildTransactionReference(idempotencyKey);

    return executeInTransaction(
        () ->
            performRedemption(
                storeUid,
                storeName,
                customerId,
                clientCode,
                aggregatedQuantities,
                idempotencyKey != null,
                txRef));
  }

  @SuppressWarnings("PMD.ExcessiveParameterList")
  private PhysicalGiftRedemptionResult performRedemption(
      UUID storeUid,
      String storeName,
      String customerId,
      String clientCode,
      Map<UUID, Integer> aggregatedQuantities,
      boolean checkIdempotency,
      String txRef) {
    PhysicalGiftRedemptionSupport.ResolvedRedemption resolved =
        PhysicalGiftRedemptionSupport.resolveRedeemedItems(
            storeUid, aggregatedQuantities, loyaltyGiftPersistencePort);
    List<RedeemedGiftItem> redeemedItems = resolved.items();

    long totalPointsLong =
        redeemedItems.stream()
            .mapToLong(RedeemedGiftItem::totalPointsCost)
            .reduce(0L, Math::addExact);
    BigDecimal totalCost = BigDecimal.valueOf(totalPointsLong).setScale(1, RoundingMode.HALF_UP);

    // Acquire row lock first so concurrent retries with the same idempotencyKey serialize safely.
    UserPointsAccount account = requireSufficientBalance(customerId, clientCode, totalCost);
    if (resolved.hasVfpExclusive()) {
      boolean isActiveVfp =
          userVfpPersistencePort != null
              && userVfpPersistencePort
                  .findByUserId(account.userId())
                  .map(UserVfpStatus::isVfp)
                  .orElse(false);
      if (!isActiveVfp) {
        throw new UserNotVfpException(
            "La remise d'un cadeau exclusif VFP nécessite un statut VFP actif pour ce client.");
      }
    }
    String resolvedClientCode =
        PhysicalGiftRedemptionSupport.resolveConsistentClientCode(account, clientCode);

    if (checkIdempotency && userPointsPersistencePort.existsTransactionByReference(txRef)) {
      throw new DuplicatePhysicalRedemptionException(
          "Cette remise de cadeaux a déjà été validée (référence : " + txRef + ").");
    }

    Instant now = clock.instant();
    BigDecimal newBalance = account.balance().subtract(totalCost).setScale(1, RoundingMode.HALF_UP);

    UserPointsAccount updatedAccount =
        new UserPointsAccount(
            account.id(),
            account.userId(),
            resolvedClientCode,
            newBalance,
            account.createdAt(),
            now);
    UserPointsAccount savedAccount = userPointsPersistencePort.saveAccount(updatedAccount);

    String description = PhysicalGiftRedemptionSupport.buildDescription(redeemedItems, storeName);
    PointsTransaction transaction =
        new PointsTransaction(
            null,
            null,
            txRef,
            savedAccount.userId(),
            storeUid,
            storeName,
            PointsTransactionType.REDEEM,
            totalCost,
            newBalance,
            SOURCE_TYPE_PHYSICAL_GIFT,
            description,
            now);
    userPointsPersistencePort.saveTransaction(savedAccount.id(), transaction);

    if (log.isInfoEnabled()) {
      log.info(
          "Physical gift redemption completed: txRef={}, storeUid={}, userId={}, pointsDeducted={}, remainingBalance={}",
          txRef,
          storeUid,
          savedAccount.userId(),
          totalCost,
          newBalance);
    }

    return new PhysicalGiftRedemptionResult(
        txRef,
        storeUid,
        storeName,
        savedAccount.userId(),
        savedAccount.clientCode(),
        totalCost,
        newBalance,
        now,
        List.copyOf(redeemedItems));
  }

  private UserPointsAccount requireSufficientBalance(
      String customerId, String clientCode, BigDecimal totalCost) {
    UserPointsAccount account =
        findAccountForUpdate(customerId, clientCode)
            .orElseThrow(
                () ->
                    new InsufficientLoyaltyPointsException(
                        String.format(
                            Locale.ROOT,
                            "Solde de points insuffisant : disponible 0.0 pts, requis %.1f pts.",
                            totalCost)));

    if (account.balance().compareTo(totalCost) < 0) {
      throw new InsufficientLoyaltyPointsException(
          String.format(
              Locale.ROOT,
              "Solde de points insuffisant : disponible %.1f pts, requis %.1f pts.",
              account.balance(),
              totalCost));
    }
    return account;
  }

  private Optional<UserPointsAccount> findAccountForUpdate(String customerId, String clientCode) {
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

  private void validateStoreOwnership(UUID storeUid, UUID callerUserId, String callerRole) {
    if (ROLE_ADMIN.equalsIgnoreCase(callerRole)) {
      return;
    }
    if (callerUserId == null) {
      throw new StoreAccessDeniedException(
          "Missing user identifier for redeeming gifts in store " + storeUid);
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

  private String resolveStoreName(UUID storeUid) {
    return storeVerificationPort
        .findStoreName(storeUid)
        .filter(name -> !name.isBlank())
        .orElse("Magasin #" + storeUid);
  }

  private <T> T executeInTransaction(java.util.function.Supplier<T> action) {
    if (transactionTemplate != null) {
      return transactionTemplate.execute(status -> action.get());
    }
    return action.get();
  }
}
