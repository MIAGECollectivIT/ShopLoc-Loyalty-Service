package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpParkingSession;

/**
 * Inbound port (Use Case) for activating and acknowledging a 20-minute VFP free parking session
 * (SLE-78 / SLE-46).
 */
public interface ActivateParkingSessionUseCase {

  VfpParkingSession activateSession(String userId, String licensePlate);
}
