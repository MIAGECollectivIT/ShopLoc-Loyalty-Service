package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.exception.MerchantLoyaltyRateNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreAccessDeniedException;
import fr.miage.collectivit.loyaltyservice.domain.exception.StoreNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.PhysicalPaymentCompletedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Application Service - PointsAccountService")
class PointsAccountServiceTest {

  private static final UUID STORE_UID_10 = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
  private static final UUID STORE_UID_20 = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");
  private static final UUID STORE_UID_99 = UUID.fromString("550e8400-e29b-41d4-a716-446655440099");
  private static final UUID UNKNOWN_STORE_UID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440999");
  private static final UUID OWNER_UID = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");
  private static final UUID OTHER_OWNER_UID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440098");

  @Mock private MerchantLoyaltyRatePersistencePort persistencePort;
  @Mock private StoreVerificationPort storeVerificationPort;
  @Mock private UserPointsPersistencePort userPointsPersistencePort;

  @InjectMocks private PointsAccountService service;

  @Test
  @DisplayName("Should create new rate in ADMIN mode without Store Service verification")
  void shouldCreateNewRateAsAdmin() {
    when(persistencePort.findByStoreUid(STORE_UID_10)).thenReturn(Optional.empty());
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result =
        service.configureRate(
            STORE_UID_10, new BigDecimal("2.00"), new BigDecimal("5.0"), OTHER_OWNER_UID, "ADMIN");

    assertThat(result.getStoreUid()).isEqualTo(STORE_UID_10);
    assertThat(result.getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.00"));
    assertThat(result.getPointsReward()).isEqualByComparingTo(new BigDecimal("5.0"));
    verify(persistencePort).save(any(MerchantLoyaltyRate.class));
    verify(storeVerificationPort, never()).findStoreOwnerUid(any());
  }

  @Test
  @DisplayName("Should allow store owner merchant to configure and update rate")
  void shouldAllowStoreOwnerMerchantToConfigureRate() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID_10)).thenReturn(Optional.of(OWNER_UID));

    MerchantLoyaltyRate existing =
        new MerchantLoyaltyRate(STORE_UID_10, new BigDecimal("1.00"), new BigDecimal("1.0"));
    when(persistencePort.findByStoreUid(STORE_UID_10)).thenReturn(Optional.of(existing));
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result =
        service.configureRate(
            STORE_UID_10, new BigDecimal("3.00"), new BigDecimal("4.5"), OWNER_UID, "MERCHANT");

    assertThat(result.getStoreUid()).isEqualTo(STORE_UID_10);
    assertThat(result.getEuroAmount()).isEqualByComparingTo(new BigDecimal("3.00"));
    assertThat(result.getPointsReward()).isEqualByComparingTo(new BigDecimal("4.5"));
    verify(persistencePort).save(existing);
  }

  @Test
  @DisplayName("Should reject modification when store does not exist (StoreNotFoundException)")
  void shouldThrowWhenStoreDoesNotExist() {
    when(storeVerificationPort.findStoreOwnerUid(UNKNOWN_STORE_UID)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.configureRate(
                    UNKNOWN_STORE_UID,
                    new BigDecimal("2.00"),
                    new BigDecimal("2.0"),
                    OWNER_UID,
                    "MERCHANT"))
        .isInstanceOf(StoreNotFoundException.class)
        .hasMessageContaining(UNKNOWN_STORE_UID.toString());

    verify(persistencePort, never()).save(any());
  }

  @Test
  @DisplayName(
      "Should reject modification when merchant is not store owner (StoreAccessDeniedException)")
  void shouldThrowWhenMerchantIsNotStoreOwner() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID_10))
        .thenReturn(Optional.of(OTHER_OWNER_UID));

    assertThatThrownBy(
            () ->
                service.configureRate(
                    STORE_UID_10,
                    new BigDecimal("2.00"),
                    new BigDecimal("2.0"),
                    OWNER_UID,
                    "MERCHANT"))
        .isInstanceOf(StoreAccessDeniedException.class)
        .hasMessageContaining("owner");

    verify(persistencePort, never()).save(any());
  }

  @Test
  @DisplayName("Should reject modification when merchant user ID is null")
  void shouldThrowWhenMerchantUserIdIsNull() {
    assertThatThrownBy(
            () ->
                service.configureRate(
                    STORE_UID_10,
                    new BigDecimal("2.00"),
                    new BigDecimal("2.0"),
                    (UUID) null,
                    "MERCHANT"))
        .isInstanceOf(StoreAccessDeniedException.class)
        .hasMessageContaining("Missing");

    verify(persistencePort, never()).save(any());
  }

  @Test
  @DisplayName("Should retrieve existing rate by storeUid")
  void shouldReturnExistingRate() {
    MerchantLoyaltyRate existing =
        new MerchantLoyaltyRate(STORE_UID_20, new BigDecimal("1.00"), new BigDecimal("2.0"));
    when(persistencePort.findByStoreUid(STORE_UID_20)).thenReturn(Optional.of(existing));

    MerchantLoyaltyRate result = service.getRateByStoreUid(STORE_UID_20);

    assertThat(result).isNotNull();
    assertThat(result.getStoreUid()).isEqualTo(STORE_UID_20);
  }

  @Test
  @DisplayName("Should throw exception when rate is not found for storeUid")
  void shouldThrowWhenRateNotFound() {
    when(persistencePort.findByStoreUid(UNKNOWN_STORE_UID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getRateByStoreUid(UNKNOWN_STORE_UID))
        .isInstanceOf(MerchantLoyaltyRateNotFoundException.class)
        .hasMessageContaining(UNKNOWN_STORE_UID.toString());
  }

  @Test
  @DisplayName(
      "Should recover and retry update when concurrent creation causes DataIntegrityViolationException")
  void shouldRecoverAndRetryWhenConcurrentCreationCausesDataIntegrityViolation() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID_10)).thenReturn(Optional.of(OWNER_UID));

    MerchantLoyaltyRate concurrentRate =
        new MerchantLoyaltyRate(STORE_UID_10, new BigDecimal("1.00"), new BigDecimal("1.0"));

    // 1st find: returns empty. 2nd find (after conflict): returns the concurrently created rate.
    when(persistencePort.findByStoreUid(STORE_UID_10))
        .thenReturn(Optional.empty())
        .thenReturn(Optional.of(concurrentRate));

    // 1st save: throws DataIntegrityViolationException. 2nd save (recovery): succeeds.
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result =
        service.configureRate(
            STORE_UID_10, new BigDecimal("2.50"), new BigDecimal("5.0"), OWNER_UID, "MERCHANT");

    assertThat(result.getEuroAmount()).isEqualByComparingTo(new BigDecimal("2.50"));
    assertThat(result.getPointsReward()).isEqualByComparingTo(new BigDecimal("5.0"));
  }

  @Test
  @DisplayName("Should propagate DataIntegrityViolationException when concurrency recovery fails")
  void shouldPropagateDataIntegrityViolationWhenRecoveryFails() {
    when(storeVerificationPort.findStoreOwnerUid(STORE_UID_10)).thenReturn(Optional.of(OWNER_UID));
    when(persistencePort.findByStoreUid(STORE_UID_10)).thenReturn(Optional.empty());
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenThrow(new org.springframework.dao.DataIntegrityViolationException("duplicate key"));

    assertThatThrownBy(
            () ->
                service.configureRate(
                    STORE_UID_10,
                    new BigDecimal("2.50"),
                    new BigDecimal("5.0"),
                    OWNER_UID,
                    "MERCHANT"))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
  }

  @Test
  @DisplayName(
      "Should execute persistence inside TransactionTemplate when TransactionTemplate is provided")
  void shouldExecutePersistenceInsideTransactionTemplate() {
    org.springframework.transaction.support.TransactionTemplate mockTemplate =
        org.mockito.Mockito.mock(org.springframework.transaction.support.TransactionTemplate.class);
    when(mockTemplate.execute(any()))
        .thenAnswer(
            invocation -> {
              org.springframework.transaction.support.TransactionCallback<?> callback =
                  invocation.getArgument(0);
              return callback.doInTransaction(null);
            });

    PointsAccountService serviceWithTx =
        new PointsAccountService(
            persistencePort, storeVerificationPort, userPointsPersistencePort, mockTemplate);

    when(storeVerificationPort.findStoreOwnerUid(STORE_UID_10)).thenReturn(Optional.of(OWNER_UID));
    when(persistencePort.findByStoreUid(STORE_UID_10)).thenReturn(Optional.empty());
    when(persistencePort.save(any(MerchantLoyaltyRate.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    MerchantLoyaltyRate result =
        serviceWithTx.configureRate(
            STORE_UID_10, new BigDecimal("3.00"), new BigDecimal("6.0"), OWNER_UID, "MERCHANT");

    assertThat(result).isNotNull();
    verify(mockTemplate).execute(any());
  }

  @Test
  @DisplayName(
      "Should skip processing when transaction reference has already been processed (Idempotence - SLE-173)")
  void shouldSkipProcessingWhenTransactionAlreadyExists() {
    PhysicalPaymentCompletedEvent event =
        new PhysicalPaymentCompletedEvent(
            "TX-ALREADY-EXISTS",
            STORE_UID_10,
            "k7P9xQ2",
            "user-123",
            new BigDecimal("20.00"),
            "EUR",
            Instant.now());

    when(userPointsPersistencePort.existsTransactionByReference("TX-ALREADY-EXISTS"))
        .thenReturn(true);

    service.processPhysicalPayment(event);

    verify(persistencePort, never()).findByStoreUid(any());
    verify(userPointsPersistencePort, never()).saveAccount(any());
    verify(userPointsPersistencePort, never()).saveTransaction(any(), any());
  }

  @Test
  @DisplayName(
      "Should calculate points with merchant rate and credit existing account (SLE-173, SLE-174)")
  void shouldCalculatePointsWithMerchantRateAndCreditExistingAccount() {
    Instant now = Instant.parse("2026-11-15T12:00:00Z");
    PhysicalPaymentCompletedEvent event =
        new PhysicalPaymentCompletedEvent(
            "TX-PHY-001", STORE_UID_10, "k7P9xQ2", "user-123", new BigDecimal("20.00"), "EUR", now);

    when(userPointsPersistencePort.existsTransactionByReference("TX-PHY-001")).thenReturn(false);
    when(persistencePort.findByStoreUid(STORE_UID_10))
        .thenReturn(
            Optional.of(
                new MerchantLoyaltyRate(
                    STORE_UID_10, new BigDecimal("2.00"), new BigDecimal("5.0"))));

    UserPointsAccount existingAccount =
        new UserPointsAccount(
            1L,
            "user-123",
            "k7P9xQ2",
            new BigDecimal("10.0"),
            now.minusSeconds(3600),
            now.minusSeconds(3600));
    when(userPointsPersistencePort.findAccountByUserIdForUpdate("user-123"))
        .thenReturn(Optional.of(existingAccount));
    when(userPointsPersistencePort.saveAccount(any(UserPointsAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.processPhysicalPayment(event);

    ArgumentCaptor<UserPointsAccount> accountCaptor =
        ArgumentCaptor.forClass(UserPointsAccount.class);
    verify(userPointsPersistencePort).saveAccount(accountCaptor.capture());
    UserPointsAccount savedAccount = accountCaptor.getValue();
    assertThat(savedAccount.balance()).isEqualByComparingTo("60.0");

    ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
    verify(userPointsPersistencePort)
        .saveTransaction(org.mockito.ArgumentMatchers.eq(1L), txCaptor.capture());
    PointsTransaction savedTx = txCaptor.getValue();
    assertThat(savedTx.transactionReference()).isEqualTo("TX-PHY-001");
    assertThat(savedTx.userId()).isEqualTo("user-123");
    assertThat(savedTx.type()).isEqualTo(PointsTransactionType.EARN);
    assertThat(savedTx.points()).isEqualByComparingTo("50.0");
    assertThat(savedTx.balanceAfter()).isEqualByComparingTo("60.0");
    assertThat(savedTx.storeUid()).isEqualTo(STORE_UID_10);
    assertThat(savedTx.sourceType()).isEqualTo("PHYSICAL_PAYMENT");
  }

  @Test
  @DisplayName(
      "Should use store name from StoreVerificationPort when available in transaction record")
  void shouldUseStoreNameFromStoreVerificationPortWhenAvailable() {
    Instant now = Instant.parse("2026-11-15T12:00:00Z");
    PhysicalPaymentCompletedEvent event =
        new PhysicalPaymentCompletedEvent(
            "TX-PHY-STORE-NAME",
            STORE_UID_10,
            "k7P9xQ2",
            "user-123",
            new BigDecimal("20.00"),
            "EUR",
            now);

    when(userPointsPersistencePort.existsTransactionByReference("TX-PHY-STORE-NAME"))
        .thenReturn(false);
    when(persistencePort.findByStoreUid(STORE_UID_10))
        .thenReturn(
            Optional.of(
                new MerchantLoyaltyRate(
                    STORE_UID_10, new BigDecimal("2.00"), new BigDecimal("5.0"))));
    when(storeVerificationPort.findStoreName(STORE_UID_10))
        .thenReturn(Optional.of("Burger King Lille"));

    UserPointsAccount existingAccount =
        new UserPointsAccount(
            1L,
            "user-123",
            "k7P9xQ2",
            new BigDecimal("10.0"),
            now.minusSeconds(3600),
            now.minusSeconds(3600));
    when(userPointsPersistencePort.findAccountByUserIdForUpdate("user-123"))
        .thenReturn(Optional.of(existingAccount));
    when(userPointsPersistencePort.saveAccount(any(UserPointsAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.processPhysicalPayment(event);

    ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
    verify(userPointsPersistencePort)
        .saveTransaction(org.mockito.ArgumentMatchers.eq(1L), txCaptor.capture());
    PointsTransaction savedTx = txCaptor.getValue();
    assertThat(savedTx.storeName()).isEqualTo("Burger King Lille");
    assertThat(savedTx.description()).contains("Burger King Lille");
  }

  @Test
  @DisplayName(
      "Should use default rate (1 EUR = 1 PTS) and create new account when store has no rate configured")
  void shouldCalculatePointsWithDefaultRateWhenStoreHasNoRateAndCreateNewAccount() {
    Instant now = Instant.parse("2026-11-15T12:00:00Z");
    PhysicalPaymentCompletedEvent event =
        new PhysicalPaymentCompletedEvent(
            "TX-PHY-NEW-ACC",
            STORE_UID_99,
            "k7P9xQ2",
            "user-new",
            new BigDecimal("15.50"),
            "EUR",
            now);

    when(userPointsPersistencePort.existsTransactionByReference("TX-PHY-NEW-ACC"))
        .thenReturn(false);
    when(persistencePort.findByStoreUid(STORE_UID_99)).thenReturn(Optional.empty());
    when(userPointsPersistencePort.findAccountByUserIdForUpdate("user-new"))
        .thenReturn(Optional.empty());
    when(userPointsPersistencePort.saveAccount(any(UserPointsAccount.class)))
        .thenAnswer(
            invocation -> {
              UserPointsAccount arg = invocation.getArgument(0);
              return new UserPointsAccount(
                  42L,
                  arg.userId(),
                  arg.clientCode(),
                  arg.balance(),
                  arg.createdAt(),
                  arg.updatedAt());
            });

    service.processPhysicalPayment(event);

    ArgumentCaptor<UserPointsAccount> accountCaptor =
        ArgumentCaptor.forClass(UserPointsAccount.class);
    verify(userPointsPersistencePort).saveAccount(accountCaptor.capture());
    UserPointsAccount savedAccount = accountCaptor.getValue();
    assertThat(savedAccount.balance()).isEqualByComparingTo("15.5");

    ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
    verify(userPointsPersistencePort)
        .saveTransaction(org.mockito.ArgumentMatchers.eq(42L), txCaptor.capture());
    PointsTransaction savedTx = txCaptor.getValue();
    assertThat(savedTx.points()).isEqualByComparingTo("15.5");
    assertThat(savedTx.balanceAfter()).isEqualByComparingTo("15.5");
    assertThat(savedTx.sourceType()).isEqualTo("PHYSICAL_PAYMENT");
  }

  @Test
  @DisplayName("Should resolve account by clientCode when customerId is null or blank")
  void shouldResolveAccountByClientCodeWhenCustomerIdIsBlank() {
    Instant now = Instant.parse("2026-11-15T12:00:00Z");
    PhysicalPaymentCompletedEvent event =
        new PhysicalPaymentCompletedEvent(
            "TX-PHY-NO-UUID", STORE_UID_10, "CARD-777", "   ", new BigDecimal("10.00"), "EUR", now);

    when(userPointsPersistencePort.existsTransactionByReference("TX-PHY-NO-UUID"))
        .thenReturn(false);
    when(persistencePort.findByStoreUid(STORE_UID_10))
        .thenReturn(
            Optional.of(
                new MerchantLoyaltyRate(
                    STORE_UID_10, new BigDecimal("1.00"), new BigDecimal("1.0"))));

    UserPointsAccount existingAccount =
        new UserPointsAccount(
            5L,
            "user-registered-777",
            "CARD-777",
            new BigDecimal("5.0"),
            now.minusSeconds(3600),
            now.minusSeconds(3600));
    when(userPointsPersistencePort.findAccountByClientCodeForUpdate("CARD-777"))
        .thenReturn(Optional.of(existingAccount));
    when(userPointsPersistencePort.saveAccount(any(UserPointsAccount.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    service.processPhysicalPayment(event);

    ArgumentCaptor<UserPointsAccount> accountCaptor =
        ArgumentCaptor.forClass(UserPointsAccount.class);
    verify(userPointsPersistencePort).saveAccount(accountCaptor.capture());
    assertThat(accountCaptor.getValue().balance()).isEqualByComparingTo("15.0");
  }
}
