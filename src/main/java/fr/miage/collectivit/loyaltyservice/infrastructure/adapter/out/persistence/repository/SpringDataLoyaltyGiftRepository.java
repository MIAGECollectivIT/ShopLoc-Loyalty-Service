package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataLoyaltyGiftRepository extends JpaRepository<LoyaltyGiftJpaEntity, Long> {

  List<LoyaltyGiftJpaEntity> findByStoreIdOrderByIdAsc(Long storeId);

  List<LoyaltyGiftJpaEntity> findByActiveTrueOrderByIdAsc();

  List<LoyaltyGiftJpaEntity> findByStoreIdAndActiveTrueOrderByIdAsc(Long storeId);

  Optional<LoyaltyGiftJpaEntity> findByIdAndStoreId(Long id, Long storeId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM LoyaltyGiftJpaEntity g WHERE g.id = :id AND g.storeId = :storeId")
  Optional<LoyaltyGiftJpaEntity> findByIdAndStoreIdForUpdate(
      @Param("id") Long id, @Param("storeId") Long storeId);
}
