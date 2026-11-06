package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.MerchantLoyaltyRateJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Interface Spring Data JPA interne à l'adaptateur de persistance. */
@Repository
public interface SpringDataMerchantLoyaltyRateRepository
    extends JpaRepository<MerchantLoyaltyRateJpaEntity, Long> {

  Optional<MerchantLoyaltyRateJpaEntity> findByStoreId(Long storeId);

  boolean existsByStoreId(Long storeId);

  void deleteByStoreId(Long storeId);
}
