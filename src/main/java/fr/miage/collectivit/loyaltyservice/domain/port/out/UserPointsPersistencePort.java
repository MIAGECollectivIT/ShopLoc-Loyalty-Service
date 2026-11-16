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

  Optional<UserPointsAccount> findAccountByClientCode(String clientCode);

  /** Reads the account with a pessimistic write lock (must be called inside a transaction). */
  Optional<UserPointsAccount> findAccountByUserIdForUpdate(String userId);

  /** Reads the account with a pessimistic write lock (must be called inside a transaction). */
  Optional<UserPointsAccount> findAccountByClientCodeForUpdate(String clientCode);

  List<PointsTransaction> findTransactionsByUserId(String userId);

  boolean existsTransactionByReference(String transactionReference);

  UserPointsAccount saveAccount(UserPointsAccount account);

  PointsTransaction saveTransaction(Long accountId, PointsTransaction transaction);
}
