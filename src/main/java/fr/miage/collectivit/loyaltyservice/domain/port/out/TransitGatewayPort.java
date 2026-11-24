package fr.miage.collectivit.loyaltyservice.domain.port.out;

import fr.miage.collectivit.loyaltyservice.domain.model.TransitGatewayTicket;

/**
 * Outbound secondary port to request a digital transit ticket from the transport authority
 * (SLE-47).
 */
public interface TransitGatewayPort {

  TransitGatewayTicket requestTransitTicket(String userId, int durationMinutes);
}
