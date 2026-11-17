package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.ParkingGatewayTicket;

/** Outbound port for registering a parking session with the third-party parking gateway. */
public interface ParkingGatewayPort {

  ParkingGatewayTicket requestParkingTicket(
      String userId, String licensePlate, int durationMinutes);
}
