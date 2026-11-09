package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.MerchantLoyaltyRateJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper.MerchantLoyaltyRateMapper;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataMerchantLoyaltyRateRepository;
import java.util.Optional;
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
  public Optional<MerchantLoyaltyRate> findByStoreId(Long storeId) {
    return repository.findByStoreId(storeId).map(mapper::toDomain);
  }

  @Override
  public boolean existsByStoreId(Long storeId) {
    return repository.existsByStoreId(storeId);
  }

  @Override
  public void deleteByStoreId(Long storeId) {
    repository.deleteByStoreId(storeId);
  }
}
