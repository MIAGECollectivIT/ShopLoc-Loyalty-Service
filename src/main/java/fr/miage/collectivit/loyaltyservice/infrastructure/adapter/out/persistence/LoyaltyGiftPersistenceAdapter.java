package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper.LoyaltyGiftMapper;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataLoyaltyGiftRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Driven persistence adapter for loyalty gift catalog storage (SLE-60 / SLE-191). */
@Component
@RequiredArgsConstructor
public class LoyaltyGiftPersistenceAdapter implements LoyaltyGiftPersistencePort {

  private final SpringDataLoyaltyGiftRepository repository;
  private final LoyaltyGiftMapper mapper;

  @Override
  public LoyaltyGift save(LoyaltyGift gift) {
    LoyaltyGiftJpaEntity entity = mapper.toEntity(gift);
    LoyaltyGiftJpaEntity saved = repository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<LoyaltyGift> findByUid(UUID uid) {
    if (uid == null) {
      return Optional.empty();
    }
    return repository.findByUid(uid).map(mapper::toDomain);
  }

  @Override
  public Optional<LoyaltyGift> findByUidAndStoreUid(UUID uid, UUID storeUid) {
    if (uid == null || storeUid == null) {
      return Optional.empty();
    }
    return repository.findByUidAndStoreUid(uid, storeUid).map(mapper::toDomain);
  }

  @Override
  public Optional<LoyaltyGift> findByUidAndStoreUidForUpdate(UUID uid, UUID storeUid) {
    if (uid == null || storeUid == null) {
      return Optional.empty();
    }
    return repository.findByUidAndStoreUidForUpdate(uid, storeUid).map(mapper::toDomain);
  }

  @Override
  public List<LoyaltyGift> findByStoreUid(UUID storeUid) {
    if (storeUid == null) {
      return List.of();
    }
    return repository.findByStoreUidOrderByIdAsc(storeUid).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<LoyaltyGift> findActiveByStoreUid(UUID storeUid) {
    if (storeUid == null) {
      return List.of();
    }
    return repository.findByStoreUidAndActiveTrueOrderByIdAsc(storeUid).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<LoyaltyGift> findAllActive() {
    return repository.findByActiveTrueOrderByIdAsc().stream().map(mapper::toDomain).toList();
  }

  @Override
  public void deleteByUid(UUID uid) {
    if (uid != null) {
      repository.findByUid(uid).ifPresent(repository::delete);
    }
  }
}
