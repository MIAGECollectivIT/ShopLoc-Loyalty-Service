package fr.miage.collectivit.loyaltyservice.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import fr.miage.collectivit.loyaltyservice.domain.model.UserActivityEvaluation;
import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpEventType;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpHistoryEntry;
import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateUserActivityUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@DisplayName("Application Service - UserVfpStatusService Test (SLE-45 / SLE-200)")
class UserVfpStatusServiceTest {

  private static final Instant FIXED_NOW = Instant.parse("2026-11-14T15:00:00Z");

  private EvaluateUserActivityUseCase evaluateUserActivityUseCase;
  private UserVfpPersistencePort userVfpPersistencePort;
  private UserVfpStatusService service;

  @BeforeEach
  void setUp() {
    evaluateUserActivityUseCase = mock(EvaluateUserActivityUseCase.class);
    userVfpPersistencePort = mock(UserVfpPersistencePort.class);
    Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
    service =
        new UserVfpStatusService(evaluateUserActivityUseCase, userVfpPersistencePort, fixedClock);
  }

  @Test
  @DisplayName("Should grant VFP status when user reaches threshold (>= 10) and was not VFP")
  void shouldGrantVfpStatusWhenEligible() {
    String userId = "USER-100";
    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(userId, 10, FIXED_NOW.minusSeconds(3600), FIXED_NOW);
    when(evaluateUserActivityUseCase.evaluateActivity(userId)).thenReturn(evaluation);
    when(userVfpPersistencePort.findByUserId(userId)).thenReturn(Optional.empty());
    when(userVfpPersistencePort.saveStatus(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserVfpStatus result = service.evaluateAndApply(userId);

    assertThat(result.isVfp()).isTrue();
    assertThat(result.getGrantedAt()).isEqualTo(FIXED_NOW);
    assertThat(result.getRevokedAt()).isNull();
    assertThat(result.getLastEvaluatedAt()).isEqualTo(FIXED_NOW);

    ArgumentCaptor<VfpHistoryEntry> historyCaptor = ArgumentCaptor.forClass(VfpHistoryEntry.class);
    verify(userVfpPersistencePort).recordHistory(historyCaptor.capture());
    VfpHistoryEntry captured = historyCaptor.getValue();
    assertThat(captured.getUserId()).isEqualTo(userId);
    assertThat(captured.getEventType()).isEqualTo(VfpEventType.VFP_GRANTED);
    assertThat(captured.getTransactionCount()).isEqualTo(10);
    assertThat(captured.getCreatedAt()).isEqualTo(FIXED_NOW);
  }

  @Test
  @DisplayName("Should revoke VFP status when user drops below threshold (< 10) and was VFP")
  void shouldRevokeVfpStatusWhenIneligible() {
    String userId = "USER-200";
    Instant previousGrantedAt = FIXED_NOW.minusSeconds(86400 * 5);
    UserVfpStatus existingVfp =
        new UserVfpStatus(userId, true, previousGrantedAt, null, previousGrantedAt);

    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(userId, 7, FIXED_NOW.minusSeconds(3600), FIXED_NOW);
    when(evaluateUserActivityUseCase.evaluateActivity(userId)).thenReturn(evaluation);
    when(userVfpPersistencePort.findByUserId(userId)).thenReturn(Optional.of(existingVfp));
    when(userVfpPersistencePort.saveStatus(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserVfpStatus result = service.evaluateAndApply(userId);

    assertThat(result.isVfp()).isFalse();
    assertThat(result.getGrantedAt()).isEqualTo(previousGrantedAt);
    assertThat(result.getRevokedAt()).isEqualTo(FIXED_NOW);
    assertThat(result.getLastEvaluatedAt()).isEqualTo(FIXED_NOW);

    ArgumentCaptor<VfpHistoryEntry> historyCaptor = ArgumentCaptor.forClass(VfpHistoryEntry.class);
    verify(userVfpPersistencePort).recordHistory(historyCaptor.capture());
    VfpHistoryEntry captured = historyCaptor.getValue();
    assertThat(captured.getUserId()).isEqualTo(userId);
    assertThat(captured.getEventType()).isEqualTo(VfpEventType.VFP_REVOKED);
    assertThat(captured.getTransactionCount()).isEqualTo(7);
  }

  @Test
  @DisplayName(
      "Should maintain active VFP status when activity remains >= 10 without duplicate audit entry")
  void shouldMaintainVfpStatusWhenStillEligible() {
    String userId = "USER-300";
    Instant previousGrantedAt = FIXED_NOW.minusSeconds(86400 * 2);
    UserVfpStatus existingVfp =
        new UserVfpStatus(userId, true, previousGrantedAt, null, previousGrantedAt);

    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(userId, 15, FIXED_NOW.minusSeconds(3600), FIXED_NOW);
    when(evaluateUserActivityUseCase.evaluateActivity(userId)).thenReturn(evaluation);
    when(userVfpPersistencePort.findByUserId(userId)).thenReturn(Optional.of(existingVfp));
    when(userVfpPersistencePort.saveStatus(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserVfpStatus result = service.evaluateAndApply(userId);

    assertThat(result.isVfp()).isTrue();
    assertThat(result.getGrantedAt()).isEqualTo(previousGrantedAt);
    assertThat(result.getRevokedAt()).isNull();
    assertThat(result.getLastEvaluatedAt()).isEqualTo(FIXED_NOW);

    verify(userVfpPersistencePort, never()).recordHistory(any());
  }

  @Test
  @DisplayName("Should maintain non-VFP status when activity remains < 10 without audit entry")
  void shouldMaintainNonVfpStatusWhenStillIneligible() {
    String userId = "USER-400";
    UserVfpStatus existingNonVfp =
        new UserVfpStatus(userId, false, null, null, FIXED_NOW.minusSeconds(86400));

    UserActivityEvaluation evaluation =
        UserActivityEvaluation.evaluate(userId, 4, FIXED_NOW.minusSeconds(3600), FIXED_NOW);
    when(evaluateUserActivityUseCase.evaluateActivity(userId)).thenReturn(evaluation);
    when(userVfpPersistencePort.findByUserId(userId)).thenReturn(Optional.of(existingNonVfp));
    when(userVfpPersistencePort.saveStatus(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    UserVfpStatus result = service.evaluateAndApply(userId);

    assertThat(result.isVfp()).isFalse();
    assertThat(result.getGrantedAt()).isNull();
    assertThat(result.getRevokedAt()).isNull();
    assertThat(result.getLastEvaluatedAt()).isEqualTo(FIXED_NOW);

    verify(userVfpPersistencePort, never()).recordHistory(any());
  }

  @Test
  @DisplayName("Should throw IllegalArgumentException when userId is blank or null")
  void shouldThrowExceptionWhenUserIdIsBlank() {
    assertThatThrownBy(() -> service.evaluateAndApply(null))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.evaluateAndApply("   "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Should retrieve persisted status or empty Optional via GetUserVfpStatusQuery")
  void shouldRetrieveStatusViaQuery() {
    when(userVfpPersistencePort.findByUserId("USER-500"))
        .thenReturn(Optional.of(new UserVfpStatus("USER-500", true, FIXED_NOW, null, FIXED_NOW)));

    Optional<UserVfpStatus> found = service.getStatus("USER-500");
    assertThat(found).isPresent();
    assertThat(found.get().isVfp()).isTrue();

    Optional<UserVfpStatus> empty = service.getStatus("NON-EXISTENT");
    assertThat(empty).isEmpty();
  }
}
