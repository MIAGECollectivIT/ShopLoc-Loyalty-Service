package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.PhysicalPaymentCompletedEvent;

/** Primary inbound use case for processing physical payment loyalty events (SLE-173). */
public interface ProcessPhysicalPaymentLoyaltyUseCase {

  void processPhysicalPayment(PhysicalPaymentCompletedEvent event);
}
