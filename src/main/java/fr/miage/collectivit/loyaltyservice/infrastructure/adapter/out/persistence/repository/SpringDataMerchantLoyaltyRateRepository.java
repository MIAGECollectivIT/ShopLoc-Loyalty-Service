package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.MerchantLoyaltyRateJpaEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Spring Data JPA repository internal to the persistence adapter. */
@Repository
public interface SpringDataMerchantLoyaltyRateRepository
    extends JpaRepository<MerchantLoyaltyRateJpaEntity, Long> {

  Optional<MerchantLoyaltyRateJpaEntity> findByStoreUid(UUID storeUid);

  List<MerchantLoyaltyRateJpaEntity> findByStoreUidIn(Collection<UUID> storeUids);

  boolean existsByStoreUid(UUID storeUid);

  void deleteByStoreUid(UUID storeUid);
}
