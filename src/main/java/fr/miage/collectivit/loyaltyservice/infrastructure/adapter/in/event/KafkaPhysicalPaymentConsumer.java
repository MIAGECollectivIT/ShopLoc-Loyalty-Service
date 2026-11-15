package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.event;

import fr.miage.collectivit.loyaltyservice.domain.port.in.ProcessPhysicalPaymentLoyaltyUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.PhysicalPaymentCompletedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Driving adapter consuming physical payment completed events from Kafka (SLE-173).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPhysicalPaymentConsumer {

  private final ProcessPhysicalPaymentLoyaltyUseCase processPhysicalPaymentLoyaltyUseCase;

  @KafkaListener(
      topics = "${application.kafka.topics.physical-payments:shoploc.physical-payments}",
      groupId = "${spring.kafka.consumer.group-id:loyalty-service-group}")
  @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.LawOfDemeter"})
  public void onPhysicalPaymentCompleted(PhysicalPaymentCompletedEvent event) {
    if (event == null || event.transactionReference() == null || event.transactionReference().isBlank()) {
      if (log.isWarnEnabled()) {
        log.warn("Received null or blank PhysicalPaymentCompletedEvent via Kafka, ignoring");
      }
      return;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Kafka physical payment event received: txRef={}, storeId={}, clientCode={}, amount={}",
          event.transactionReference(),
          event.storeId(),
          event.clientCode(),
          event.amount());
    }

    try {
      processPhysicalPaymentLoyaltyUseCase.processPhysicalPayment(event);
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error(
            "Error processing physical payment event via Kafka for txRef {}: {}",
            event.transactionReference(),
            ex.getMessage(),
            ex);
      }
    }
  }
}

