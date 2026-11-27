package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsAccountJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.UserPointsTransactionJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper.UserPointsMapper;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataUserPointsAccountRepository;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataUserPointsTransactionRepository;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Secondary outbound persistence adapter implementing {@link UserPointsPersistencePort} via Spring
 * Data JPA (SLE-38 / SLE-176).
 */
@Component
@RequiredArgsConstructor
@SuppressWarnings("PMD.LawOfDemeter")
public class UserPointsPersistenceAdapter implements UserPointsPersistencePort {

  private final SpringDataUserPointsAccountRepository accountRepository;
  private final SpringDataUserPointsTransactionRepository transactionRepository;
  private final UserPointsMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public Optional<UserPointsAccount> findAccountByUserId(String userId) {
    return accountRepository.findByUserId(userId).map(mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<UserPointsAccount> findAccountByClientCode(String clientCode) {
    return accountRepository.findByClientCode(clientCode).map(mapper::toDomain);
  }

  @Override
  @Transactional
  public Optional<UserPointsAccount> findAccountByUserIdForUpdate(String userId) {
    return accountRepository.findByUserIdForUpdate(userId).map(mapper::toDomain);
  }

  @Override
  @Transactional
  public Optional<UserPointsAccount> findAccountByClientCodeForUpdate(String clientCode) {
    return accountRepository.findByClientCodeForUpdate(clientCode).map(mapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<PointsTransaction> findTransactionsByUserId(String userId) {
    return transactionRepository.findByUserIdOrderByCreatedAtDescIdDesc(userId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public boolean existsTransactionByReference(String transactionReference) {
    return transactionRepository.existsByTransactionReference(transactionReference);
  }

  @Override
  @Transactional(readOnly = true)
  public boolean hasPriorPurchaseInStore(String userId, UUID storeUid) {
    if (userId == null || userId.isBlank() || storeUid == null) {
      return false;
    }
    return transactionRepository.existsByUserIdAndStoreUidAndType(
        userId.trim(), storeUid, PointsTransactionType.EARN);
  }

  @Override
  @Transactional(readOnly = true)
  public Set<UUID> findEligibleStoreUidsForUser(String userId) {
    if (userId == null || userId.isBlank()) {
      return Set.of();
    }
    return transactionRepository.findDistinctStoreUidsByUserIdAndType(
        userId.trim(), PointsTransactionType.EARN);
  }

  @Override
  @Transactional
  public UserPointsAccount saveAccount(UserPointsAccount account) {
    UserPointsAccountJpaEntity entityToSave =
        accountRepository
            .findByUserIdForUpdate(account.userId())
            .map(
                existing -> {
                  existing.setClientCode(account.clientCode());
                  existing.setBalance(account.balance());
                  existing.setUpdatedAt(account.updatedAt());
                  return existing;
                })
            .orElseGet(() -> mapper.toEntity(account));

    UserPointsAccountJpaEntity saved = accountRepository.save(entityToSave);
    return mapper.toDomain(saved);
  }

  @Override
  @Transactional
  public PointsTransaction saveTransaction(Long accountId, PointsTransaction transaction) {
    UserPointsTransactionJpaEntity entity = mapper.toEntity(accountId, transaction);
    UserPointsTransactionJpaEntity saved = transactionRepository.save(entity);
    return mapper.toDomain(saved);
  }
}
