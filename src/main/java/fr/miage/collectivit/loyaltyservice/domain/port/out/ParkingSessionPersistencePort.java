package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpParkingSession;
import java.time.LocalDate;
import java.util.Optional;

/** Outbound persistence port for VFP parking sessions. */
public interface ParkingSessionPersistencePort {

  boolean existsForUserOnDate(String userId, LocalDate sessionDate);

  VfpParkingSession save(VfpParkingSession session);

  Optional<VfpParkingSession> findLatestByUserAndDate(String userId, LocalDate sessionDate);
}
