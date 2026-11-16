package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.model.UserTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpProgress;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserVfpStatusQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.out.SalesTransactionPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Application Service - UserVfpProgressService Test (SLE-51 / SLE-238)")
class UserVfpProgressServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-11-16T12:00:00Z");

  private SalesTransactionPort salesTransactionPort;
  private GetUserVfpStatusQuery getUserVfpStatusQuery;
  private EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase;
  private UserVfpProgressService service;

  @BeforeEach
  void setUp() {
    salesTransactionPort = mock(SalesTransactionPort.class);
    getUserVfpStatusQuery = mock(GetUserVfpStatusQuery.class);
    evaluateAndApplyVfpStatusUseCase = mock(EvaluateAndApplyVfpStatusUseCase.class);
    Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    service =
        new UserVfpProgressService(
            salesTransactionPort,
            getUserVfpStatusQuery,
            evaluateAndApplyVfpStatusUseCase,
            fixedClock);
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when userId is blank or null")
  void shouldThrowWhenUserIdIsBlank() {
    assertThatThrownBy(() -> service.getProgress(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.getProgress("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should calculate non-VFP progression correctly (4 transactions, 6 remaining)")
  void shouldCalculateNonVfpProgression() {
    String userId = "USER-NON-VFP";
    List<UserTransaction> txList = new ArrayList<>();
    for (int i = 1; i <= 4; i++) {
      txList.add(createTx("tx-" + i, FIXED_NOW.minus(Duration.ofDays(i))));
    }

    when(salesTransactionPort.getApprovedTransactions(eq(userId), any(), any())).thenReturn(txList);
    when(getUserVfpStatusQuery.getStatus(userId))
        .thenReturn(Optional.of(UserVfpStatus.initial(userId, FIXED_NOW)));

    VfpProgress progress = service.getProgress(userId);

    assertThat(progress.isVfp()).isFalse();
    assertThat(progress.getCurrentTransactionCount()).isEqualTo(4);
    assertThat(progress.getThreshold()).isEqualTo(10);
    assertThat(progress.getRemainingTransactionsToQualify()).isEqualTo(6);
    assertThat(progress.getRemainingTransactionsToMaintain()).isEqualTo(0);
    assertThat(progress.getDaysRemainingBeforeRevocation()).isNull();
    assertThat(progress.getEstimatedRevocationDate()).isNull();
  }

  @Test
  @DisplayName(
      "Should calculate active VFP revocation forecast correctly when exactly at threshold (10 tx)")
  void shouldCalculateActiveVfpWhenExactlyAtThreshold() {
    String userId = "USER-VFP-10";
    List<UserTransaction> txList = new ArrayList<>();

    // Oldest tx placed at J-13 (expires in 2 days from FIXED_NOW)
    txList.add(createTx("tx-oldest", FIXED_NOW.minus(Duration.ofDays(13))));
    for (int i = 2; i <= 10; i++) {
      txList.add(createTx("tx-" + i, FIXED_NOW.minus(Duration.ofDays(13 - i))));
    }

    UserVfpStatus vfpStatus =
        new UserVfpStatus(userId, true, FIXED_NOW.minus(Duration.ofDays(10)), null, FIXED_NOW);
    when(salesTransactionPort.getApprovedTransactions(eq(userId), any(), any())).thenReturn(txList);
    when(getUserVfpStatusQuery.getStatus(userId)).thenReturn(Optional.of(vfpStatus));

    VfpProgress progress = service.getProgress(userId);

    assertThat(progress.isVfp()).isTrue();
    assertThat(progress.getCurrentTransactionCount()).isEqualTo(10);
    assertThat(progress.getRemainingTransactionsToQualify()).isEqualTo(0);
    assertThat(progress.getRemainingTransactionsToMaintain()).isEqualTo(1);
    assertThat(progress.getDaysRemainingBeforeRevocation()).isEqualTo(2L);
    assertThat(progress.getEstimatedRevocationDate())
        .isEqualTo(txList.get(0).createdAt().plus(Duration.ofDays(15)));
    assertThat(progress.getExpiringTransactionsCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should calculate active VFP revocation forecast with cushion (12 tx)")
  void shouldCalculateActiveVfpWithCushion() {
    String userId = "USER-VFP-12";
    List<UserTransaction> txList = new ArrayList<>();

    // tx 0 & 1 at J-14 (expire in 1 day)
    txList.add(createTx("tx-0", FIXED_NOW.minus(Duration.ofDays(14))));
    txList.add(createTx("tx-1", FIXED_NOW.minus(Duration.ofDays(14))));
    // tx 2 (critical index: 12 - 10 = 2) at J-11 (expires in 4 days)
    txList.add(createTx("tx-2", FIXED_NOW.minus(Duration.ofDays(11))));

    for (int i = 3; i < 12; i++) {
      txList.add(createTx("tx-" + i, FIXED_NOW.minus(Duration.ofDays(12 - i))));
    }

    UserVfpStatus vfpStatus =
        new UserVfpStatus(userId, true, FIXED_NOW.minus(Duration.ofDays(20)), null, FIXED_NOW);
    when(salesTransactionPort.getApprovedTransactions(eq(userId), any(), any())).thenReturn(txList);
    when(getUserVfpStatusQuery.getStatus(userId)).thenReturn(Optional.of(vfpStatus));

    VfpProgress progress = service.getProgress(userId);

    assertThat(progress.isVfp()).isTrue();
    assertThat(progress.getCurrentTransactionCount()).isEqualTo(12);
    assertThat(progress.getRemainingTransactionsToQualify()).isEqualTo(0);
    assertThat(progress.getDaysRemainingBeforeRevocation()).isEqualTo(4L);
    assertThat(progress.getExpiringTransactionsCount()).isEqualTo(3);
    assertThat(progress.getRemainingTransactionsToMaintain()).isEqualTo(1);
  }

  private UserTransaction createTx(String id, Instant createdAt) {
    return new UserTransaction(
        id,
        "REF-" + id,
        new BigDecimal("15.50"),
        "EUR",
        "TRANSACTION_APPROVED",
        "AUTH-" + id,
        createdAt);
  }
}
