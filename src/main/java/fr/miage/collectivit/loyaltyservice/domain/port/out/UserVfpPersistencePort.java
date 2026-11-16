package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpHistoryEntry;
import java.util.List;
import java.util.Optional;

/**
 * Outbound driven SPI port defining persistence contracts for user VFP statuses and audit history
 * (SLE-45 / SLE-199).
 */
public interface UserVfpPersistencePort {

  Optional<UserVfpStatus> findByUserId(String userId);

  UserVfpStatus saveStatus(UserVfpStatus status);

  void recordHistory(VfpHistoryEntry entry);

  List<VfpHistoryEntry> findHistoryByUserId(String userId);

  List<UserVfpStatus> findActiveVfpUsers(int page, int size);

  List<UserVfpStatus> findActiveVfpUsersAfter(String lastUserId, int size);
}
