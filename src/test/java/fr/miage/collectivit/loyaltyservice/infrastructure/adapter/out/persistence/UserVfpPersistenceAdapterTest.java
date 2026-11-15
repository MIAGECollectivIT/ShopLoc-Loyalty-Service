package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpEventType;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpHistoryEntry;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("Persistence Adapter - UserVfpPersistenceAdapter Integration Test (SLE-45 / SLE-199)")
class UserVfpPersistenceAdapterTest {

  @Autowired private UserVfpPersistenceAdapter persistenceAdapter;

  @Test
  @DisplayName("Should save and retrieve UserVfpStatus via adapter")
  void shouldSaveAndFindUserVfpStatus() {
    String userId = "USER-TEST-1";
    Instant now = Instant.parse("2026-11-14T12:00:00Z");
    UserVfpStatus status = new UserVfpStatus(userId, true, now, null, now);

    UserVfpStatus saved = persistenceAdapter.saveStatus(status);
    assertThat(saved).isNotNull();
    assertThat(saved.getUserId()).isEqualTo(userId);
    assertThat(saved.isVfp()).isTrue();

    Optional<UserVfpStatus> found = persistenceAdapter.findByUserId(userId);
    assertThat(found).isPresent();
    assertThat(found.get().getUserId()).isEqualTo(userId);
    assertThat(found.get().isVfp()).isTrue();
    assertThat(found.get().getGrantedAt()).isEqualTo(now);
    assertThat(found.get().getRevokedAt()).isNull();
  }

  @Test
  @DisplayName("Should record and query audit history entries ordered by created_at DESC")
  void shouldRecordAndQueryAuditHistory() {
    String userId = "USER-AUDIT-1";
    Instant t1 = Instant.parse("2026-11-01T10:00:00Z");
    Instant t2 = Instant.parse("2026-11-10T14:00:00Z");

    persistenceAdapter.recordHistory(VfpHistoryEntry.of(userId, VfpEventType.VFP_GRANTED, 11, t1));
    persistenceAdapter.recordHistory(VfpHistoryEntry.of(userId, VfpEventType.VFP_REVOKED, 8, t2));

    List<VfpHistoryEntry> history = persistenceAdapter.findHistoryByUserId(userId);
    assertThat(history).hasSize(2);
    assertThat(history.get(0).getEventType()).isEqualTo(VfpEventType.VFP_REVOKED);
    assertThat(history.get(0).getTransactionCount()).isEqualTo(8);
    assertThat(history.get(1).getEventType()).isEqualTo(VfpEventType.VFP_GRANTED);
    assertThat(history.get(1).getTransactionCount()).isEqualTo(11);
  }

  @Test
  @DisplayName("Should retrieve active VFP users paginated")
  void shouldRetrieveActiveVfpUsersPaginated() {
    Instant now = Instant.now();
    persistenceAdapter.saveStatus(new UserVfpStatus("ACTIVE-1", true, now, null, now));
    persistenceAdapter.saveStatus(new UserVfpStatus("ACTIVE-2", true, now, null, now));
    persistenceAdapter.saveStatus(new UserVfpStatus("INACTIVE-3", false, null, now, now));

    List<UserVfpStatus> activeUsers = persistenceAdapter.findActiveVfpUsers(0, 10);
    assertThat(activeUsers)
        .extracting(UserVfpStatus::getUserId)
        .contains("ACTIVE-1", "ACTIVE-2")
        .doesNotContain("INACTIVE-3");
  }
}
