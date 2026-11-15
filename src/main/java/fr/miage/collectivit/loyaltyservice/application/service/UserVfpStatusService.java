package fr.miage.collectivit.loyaltyservice.application.service;

import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpEventType;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpHistoryEntry;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateUserActivityUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetUserVfpStatusQuery;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core domain application service that evaluates transaction activity and orchestrates real-time
 * attribution, maintenance, and revocation of Very Faithful Person (VFP) status (SLE-45 / SLE-200).
 */
@Slf4j
@Service
public class UserVfpStatusService
    implements EvaluateAndApplyVfpStatusUseCase, GetUserVfpStatusQuery {

  private final EvaluateUserActivityUseCase evaluateUserActivityUseCase;
  private final UserVfpPersistencePort userVfpPersistencePort;
  private final Clock clock;

  @Autowired
  public UserVfpStatusService(
      EvaluateUserActivityUseCase evaluateUserActivityUseCase,
      UserVfpPersistencePort userVfpPersistencePort) {
    this(evaluateUserActivityUseCase, userVfpPersistencePort, Clock.systemUTC());
  }

  public UserVfpStatusService(
      EvaluateUserActivityUseCase evaluateUserActivityUseCase,
      UserVfpPersistencePort userVfpPersistencePort,
      Clock clock) {
    this.evaluateUserActivityUseCase = evaluateUserActivityUseCase;
    this.userVfpPersistencePort = userVfpPersistencePort;
    this.clock = clock;
  }

  @Override
  @Transactional
  public UserVfpStatus evaluateAndApply(String userId) {
    if (userId == null || userId.isBlank()) {
      throw new IllegalArgumentException("User ID must not be blank");
    }
    String cleanUserId = userId.trim();
    Instant now = clock.instant();

    UserActivityEvaluation evaluation = evaluateUserActivityUseCase.evaluateActivity(cleanUserId);
    int transactionCount = evaluation.getTransactionCount();
    boolean eligible = evaluation.isEligibleVfp();

    UserVfpStatus currentStatus =
        userVfpPersistencePort
            .findByUserId(cleanUserId)
            .orElseGet(() -> UserVfpStatus.initial(cleanUserId, now));

    UserVfpStatus updatedStatus =
        determineNextStatus(currentStatus, eligible, transactionCount, now);

    return userVfpPersistencePort.saveStatus(updatedStatus);
  }

  private UserVfpStatus determineNextStatus(
      UserVfpStatus current, boolean eligible, int transactionCount, Instant now) {
    String userId = current.getUserId();
    if (eligible && !current.isVfp()) {
      userVfpPersistencePort.recordHistory(
          VfpHistoryEntry.of(userId, VfpEventType.VFP_GRANTED, transactionCount, now));
      if (log.isInfoEnabled()) {
        log.info(
            "User {} granted VFP status with {} transactions in sliding window.",
            userId,
            transactionCount);
      }
      return current.grant(now);
    }
    if (!eligible && current.isVfp()) {
      userVfpPersistencePort.recordHistory(
          VfpHistoryEntry.of(userId, VfpEventType.VFP_REVOKED, transactionCount, now));
      if (log.isInfoEnabled()) {
        log.info("User {} revoked VFP status (transactions fell to {}).", userId, transactionCount);
      }
      return current.revoke(now);
    }
    return current.maintain(now);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserVfpStatus> getStatus(String userId) {
    if (userId == null || userId.isBlank()) {
      return Optional.empty();
    }
    return userVfpPersistencePort.findByUserId(userId.trim());
  }
}
