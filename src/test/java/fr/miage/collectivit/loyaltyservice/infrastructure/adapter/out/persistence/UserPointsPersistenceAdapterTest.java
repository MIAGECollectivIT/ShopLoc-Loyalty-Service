package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("Persistence Adapter - UserPointsPersistenceAdapter Integration Tests (SLE-176)")
class UserPointsPersistenceAdapterTest {

  @Autowired private UserPointsPersistenceAdapter persistenceAdapter;

  @Test
  @DisplayName("Should save, update, and retrieve loyalty points account and ordered transactions")
  void shouldPersistAndRetrieveAccountAndTransactions() {
    String userId = "user-uuid-points-001";
    Instant createdAt = Instant.parse("2026-11-10T10:00:00Z");
    Instant firstTxTime = Instant.parse("2026-11-11T12:00:00Z");
    Instant secondTxTime = Instant.parse("2026-11-15T09:00:00Z");

    UserPointsAccount savedAccount =
        persistenceAdapter.saveAccount(
            new UserPointsAccount(
                null, userId, "CL95001", new BigDecimal("50.0"), createdAt, createdAt));

    assertThat(savedAccount.id()).isNotNull();
    assertThat(savedAccount.balance()).isEqualByComparingTo("50.0");

    persistenceAdapter.saveTransaction(
        savedAccount.id(),
        new PointsTransaction(
            null,
            null,
            "PTS-REF-001",
            userId,
            UUID.fromString("550e8400-e29b-41d4-a716-446655440010"),
            "Boulangerie Artisanale",
            PointsTransactionType.EARN,
            new BigDecimal("60.0"),
            new BigDecimal("60.0"),
            "PURCHASE",
            "Achat pain et viennoiseries",
            firstTxTime));

    persistenceAdapter.saveTransaction(
        savedAccount.id(),
        new PointsTransaction(
            null,
            null,
            "PTS-REF-002",
            userId,
            UUID.fromString("550e8400-e29b-41d4-a716-446655440011"),
            "Fromagerie Locale",
            PointsTransactionType.REDEEM,
            new BigDecimal("10.0"),
            new BigDecimal("50.0"),
            "CHECKOUT",
            "Utilisation points au paiement",
            secondTxTime));

    UserPointsAccount updatedAccount =
        persistenceAdapter.saveAccount(
            new UserPointsAccount(
                savedAccount.id(),
                userId,
                "CL95001",
                new BigDecimal("75.5"),
                createdAt,
                secondTxTime));

    assertThat(updatedAccount.id()).isEqualTo(savedAccount.id());
    assertThat(updatedAccount.balance()).isEqualByComparingTo("75.5");

    Optional<UserPointsAccount> foundAccount = persistenceAdapter.findAccountByUserId(userId);
    assertThat(foundAccount).isPresent();
    assertThat(foundAccount.get().balance()).isEqualByComparingTo("75.5");
    assertThat(foundAccount.get().clientCode()).isEqualTo("CL95001");

    List<PointsTransaction> transactions = persistenceAdapter.findTransactionsByUserId(userId);
    assertThat(transactions).hasSize(2);
    assertThat(transactions.get(0).transactionReference()).isEqualTo("PTS-REF-002");
    assertThat(transactions.get(0).type()).isEqualTo(PointsTransactionType.REDEEM);
    assertThat(transactions.get(1).transactionReference()).isEqualTo("PTS-REF-001");
    assertThat(transactions.get(1).type()).isEqualTo(PointsTransactionType.EARN);
  }
}
