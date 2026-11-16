package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.event;

import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Driving adapter consuming transaction approved events from Kafka to trigger dynamic VFP
 * evaluation (SLE-45).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaTransactionApprovedConsumer {

  private final EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase;

  @KafkaListener(
      topics = "${application.kafka.topics.transaction-approved:shoploc.transactions.approved}",
      groupId = "${spring.kafka.consumer.group-id:loyalty-service-group}")
  @SuppressWarnings({"PMD.AvoidCatchingGenericException", "PMD.LawOfDemeter"})
  public void onTransactionApproved(TransactionApprovedEvent event) {
    if (event == null || event.userId() == null || event.userId().isBlank()) {
      if (log.isWarnEnabled()) {
        log.warn("Received null or blank TransactionApprovedEvent via Kafka, ignoring");
      }
      return;
    }

    if (log.isInfoEnabled()) {
      log.info(
          "Kafka transaction approved event received for userId={}, triggering VFP re-evaluation",
          event.userId());
    }

    try {
      evaluateAndApplyVfpStatusUseCase.evaluateAndApply(event.userId().trim());
    } catch (Exception ex) {
      if (log.isErrorEnabled()) {
        log.error(
            "Error evaluating VFP status via Kafka event for userId {}: {}",
            event.userId(),
            ex.getMessage(),
            ex);
      }
    }
  }
}
