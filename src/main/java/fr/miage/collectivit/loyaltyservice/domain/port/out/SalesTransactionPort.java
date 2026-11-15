package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.UserTransaction;
import java.time.Instant;
import java.util.List;

/** Outbound hexagonal port for querying approved customer sales transactions from Sales Service. */
public interface SalesTransactionPort {

  /**
   * Retrieves all approved transactions for the given user within the specified interval.
   *
   * @param userId unique user identifier
   * @param from start of the time interval (inclusive)
   * @param to end of the time interval (inclusive)
   * @return list of approved transactions
   */
  List<UserTransaction> getApprovedTransactions(String userId, Instant from, Instant to);
}
