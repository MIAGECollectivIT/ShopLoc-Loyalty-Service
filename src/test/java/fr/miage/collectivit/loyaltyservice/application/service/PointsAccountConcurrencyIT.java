package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ProcessPhysicalPaymentLoyaltyUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.PhysicalPaymentCompletedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(
    properties =
        "spring.datasource.url=jdbc:h2:mem:concurrency_testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL")
@DisplayName("Concurrency - no lost update on points credit")
class PointsAccountConcurrencyIT {

  private static final int PARALLEL_PAYMENTS = 8;

  @Autowired private ProcessPhysicalPaymentLoyaltyUseCase useCase;
  @Autowired private UserPointsPersistencePort persistencePort;

  @Test
  @DisplayName("Concurrent payments for the same customer must all be credited")
  void concurrentCreditsMustNotLosePoints() throws Exception {
    String userId = "user-concurrency-1";
    Instant now = Instant.now();
    persistencePort.saveAccount(
        new UserPointsAccount(null, userId, "CARD-CONC-1", new BigDecimal("100.0"), now, now));

    ExecutorService pool = Executors.newFixedThreadPool(PARALLEL_PAYMENTS);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<?>> futures = new ArrayList<>();
    for (int i = 0; i < PARALLEL_PAYMENTS; i++) {
      String txRef = "TX-CONC-" + i;
      futures.add(
          pool.submit(
              () -> {
                start.await();
                // default rate 1 EUR = 1 PTS -> +10.0 each
                useCase.processPhysicalPayment(
                    new PhysicalPaymentCompletedEvent(
                        txRef,
                        999L,
                        "CARD-CONC-1",
                        userId,
                        new BigDecimal("10.00"),
                        "EUR",
                        Instant.now()));
                return null;
              }));
    }
    start.countDown();
    for (Future<?> f : futures) {
      f.get(30, TimeUnit.SECONDS);
    }
    pool.shutdown();

    UserPointsAccount account = persistencePort.findAccountByUserId(userId).orElseThrow();
    assertThat(account.balance()).isEqualByComparingTo("180.0");
    assertThat(persistencePort.findTransactionsByUserId(userId)).hasSize(PARALLEL_PAYMENTS);
  }
}
