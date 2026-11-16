package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataLoyaltyGiftRepository extends JpaRepository<LoyaltyGiftJpaEntity, Long> {

  List<LoyaltyGiftJpaEntity> findByStoreIdOrderByIdAsc(Long storeId);

  List<LoyaltyGiftJpaEntity> findByStoreIdAndActiveTrueOrderByIdAsc(Long storeId);

  Optional<LoyaltyGiftJpaEntity> findByIdAndStoreId(Long id, Long storeId);
}
