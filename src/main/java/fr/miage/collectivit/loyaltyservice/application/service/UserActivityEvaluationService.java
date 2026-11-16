package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.model.UserTransaction;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateUserActivityUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.SalesTransactionPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Domain calculation engine evaluating user purchasing activity over a dynamic 15-day sliding
 * window without daily transaction caps to determine VFP status qualification (SLE-44 / SLE-162).
 */
@Slf4j
@Service
public class UserActivityEvaluationService implements EvaluateUserActivityUseCase {

  private final SalesTransactionPort salesTransactionPort;
  private final Clock clock;

  @Autowired
  public UserActivityEvaluationService(SalesTransactionPort salesTransactionPort) {
    this(salesTransactionPort, Clock.systemUTC());
  }

  public UserActivityEvaluationService(SalesTransactionPort salesTransactionPort, Clock clock) {
    this.salesTransactionPort = salesTransactionPort;
    this.clock = clock;
  }

  @Override
  public UserActivityEvaluation evaluateActivity(String userId) {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("User ID must not be blank");
    }

    Instant windowEnd = clock.instant();
    Instant windowStart =
        windowEnd.minus(Duration.ofDays(UserActivityEvaluation.SLIDING_WINDOW_DAYS));

    if (log.isDebugEnabled()) {
      log.debug(
          "Evaluating activity for user {} on sliding window [{} -> {}]",
          userId,
          windowStart,
          windowEnd);
    }

    List<UserTransaction> transactions =
        salesTransactionPort.getApprovedTransactions(userId.trim(), windowStart, windowEnd);

    int count = transactions != null ? transactions.size() : 0;
    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(userId.trim(), count, windowStart, windowEnd);

    if (log.isInfoEnabled()) {
      log.info(
          "Activity evaluation calculated: userId={}, transactionCount={}, threshold={}, isEligibleVfp={}",
          evaluation.getUserId(),
          evaluation.getTransactionCount(),
          evaluation.getThreshold(),
          evaluation.isEligibleVfp());
    }

    return evaluation;
  }
}
