package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.domain.model.VfpTransitTicket;

/** Primary inbound port for activating a free daily VFP public transit ticket (SLE-47). */
public interface ActivateTransitTicketUseCase {

  VfpTransitTicket activateTicket(String userId);
}
