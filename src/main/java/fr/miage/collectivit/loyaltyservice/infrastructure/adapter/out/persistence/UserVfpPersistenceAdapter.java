package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.UserVfpStatus;
import fr.miage.collectivit.loyaltyservice.domain.model.VfpHistoryEntry;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVfpPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserVfpHistoryJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserVfpStatusJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper.UserVfpMapper;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataUserVfpHistoryRepository;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataUserVfpStatusRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Secondary driven persistence adapter implementing UserVfpPersistencePort (SLE-45 / SLE-199). */
@Component
@RequiredArgsConstructor
public class UserVfpPersistenceAdapter implements UserVfpPersistencePort {

  private final SpringDataUserVfpStatusRepository statusRepository;
  private final SpringDataUserVfpHistoryRepository historyRepository;
  private final UserVfpMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public Optional<UserVfpStatus> findByUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      return Optional.empty();
    }
    return statusRepository.findById(userId.trim()).map(mapper::toDomain);
  }

  @Override
  @Transactional
  public UserVfpStatus saveStatus(UserVfpStatus status) {
    if (status == null) {
      throw new IllegalArgumentException("UserVfpStatus must not be null");
    }
    UserVfpStatusJpaEntity entity = mapper.toJpaEntity(status);
    UserVfpStatusJpaEntity saved = statusRepository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  @Transactional
  public void recordHistory(VfpHistoryEntry entry) {
    if (entry == null) {
      throw new IllegalArgumentException("VfpHistoryEntry must not be null");
    }
    UserVfpHistoryJpaEntity entity = mapper.toJpaEntity(entry);
    historyRepository.save(entity);
  }

  @Override
  @Transactional(readOnly = true)
  public List<VfpHistoryEntry> findHistoryByUserId(String userId) {
    if (userId == null || userId.isBlank()) {
      return Collections.emptyList();
    }
    return historyRepository.findByUserIdOrderByCreatedAtDesc(userId.trim()).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<UserVfpStatus> findActiveVfpUsers(int page, int size) {
    int validPage = Math.max(0, page);
    int validSize = size > 0 ? size : 50;
    return statusRepository
        .findByIsVfpTrue(PageRequest.of(validPage, validSize))
        .map(mapper::toDomain)
        .getContent();
  }
}
