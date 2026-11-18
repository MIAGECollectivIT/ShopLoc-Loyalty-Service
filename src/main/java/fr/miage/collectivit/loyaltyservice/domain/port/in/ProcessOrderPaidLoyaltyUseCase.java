package fr.miage.collectivit.loyaltyservice.domain.port.in;

import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.OrderPaidKafkaEvent;

/**
 * Primary inbound use case for processing click & collect order paid loyalty
 * events.
 */
public interface ProcessOrderPaidLoyaltyUseCase {

  void processOrderPaid(OrderPaidKafkaEvent event);
}
