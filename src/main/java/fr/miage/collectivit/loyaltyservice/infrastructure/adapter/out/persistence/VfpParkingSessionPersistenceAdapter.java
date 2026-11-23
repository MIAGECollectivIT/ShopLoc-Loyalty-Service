package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpParkingSession;
import fr.miage.collectivit.loyaltyservice.domain.port.out.ParkingSessionPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.entity.VfpParkingSessionJpaEntity;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.mapper.VfpParkingSessionMapper;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.persistence.repository.SpringDataVfpParkingSessionRepository;
import java.time.LocalDate;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Secondary driven persistence adapter for VFP parking sessions. */
@Component
@RequiredArgsConstructor
public class VfpParkingSessionPersistenceAdapter implements ParkingSessionPersistencePort {

  private final SpringDataVfpParkingSessionRepository repository;
  private final VfpParkingSessionMapper mapper;

  @Override
  @Transactional(readOnly = true)
  public boolean existsForUserOnDate(String userId, LocalDate sessionDate) {
    if (userId == null || userId.isBlank() || sessionDate == null) {
      return false;
    }
    return repository.existsByUserIdAndSessionDate(userId.trim(), sessionDate);
  }

  @Override
  @Transactional
  public VfpParkingSession save(VfpParkingSession session) {
    if (session == null) {
      throw new IllegalArgumentException("VfpParkingSession must not be null");
    }
    VfpParkingSessionJpaEntity entity = mapper.toJpaEntity(session);
    VfpParkingSessionJpaEntity saved = repository.save(entity);
    return mapper.toDomain(saved);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<VfpParkingSession> findLatestByUserAndDate(String userId, LocalDate sessionDate) {
    if (userId == null || userId.isBlank() || sessionDate == null) {
      return Optional.empty();
    }
    return repository
        .findFirstByUserIdAndSessionDateOrderByCreatedAtDesc(userId.trim(), sessionDate)
        .map(mapper::toDomain);
  }
}
