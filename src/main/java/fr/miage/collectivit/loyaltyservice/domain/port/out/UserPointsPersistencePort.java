package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import java.util.List;
import java.util.Optional;

/**
 * Secondary outbound persistence port for loyalty points accounts and transactions (SLE-38 /
 * SLE-176).
 */
public interface UserPointsPersistencePort {

  Optional<UserPointsAccount> findAccountByUserId(String userId);

  List<PointsTransaction> findTransactionsByUserId(String userId);

  UserPointsAccount saveAccount(UserPointsAccount account);

  PointsTransaction saveTransaction(Long accountId, PointsTransaction transaction);
}
