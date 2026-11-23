package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.event;

import fr.miage.collectivit.loyaltyservice.domain.port.in.ProcessOrderPaidLoyaltyUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.OrderPaidKafkaEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Driving adapter consuming click & collect order paid events from Kafka. */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaOrderPaidConsumer {

  private final ProcessOrderPaidLoyaltyUseCase processOrderPaidLoyaltyUseCase;

  @KafkaListener(
      topics = "${application.kafka.topics.order-payments:shoploc.order-payments}",
      groupId = "${spring.kafka.consumer.group-id:loyalty-service-group}")
  @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.LawOfDemeter"})
  public void onOrderPaid(OrderPaidKafkaEvent event) {
    if (event == null || event.orderReference() == null || event.orderReference().isBlank()) {
      if (log.isWarnEnabled()) {
        log.warn("Received null or blank OrderPaidKafkaEvent via Kafka, ignoring");
      }
      return;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Kafka order paid event received: orderRef={}, customerId={}, totalAmount={}",
          event.orderReference(),
          event.customerId(),
          event.totalAmount());
    }

    try {
      processOrderPaidLoyaltyUseCase.processOrderPaid(event);
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error(
            "Error processing order paid event via Kafka for orderRef {}: {}",
            event.orderReference(),
            ex.getMessage(),
            ex);
      }
      throw ex;
    }
  }
}
