package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.model.UserTransaction;
import fr.miage.collectivit.loyaltyservice.domain.port.out.SalesTransactionPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Application Service - UserActivityEvaluationService Test (SLE-162)")
class UserActivityEvaluationServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-11-14T14:00:00Z");
  private static final Instant EXPECTED_WINDOW_START =
      FIXED_NOW.minus(Duration.ofDays(UserActivityEvaluation.SLIDING_WINDOW_DAYS));

  private SalesTransactionPort salesTransactionPort;
  private UserActivityEvaluationService service;

  @BeforeEach
  void setUp() {
    salesTransactionPort = mock(SalesTransactionPort.class);
    Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    service = new UserActivityEvaluationService(salesTransactionPort, fixedClock);
  }

  private UserTransaction createMockTransaction(String id, Instant createdAt) {
    return new UserTransaction(
        id,
        "REF-" + id,
        BigDecimal.valueOf(19.99),
        "EUR",
        "TRANSACTION_APPROVED",
        "AUTH-" + id,
        createdAt);
  }

  @Test
  @DisplayName("SLE-162: Less than 10 transactions (e.g., 7) -> isEligibleVfp should be false")
  void testBelowThresholdEligibility() {
    String userId = "USER-1";
    List<UserTransaction> transactions = new ArrayList<>();
    for (int i = 1; i <= 7; i++) {
      transactions.add(createMockTransaction("TXN-" + i, FIXED_NOW.minus(Duration.ofDays(i))));
    }

    when(salesTransactionPort.getApprovedTransactions(
            eq(userId), eq(EXPECTED_WINDOW_START), eq(FIXED_NOW)))
        .thenReturn(transactions);

    UserActivityEvaluation result = service.evaluateActivity(userId);

    assertThat(result.getUserId()).isEqualTo(userId);
    assertThat(result.getTransactionCount()).isEqualTo(7);
    assertThat(result.getThreshold()).isEqualTo(10);
    assertThat(result.isEligibleVfp()).isFalse();
    assertThat(result.getWindowStart()).isEqualTo(EXPECTED_WINDOW_START);
    assertThat(result.getWindowEnd()).isEqualTo(FIXED_NOW);
  }

  @Test
  @DisplayName("SLE-162: Exactly 10 transactions -> isEligibleVfp should be true")
  void testExactThresholdEligibility() {
    String userId = "USER-2";
    List<UserTransaction> transactions = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      transactions.add(createMockTransaction("TXN-" + i, FIXED_NOW.minus(Duration.ofDays(i))));
    }

    when(salesTransactionPort.getApprovedTransactions(
            eq(userId), eq(EXPECTED_WINDOW_START), eq(FIXED_NOW)))
        .thenReturn(transactions);

    UserActivityEvaluation result = service.evaluateActivity(userId);

    assertThat(result.getTransactionCount()).isEqualTo(10);
    assertThat(result.getThreshold()).isEqualTo(10);
    assertThat(result.isEligibleVfp()).isTrue();
  }

  @Test
  @DisplayName("SLE-162: Greater than 10 transactions (e.g., 14) -> isEligibleVfp should be true")
  void testAboveThresholdEligibility() {
    String userId = "USER-3";
    List<UserTransaction> transactions = new ArrayList<>();
    for (int i = 1; i <= 14; i++) {
      transactions.add(
          createMockTransaction("TXN-" + i, FIXED_NOW.minus(Duration.ofHours(i * 12))));
    }

    when(salesTransactionPort.getApprovedTransactions(
            eq(userId), eq(EXPECTED_WINDOW_START), eq(FIXED_NOW)))
        .thenReturn(transactions);

    UserActivityEvaluation result = service.evaluateActivity(userId);

    assertThat(result.getTransactionCount()).isEqualTo(14);
    assertThat(result.isEligibleVfp()).isTrue();
  }

  @Test
  @DisplayName("SLE-162: Multiple transactions on the same day -> all counted without daily cap")
  void testMultipleTransactionsSameDayWithoutDailyCap() {
    String userId = "USER-MULTI";
    List<UserTransaction> transactions = new ArrayList<>();
    // 5 transactions on the same day (today)
    for (int i = 1; i <= 5; i++) {
      transactions.add(
          createMockTransaction("TXN-SAME-DAY-" + i, FIXED_NOW.minus(Duration.ofMinutes(i * 30))));
    }
    // 6 transactions on another day
    for (int i = 6; i <= 11; i++) {
      transactions.add(
          createMockTransaction(
              "TXN-OTHER-DAY-" + i,
              FIXED_NOW.minus(Duration.ofDays(3)).minus(Duration.ofMinutes(i * 10))));
    }

    when(salesTransactionPort.getApprovedTransactions(
            eq(userId), eq(EXPECTED_WINDOW_START), eq(FIXED_NOW)))
        .thenReturn(transactions);

    UserActivityEvaluation result = service.evaluateActivity(userId);

    assertThat(result.getTransactionCount()).isEqualTo(11);
    assertThat(result.isEligibleVfp()).isTrue();
  }

  @Test
  @DisplayName("SLE-162: Zero transactions -> transactionCount = 0 and isEligibleVfp = false")
  void testZeroTransactions() {
    String userId = "USER-EMPTY";
    when(salesTransactionPort.getApprovedTransactions(
            eq(userId), eq(EXPECTED_WINDOW_START), eq(FIXED_NOW)))
        .thenReturn(Collections.emptyList());

    UserActivityEvaluation result = service.evaluateActivity(userId);

    assertThat(result.getTransactionCount()).isEqualTo(0);
    assertThat(result.isEligibleVfp()).isFalse();
  }

  @Test
  @DisplayName("SLE-162: Null or blank userId throws IllegalArgumentException")
  void testBlankUserIdThrows() {
    assertThatThrownBy(() -> service.evaluateActivity(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.evaluateActivity("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
