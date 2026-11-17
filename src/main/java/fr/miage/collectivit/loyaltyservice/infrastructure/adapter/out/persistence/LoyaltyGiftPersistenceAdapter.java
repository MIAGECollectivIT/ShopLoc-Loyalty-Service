package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.LoyaltyGift;
import fr.miage.collectivit.loyaltyservice.domain.port.out.LoyaltyGiftPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper.LoyaltyGiftMapper;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataLoyaltyGiftRepository;
import java.util.List;
import java.util.Optional;
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
  public Optional<LoyaltyGift> findById(Long id) {
    if (id == null) {
      return Optional.empty();
    }
    return repository.findById(id).map(mapper::toDomain);
  }

  @Override
  public Optional<LoyaltyGift> findByIdAndStoreId(Long id, Long storeId) {
    if (id == null || storeId == null) {
      return Optional.empty();
    }
    return repository.findByIdAndStoreId(id, storeId).map(mapper::toDomain);
  }

  @Override
  public Optional<LoyaltyGift> findByIdAndStoreIdForUpdate(Long id, Long storeId) {
    if (id == null || storeId == null) {
      return Optional.empty();
    }
    return repository.findByIdAndStoreIdForUpdate(id, storeId).map(mapper::toDomain);
  }

  @Override
  public List<LoyaltyGift> findByStoreId(Long storeId) {
    if (storeId == null) {
      return List.of();
    }
    return repository.findByStoreIdOrderByIdAsc(storeId).stream().map(mapper::toDomain).toList();
  }

  @Override
  public List<LoyaltyGift> findActiveByStoreId(Long storeId) {
    if (storeId == null) {
      return List.of();
    }
    return repository.findByStoreIdAndActiveTrueOrderByIdAsc(storeId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<LoyaltyGift> findAllActive() {
    return repository.findByActiveTrueOrderByIdAsc().stream().map(mapper::toDomain).toList();
  }

  @Override
  public void deleteById(Long id) {
    if (id != null) {
      repository.deleteById(id);
    }
  }
}
