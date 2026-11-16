package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.model.UserTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpProgress;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserVfpProgressQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserVfpStatusQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.out.SalesTransactionPort;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Application service calculating a customer's VFP qualification progress, maintenance targets, and
 * countdown to status revocation over the 15-day sliding window (SLE-51 / SLE-238).
 */
@Slf4j
@Service
public class UserVfpProgressService implements GetUserVfpProgressQuery {

  private final SalesTransactionPort salesTransactionPort;
  private final GetUserVfpStatusQuery getUserVfpStatusQuery;
  private final EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase;
  private final Clock clock;

  @Autowired
  public UserVfpProgressService(
      SalesTransactionPort salesTransactionPort,
      GetUserVfpStatusQuery getUserVfpStatusQuery,
      EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase) {
    this(
        salesTransactionPort,
        getUserVfpStatusQuery,
        evaluateAndApplyVfpStatusUseCase,
        Clock.systemUTC());
  }

  public UserVfpProgressService(
      SalesTransactionPort salesTransactionPort,
      GetUserVfpStatusQuery getUserVfpStatusQuery,
      EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase,
      Clock clock) {
    this.salesTransactionPort = salesTransactionPort;
    this.getUserVfpStatusQuery = getUserVfpStatusQuery;
    this.evaluateAndApplyVfpStatusUseCase = evaluateAndApplyVfpStatusUseCase;
    this.clock = clock;
  }

  @Override
  public VfpProgress getProgress(String userId) {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("User ID must not be blank");
    }

    String cleanUserId = userId.trim();
    Instant now = clock.instant();
    Instant windowStart = now.minus(Duration.ofDays(VfpProgress.SLIDING_WINDOW_DAYS));

    List<UserTransaction> validTransactions = fetchValidTransactions(cleanUserId, windowStart, now);
    int count = validTransactions.size();
    boolean isVfpActive = resolveIsVfp(cleanUserId);

    int remainingToQualify = isVfpActive ? 0 : Math.max(0, VfpProgress.VFP_THRESHOLD - count);
    RevocationForecast forecast =
        isVfpActive
            ? computeForecast(validTransactions, count, now)
            : RevocationForecast.inactive();

    return VfpProgress.builder()
        .userId(cleanUserId)
        .isVfp(isVfpActive)
        .currentTransactionCount(count)
        .threshold(VfpProgress.VFP_THRESHOLD)
        .remainingTransactionsToQualify(remainingToQualify)
        .remainingTransactionsToMaintain(forecast.remainingToMaintain())
        .daysRemainingBeforeRevocation(forecast.daysRemaining())
        .hoursRemainingBeforeRevocation(forecast.hoursRemaining())
        .estimatedRevocationDate(forecast.estimatedRevocationDate())
        .expiringTransactionsCount(forecast.expiringTransactionsCount())
        .windowStart(windowStart)
        .windowEnd(now)
        .evaluatedAt(now)
        .build();
  }

  private List<UserTransaction> fetchValidTransactions(
      String userId, Instant windowStart, Instant windowEnd) {
    List<UserTransaction> transactions =
        salesTransactionPort.getApprovedTransactions(userId, windowStart, windowEnd);
    if (transactions == null) {
      return List.of();
    }
    return transactions.stream()
        .filter(t -> t.status() == null || "TRANSACTION_APPROVED".equalsIgnoreCase(t.status()))
        .filter(
            t ->
                t.createdAt() != null
                    && !t.createdAt().isBefore(windowStart)
                    && !t.createdAt().isAfter(windowEnd))
        .sorted(Comparator.comparing(UserTransaction::createdAt))
        .toList();
  }

  private boolean resolveIsVfp(String userId) {
    UserVfpStatus status =
        getUserVfpStatusQuery
            .getStatus(userId)
            .orElseGet(() -> evaluateAndApplyVfpStatusUseCase.evaluateAndApply(userId));
    return status != null && status.isVfp();
  }

  private RevocationForecast computeForecast(
      List<UserTransaction> validTransactions, int count, Instant now) {
    if (count < VfpProgress.VFP_THRESHOLD) {
      return new RevocationForecast(VfpProgress.VFP_THRESHOLD - count, 0L, 0L, now, 0);
    }

    int criticalIndex = count - VfpProgress.VFP_THRESHOLD;
    UserTransaction criticalTx = validTransactions.get(criticalIndex);
    Instant revocationInstant =
        criticalTx.createdAt().plus(Duration.ofDays(VfpProgress.SLIDING_WINDOW_DAYS));

    if (!revocationInstant.isAfter(now)) {
      return new RevocationForecast(1, 0L, 0L, revocationInstant, 1);
    }

    Duration diff = Duration.between(now, revocationInstant);
    long days = Math.max(0L, diff.toDays());
    long hours = Math.max(0L, diff.toHoursPart());

    int expiringCount =
        (int)
            validTransactions.stream()
                .filter(t -> !t.createdAt().isAfter(criticalTx.createdAt()))
                .count();

    int remainingAfter = count - expiringCount;
    int needed = Math.max(1, VfpProgress.VFP_THRESHOLD - remainingAfter);

    return new RevocationForecast(needed, days, hours, revocationInstant, expiringCount);
  }

  private record RevocationForecast(
      int remainingToMaintain,
      Long daysRemaining,
      Long hoursRemaining,
      Instant estimatedRevocationDate,
      int expiringTransactionsCount) {

    static RevocationForecast inactive() {
      return new RevocationForecast(0, null, null, null, 0);
    }
  }
}
