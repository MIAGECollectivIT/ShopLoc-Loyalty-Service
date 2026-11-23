package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.MerchantLoyaltyRateJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper.MerchantLoyaltyRateMapper;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataMerchantLoyaltyRateRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Outbound persistence adapter connecting the domain persistence port to Spring Data JPA. */
@Component
@RequiredArgsConstructor
public class MerchantLoyaltyRatePersistenceAdapter implements MerchantLoyaltyRatePersistencePort {

  private final SpringDataMerchantLoyaltyRateRepository repository;
  private final MerchantLoyaltyRateMapper mapper;

  @Override
  public MerchantLoyaltyRate save(MerchantLoyaltyRate rate) {
    MerchantLoyaltyRateJpaEntity jpaEntity = mapper.toJpaEntity(rate);
    MerchantLoyaltyRateJpaEntity saved = repository.save(jpaEntity);
    return mapper.toDomain(saved);
  }

  @Override
  public Optional<MerchantLoyaltyRate> findByStoreUid(UUID storeUid) {
    return repository.findByStoreUid(storeUid).map(mapper::toDomain);
  }

  @Override
  public boolean existsByStoreUid(UUID storeUid) {
    return repository.existsByStoreUid(storeUid);
  }

  @Override
  public void deleteByStoreUid(UUID storeUid) {
    repository.deleteByStoreUid(storeUid);
  }
}
