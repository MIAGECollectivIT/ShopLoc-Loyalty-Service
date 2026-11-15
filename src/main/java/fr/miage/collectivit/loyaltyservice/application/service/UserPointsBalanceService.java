package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsBalance;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserPointsBalanceQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserPointsBalanceService implements GetUserPointsBalanceQuery {

  private final UserPointsPersistencePort persistencePort;
  private final Clock clock;

  @Autowired
  public UserPointsBalanceService(UserPointsPersistencePort persistencePort) {
    this(persistencePort, Clock.systemUTC());
  }

  public UserPointsBalanceService(UserPointsPersistencePort persistencePort, Clock clock) {
    this.persistencePort = persistencePort;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public UserPointsBalance getUserPointsBalance(String userId) {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("User ID must not be blank");
    }

    String cleanUserId = userId.trim();
    UserPointsAccount account =
        persistencePort
            .findAccountByUserId(cleanUserId)
            .orElseGet(() -> UserPointsAccount.empty(cleanUserId, clock.instant()));

    List<PointsTransaction> transactions = persistencePort.findTransactionsByUserId(cleanUserId);
    List<PointsTransaction> safeTransactions =
        transactions != null ? transactions : Collections.emptyList();

    return new UserPointsBalance(
        cleanUserId,
        account.clientCode(),
        account.balance(),
        account.updatedAt(),
        safeTransactions);
  }
}
