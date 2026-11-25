package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.MerchantLoyaltyRateJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper.MerchantLoyaltyRateMapper;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataMerchantLoyaltyRateRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

/** Outbound persistence adapter connecting the domain persistence port to Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class MerchantLoyaltyRatePersistenceAdapter implements MerchantLoyaltyRatePersistencePort {

  private static final String CACHE_LOYALTY_RATES = "loyaltyRates";

  private final SpringDataMerchantLoyaltyRateRepository repository;
  private final MerchantLoyaltyRateMapper mapper;

  @Override
  @CacheEvict(value = CACHE_LOYALTY_RATES, allEntries = true)
  public MerchantLoyaltyRate save(MerchantLoyaltyRate rate) {
    MerchantLoyaltyRateJpaEntity jpaEntity = mapper.toJpaEntity(rate);
    MerchantLoyaltyRateJpaEntity saved = repository.save(jpaEntity);
    return mapper.toDomain(saved);
  }

  @Override
  @Cacheable(value = CACHE_LOYALTY_RATES, key = "#storeUid")
  public Optional<MerchantLoyaltyRate> findByStoreUid(UUID storeUid) {
    return repository.findByStoreUid(storeUid).map(mapper::toDomain);
  }

  @Override
  public List<MerchantLoyaltyRate> findByStoreUidIn(Collection<UUID> storeUids) {
    if (storeUids == null || storeUids.isEmpty()) {
      return List.of();
    }
    return repository.findByStoreUidIn(storeUids).stream().map(mapper::toDomain).toList();
  }

  @Override
  public boolean existsByStoreUid(UUID storeUid) {
    return repository.existsByStoreUid(storeUid);
  }

  @Override
  @CacheEvict(value = CACHE_LOYALTY_RATES, allEntries = true)
  public void deleteByStoreUid(UUID storeUid) {
    repository.deleteByStoreUid(storeUid);
  }
}
