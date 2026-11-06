package fr.miage.collectivit.loyaltyservice.repository;

import fr.miage.collectivit.loyaltyservice.entity.MerchantLoyaltyRate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository Spring Data JPA pour la persistance du barème de fidélité des commerçants (Story
 * SLE-55 / SLE-145).
 */
@Repository
public interface MerchantLoyaltyRateRepository extends JpaRepository<MerchantLoyaltyRate, Long> {

  Optional<MerchantLoyaltyRate> findByStoreId(Long storeId);

  boolean existsByStoreId(Long storeId);

  void deleteByStoreId(Long storeId);
}
