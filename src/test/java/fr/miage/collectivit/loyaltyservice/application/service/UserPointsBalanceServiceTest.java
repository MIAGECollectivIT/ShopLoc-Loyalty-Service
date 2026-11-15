package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsBalance;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Application Service - UserPointsBalanceService Unit Tests (SLE-176)")
class UserPointsBalanceServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-11-15T10:00:00Z");

  @Mock private UserPointsPersistencePort persistencePort;

  private UserPointsBalanceService service;

  @BeforeEach
  void setUp() {
    service = new UserPointsBalanceService(persistencePort, Clock.fixed(FIXED_NOW, ZoneOffset.UTC));
  }

  @Test
  @DisplayName("Should return existing points account and ordered transactions")
  void shouldReturnExistingBalanceAndTransactions() {
    String userId = "550e8400-e29b-41d4-a716-446655440000";
    Instant updatedAt = Instant.parse("2026-11-15T09:30:00Z");

    UserPointsAccount account =
        new UserPointsAccount(
            1L,
            userId,
            "CL95001",
            new BigDecimal("142.5"),
            Instant.parse("2026-10-01T08:00:00Z"),
            updatedAt);

    PointsTransaction tx1 =
        new PointsTransaction(
            10L,
            "PTS-EARN-001",
            userId,
            5L,
            "Boulangerie du Centre",
            PointsTransactionType.EARN,
            new BigDecimal("12.5"),
            new BigDecimal("142.5"),
            "PURCHASE",
            "Achat en boutique",
            updatedAt);

    PointsTransaction tx2 =
        new PointsTransaction(
            9L,
            "PTS-REDEEM-001",
            userId,
            7L,
            "Primeur Local",
            PointsTransactionType.REDEEM,
            new BigDecimal("20.0"),
            new BigDecimal("130.0"),
            "CHECKOUT",
            "Remise fidélité appliquée",
            Instant.parse("2026-11-14T18:00:00Z"));

    when(persistencePort.findAccountByUserId(userId)).thenReturn(Optional.of(account));
    when(persistencePort.findTransactionsByUserId(userId)).thenReturn(List.of(tx1, tx2));

    UserPointsBalance result = service.getUserPointsBalance("  " + userId + "  ");

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.clientCode()).isEqualTo("CL95001");
    assertThat(result.balance()).isEqualByComparingTo("142.5");
    assertThat(result.updatedAt()).isEqualTo(updatedAt);
    assertThat(result.transactions()).containsExactly(tx1, tx2);

    verify(persistencePort).findAccountByUserId(userId);
    verify(persistencePort).findTransactionsByUserId(userId);
  }

  @Test
  @DisplayName("Should return zero balance and empty transactions when no account exists yet")
  void shouldReturnZeroBalanceWhenAccountDoesNotExist() {
    String userId = "new-user-uuid";
    when(persistencePort.findAccountByUserId(userId)).thenReturn(Optional.empty());
    when(persistencePort.findTransactionsByUserId(userId)).thenReturn(null);

    UserPointsBalance result = service.getUserPointsBalance(userId);

    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.clientCode()).isNull();
    assertThat(result.balance()).isEqualByComparingTo("0.0");
    assertThat(result.updatedAt()).isEqualTo(FIXED_NOW);
    assertThat(result.transactions()).isEmpty();
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when userId is null or blank")
  void shouldRejectBlankUserId() {
    assertThatThrownBy(() -> service.getUserPointsBalance(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User ID must not be blank");
    assertThatThrownBy(() -> service.getUserPointsBalance("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User ID must not be blank");

    verifyNoInteractions(persistencePort);
  }
}
