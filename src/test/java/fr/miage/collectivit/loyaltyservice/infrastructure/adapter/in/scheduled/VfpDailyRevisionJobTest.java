package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.scheduled;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Scheduled Job - VfpDailyRevisionJob Test (SLE-45 / SLE-202)")
class VfpDailyRevisionJobTest {

  private UserVfpPersistencePort userVfpPersistencePort;
  private EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase;
  private SimpleMeterRegistry meterRegistry;
  private VfpDailyRevisionJob job;

  @BeforeEach
  void setUp() {
    userVfpPersistencePort = mock(UserVfpPersistencePort.class);
    evaluateAndApplyVfpStatusUseCase = mock(EvaluateAndApplyVfpStatusUseCase.class);
    meterRegistry = new SimpleMeterRegistry();
    job =
        new VfpDailyRevisionJob(
            userVfpPersistencePort, evaluateAndApplyVfpStatusUseCase, meterRegistry);
  }

  @Test
  @DisplayName("Should audit active VFP users and count revocations when activity expires")
  void shouldAuditAndRevokeExpiredVfpUsers() {
    Instant now = Instant.parse("2026-11-14T02:00:00Z");
    UserVfpStatus userA =
        new UserVfpStatus("USER-A", true, now.minusSeconds(86400 * 20), null, now);
    UserVfpStatus userB =
        new UserVfpStatus("USER-B", true, now.minusSeconds(86400 * 10), null, now);

    when(userVfpPersistencePort.findActiveVfpUsers(0, 50)).thenReturn(List.of(userA, userB));
    when(userVfpPersistencePort.findActiveVfpUsers(1, 50)).thenReturn(Collections.emptyList());

    // User A dropped under 10 -> revoked (isVfp = false)
    UserVfpStatus revokedA = new UserVfpStatus("USER-A", false, userA.getGrantedAt(), now, now);
    when(evaluateAndApplyVfpStatusUseCase.evaluateAndApply("USER-A")).thenReturn(revokedA);

    // User B maintained >= 10 -> still VFP (isVfp = true)
    UserVfpStatus maintainedB = new UserVfpStatus("USER-B", true, userB.getGrantedAt(), null, now);
    when(evaluateAndApplyVfpStatusUseCase.evaluateAndApply("USER-B")).thenReturn(maintainedB);

    int revokedCount = job.executeRevision();

    assertThat(revokedCount).isEqualTo(1);
    verify(evaluateAndApplyVfpStatusUseCase).evaluateAndApply("USER-A");
    verify(evaluateAndApplyVfpStatusUseCase).evaluateAndApply("USER-B");

    assertThat(meterRegistry.counter("loyalty.vfp.audits").count()).isEqualTo(2.0);
    assertThat(meterRegistry.counter("loyalty.vfp.revocations").count()).isEqualTo(1.0);
  }

  @Test
  @DisplayName("Should handle errors on individual user audit gracefully without stopping batch")
  void shouldHandleErrorsGracefullyDuringBatch() {
    Instant now = Instant.parse("2026-11-14T02:00:00Z");
    UserVfpStatus userError = new UserVfpStatus("USER-ERR", true, now, null, now);
    UserVfpStatus userValid = new UserVfpStatus("USER-OK", true, now, null, now);

    when(userVfpPersistencePort.findActiveVfpUsers(0, 50))
        .thenReturn(List.of(userError, userValid));
    when(userVfpPersistencePort.findActiveVfpUsers(1, 50)).thenReturn(Collections.emptyList());

    when(evaluateAndApplyVfpStatusUseCase.evaluateAndApply("USER-ERR"))
        .thenThrow(new RuntimeException("Simulated Sales Service timeout"));
    when(evaluateAndApplyVfpStatusUseCase.evaluateAndApply("USER-OK"))
        .thenReturn(new UserVfpStatus("USER-OK", true, now, null, now));

    int revokedCount = job.executeRevision();

    assertThat(revokedCount).isEqualTo(0);
    verify(evaluateAndApplyVfpStatusUseCase).evaluateAndApply("USER-ERR");
    verify(evaluateAndApplyVfpStatusUseCase).evaluateAndApply("USER-OK");
  }
}
