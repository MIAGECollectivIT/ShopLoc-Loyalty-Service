package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.scheduled;

import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled nightly batch job running periodic VFP qualification audits to automatically revoke
 * expired statuses when purchasing activity falls below 10 transactions over 15 days (SLE-45 /
 * SLE-202).
 */
@Slf4j
@Component
public class VfpDailyRevisionJob {

  private static final int BATCH_PAGE_SIZE = 50;

  private final UserVfpPersistencePort userVfpPersistencePort;
  private final EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase;
  private final MeterRegistry meterRegistry;

  @Autowired
  public VfpDailyRevisionJob(
      UserVfpPersistencePort userVfpPersistencePort,
      EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase,
      @Autowired(required = false) MeterRegistry meterRegistry) {
    this.userVfpPersistencePort = userVfpPersistencePort;
    this.evaluateAndApplyVfpStatusUseCase = evaluateAndApplyVfpStatusUseCase;
    this.meterRegistry = meterRegistry;
  }

  @Scheduled(cron = "${loyalty.vfp.revision.cron:0 0 2 * * ?}")
  public int executeRevision() {
    if (log.isInfoEnabled()) {
      log.info("Starting scheduled daily VFP revision batch job...");
    }
    long startTime = System.currentTimeMillis();
    int totalAudited = 0;
    int totalRevoked = 0;
    String lastUserId = null;

    List<UserVfpStatus> activeUsers =
        userVfpPersistencePort.findActiveVfpUsersAfter(lastUserId, BATCH_PAGE_SIZE);
    while (!activeUsers.isEmpty()) {
      for (UserVfpStatus user : activeUsers) {
        totalAudited++;
        if (auditUser(user)) {
          totalRevoked++;
        }
        lastUserId = user.getUserId();
      }
      if (activeUsers.size() < BATCH_PAGE_SIZE) {
        break;
      }
      activeUsers = userVfpPersistencePort.findActiveVfpUsersAfter(lastUserId, BATCH_PAGE_SIZE);
    }

    long duration = System.currentTimeMillis() - startTime;
    recordMetrics(duration, totalAudited, totalRevoked);

    return totalRevoked;
  }

  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  private boolean auditUser(UserVfpStatus user) {
    try {
      UserVfpStatus updated = evaluateAndApplyVfpStatusUseCase.evaluateAndApply(user.getUserId());
      return !updated.isVfp();
    } catch (RuntimeException ex) {
      if (log.isErrorEnabled()) {
        log.error(
            "Error auditing VFP status for user {} during scheduled revision: {}",
            user.getUserId(),
            ex.getMessage(),
            ex);
      }
      return false;
    }
  }

  private void recordMetrics(long duration, int totalAudited, int totalRevoked) {
    if (log.isInfoEnabled()) {
      log.info(
          "Daily VFP revision batch job completed in {} ms: totalAudited={}, totalRevoked={}",
          duration,
          totalAudited,
          totalRevoked);
    }

    if (meterRegistry != null) {
      meterRegistry.counter("loyalty.vfp.audits").increment(totalAudited);
      meterRegistry.counter("loyalty.vfp.revocations").increment(totalRevoked);
    }
  }
}
