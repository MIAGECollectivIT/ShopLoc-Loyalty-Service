package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.event;

import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Asynchronous event listener consuming transaction approval events to trigger non-blocking VFP
 * status re-evaluation for the user (SLE-45 / SLE-201).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TransactionApprovedEventListener {

  private final EvaluateAndApplyVfpStatusUseCase evaluateAndApplyVfpStatusUseCase;

  @Async
  @EventListener
  @SuppressWarnings("PMD.AvoidCatchingGenericException")
  public void onTransactionApproved(TransactionApprovedEvent event) {
    if (event == null || event.userId() == null || event.userId().isBlank()) {
      return;
    }
    String userId = event.userId().trim();
    if (log.isInfoEnabled()) {
      log.info(
          "Received TransactionApprovedEvent for user {}. Triggering VFP re-evaluation.", userId);
    }
    try {
      evaluateAndApplyVfpStatusUseCase.evaluateAndApply(userId);
    } catch (RuntimeException ex) {
      if (log.isErrorEnabled()) {
        log.error(
            "Failed to re-evaluate VFP status for user {} upon transaction event: {}",
            userId,
            ex.getMessage(),
            ex);
      }
    }
  }
}
