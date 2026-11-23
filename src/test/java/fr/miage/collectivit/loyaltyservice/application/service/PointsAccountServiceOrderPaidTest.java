package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.OrderPaidKafkaEvent;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.StoreOrderBreakdown;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
@DisplayName("PointsAccountService Order Paid Loyalty Tests (SLE-221, SLE-222)")
class PointsAccountServiceOrderPaidTest {

  private static final UUID STORE_UID_10 = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
  private static final UUID STORE_UID_20 = UUID.fromString("550e8400-e29b-41d4-a716-446655440020");

  @Mock private MerchantLoyaltyRatePersistencePort persistencePort;
  @Mock private StoreVerificationPort storeVerificationPort;
  @Mock private UserPointsPersistencePort userPointsPersistencePort;

  @InjectMocks private PointsAccountService service;

  @Test
  @DisplayName(
      "Should credit points calculated per store loyalty rates for multi-store click & collect order")
  void shouldCreditPointsCalculatedPerStoreLoyaltyRates() {
    StoreOrderBreakdown breakdown1 =
        new StoreOrderBreakdown(STORE_UID_10, "Boulangerie Le Moulin", new BigDecimal("20.00"));
    StoreOrderBreakdown breakdown2 =
        new StoreOrderBreakdown(STORE_UID_20, "Librairie Centrale", new BigDecimal("15.00"));

    OrderPaidKafkaEvent event =
        new OrderPaidKafkaEvent(
            "CMD-CC-20261118-001",
            "user-123",
            "k7P9xQ2",
            new BigDecimal("35.00"),
            "EUR",
            List.of(breakdown1, breakdown2),
            Instant.now());

    when(userPointsPersistencePort.existsTransactionByReference("CMD-CC-20261118-001"))
        .thenReturn(false);

    // Store 10 has rate 2.00 EUR = 10.0 PTS => 20 EUR = 100 PTS
    MerchantLoyaltyRate rateStore10 =
        new MerchantLoyaltyRate(STORE_UID_10, new BigDecimal("2.00"), new BigDecimal("10.0"));
    when(persistencePort.findByStoreUid(STORE_UID_10)).thenReturn(Optional.of(rateStore10));

    // Store 20 has no rate configured => default 1.00 EUR = 1.0 PTS => 15 EUR = 15 PTS
    when(persistencePort.findByStoreUid(STORE_UID_20)).thenReturn(Optional.empty());

    UserPointsAccount existingAccount =
        new UserPointsAccount(
            1L, "user-123", "k7P9xQ2", new BigDecimal("50.0"), Instant.now(), Instant.now());
    when(userPointsPersistencePort.findAccountByUserIdForUpdate("user-123"))
        .thenReturn(Optional.of(existingAccount));
    when(userPointsPersistencePort.saveAccount(any(UserPointsAccount.class)))
        .thenAnswer(i -> i.getArgument(0));

    service.processOrderPaid(event);

    // Expected total points: 100.0 + 15.0 = 115.0 PTS
    // New balance: 50.0 + 115.0 = 165.0 PTS
    ArgumentCaptor<UserPointsAccount> accountCaptor =
        ArgumentCaptor.forClass(UserPointsAccount.class);
    verify(userPointsPersistencePort).saveAccount(accountCaptor.capture());
    UserPointsAccount savedAccount = accountCaptor.getValue();
    assertThat(savedAccount.balance()).isEqualByComparingTo(new BigDecimal("165.0"));

    ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
    verify(userPointsPersistencePort).saveTransaction(eq(1L), txCaptor.capture());
    PointsTransaction savedTx = txCaptor.getValue();
    assertThat(savedTx.transactionReference()).isEqualTo("CMD-CC-20261118-001");
    assertThat(savedTx.userId()).isEqualTo("user-123");
    assertThat(savedTx.type()).isEqualTo(PointsTransactionType.EARN);
    assertThat(savedTx.points()).isEqualByComparingTo(new BigDecimal("115.0"));
    assertThat(savedTx.balanceAfter()).isEqualByComparingTo(new BigDecimal("165.0"));
    assertThat(savedTx.sourceType()).isEqualTo("ORDER_CLICK_AND_COLLECT");
  }

  @Test
  @DisplayName("Should create account if user does not exist and credit earned points")
  void shouldCreateAccountIfUserDoesNotExist() {
    StoreOrderBreakdown breakdown =
        new StoreOrderBreakdown(STORE_UID_10, "Boulangerie", new BigDecimal("30.00"));
    OrderPaidKafkaEvent event =
        new OrderPaidKafkaEvent(
            "CMD-CC-20261118-NEW",
            "user-new",
            "NEW123",
            new BigDecimal("30.00"),
            "EUR",
            List.of(breakdown),
            Instant.now());

    when(userPointsPersistencePort.existsTransactionByReference("CMD-CC-20261118-NEW"))
        .thenReturn(false);
    when(persistencePort.findByStoreUid(STORE_UID_10))
        .thenReturn(Optional.empty()); // default 1 EUR = 1 PT => 30 PTS
    when(userPointsPersistencePort.findAccountByUserIdForUpdate("user-new"))
        .thenReturn(Optional.empty());
    when(userPointsPersistencePort.findAccountByClientCodeForUpdate("NEW123"))
        .thenReturn(Optional.empty());

    when(userPointsPersistencePort.saveAccount(any(UserPointsAccount.class)))
        .thenAnswer(
            i -> {
              UserPointsAccount a = i.getArgument(0);
              return new UserPointsAccount(
                  99L, a.userId(), a.clientCode(), a.balance(), a.createdAt(), a.updatedAt());
            });

    service.processOrderPaid(event);

    ArgumentCaptor<UserPointsAccount> accountCaptor =
        ArgumentCaptor.forClass(UserPointsAccount.class);
    verify(userPointsPersistencePort).saveAccount(accountCaptor.capture());
    UserPointsAccount created = accountCaptor.getValue();
    assertThat(created.userId()).isEqualTo("user-new");
    assertThat(created.clientCode()).isEqualTo("NEW123");
    assertThat(created.balance()).isEqualByComparingTo(new BigDecimal("30.0"));

    ArgumentCaptor<PointsTransaction> txCaptor = ArgumentCaptor.forClass(PointsTransaction.class);
    verify(userPointsPersistencePort).saveTransaction(eq(99L), txCaptor.capture());
    PointsTransaction savedTx = txCaptor.getValue();
    assertThat(savedTx.points()).isEqualByComparingTo(new BigDecimal("30.0"));
    assertThat(savedTx.balanceAfter()).isEqualByComparingTo(new BigDecimal("30.0"));
  }

  @Test
  @DisplayName("Should be idempotent when order reference was already processed")
  void shouldBeIdempotentOnOrderReference() {
    OrderPaidKafkaEvent event =
        new OrderPaidKafkaEvent(
            "CMD-CC-DUPLICATE",
            "user-123",
            "k7P9xQ2",
            new BigDecimal("50.00"),
            "EUR",
            List.of(),
            Instant.now());

    when(userPointsPersistencePort.existsTransactionByReference("CMD-CC-DUPLICATE"))
        .thenReturn(true);

    service.processOrderPaid(event);

    verify(userPointsPersistencePort, never()).saveAccount(any());
    verify(userPointsPersistencePort, never()).saveTransaction(any(), any());
  }

  @Test
  @DisplayName("Should ignore null event or blank order reference")
  void shouldIgnoreNullEventOrBlankOrderReference() {
    service.processOrderPaid(null);

    OrderPaidKafkaEvent blankEvent =
        new OrderPaidKafkaEvent(
            "   ", "user-123", "k7P9xQ2", new BigDecimal("10.00"), "EUR", List.of(), Instant.now());
    service.processOrderPaid(blankEvent);

    verify(userPointsPersistencePort, never()).existsTransactionByReference(any());
    verify(userPointsPersistencePort, never()).saveAccount(any());
  }
}
