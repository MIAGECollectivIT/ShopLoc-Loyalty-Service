package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import fr.miage.collectivit.loyaltyservice.domain.port.in.EvaluateAndApplyVfpStatusUseCase;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("KafkaTransactionApprovedConsumer Unit Tests")
class KafkaTransactionApprovedConsumerTest {

  private EvaluateAndApplyVfpStatusUseCase useCase;
  private KafkaTransactionApprovedConsumer consumer;

  @BeforeEach
  void setUp() {
    useCase = mock(EvaluateAndApplyVfpStatusUseCase.class);
    consumer = new KafkaTransactionApprovedConsumer(useCase);
  }

  @Test
  @DisplayName("Should invoke use case when valid event is received via Kafka")
  void shouldInvokeUseCaseOnValidEvent() {
    TransactionApprovedEvent event =
        new TransactionApprovedEvent("user-42", "txn-101", "REF-202", 55.0, "EUR", Instant.now());

    consumer.onTransactionApproved(event);

    verify(useCase).evaluateAndApply("user-42");
  }

  @Test
  @DisplayName("Should ignore null event or event without userId")
  void shouldIgnoreInvalidEvent() {
    consumer.onTransactionApproved(null);

    verify(useCase, never()).evaluateAndApply(any());
  }

  @Test
  @DisplayName("Should rethrow use case exception for Kafka DefaultErrorHandler retry and DLT")
  void shouldRethrowUseCaseExceptionForKafkaRetry() {
    TransactionApprovedEvent event =
        new TransactionApprovedEvent("user-err", "txn-102", "REF-203", 20.0, "EUR", Instant.now());
    doThrow(new RuntimeException("Database error")).when(useCase).evaluateAndApply("user-err");

    assertThatThrownBy(() -> consumer.onTransactionApproved(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessage("Database error");
  }
}
