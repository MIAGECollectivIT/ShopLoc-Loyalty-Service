package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.LoyaltyGiftJpaEntity;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpringDataLoyaltyGiftRepository extends JpaRepository<LoyaltyGiftJpaEntity, Long> {

  Optional<LoyaltyGiftJpaEntity> findByUid(UUID uid);

  List<LoyaltyGiftJpaEntity> findByStoreUidOrderByIdAsc(UUID storeUid);

  List<LoyaltyGiftJpaEntity> findByActiveTrueOrderByIdAsc();

  List<LoyaltyGiftJpaEntity> findByStoreUidAndActiveTrueOrderByIdAsc(UUID storeUid);

  Optional<LoyaltyGiftJpaEntity> findByUidAndStoreUid(UUID uid, UUID storeUid);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT g FROM LoyaltyGiftJpaEntity g WHERE g.uid = :uid AND g.storeUid = :storeUid")
  Optional<LoyaltyGiftJpaEntity> findByUidAndStoreUidForUpdate(
      @Param("uid") UUID uid, @Param("storeUid") UUID storeUid);
}
