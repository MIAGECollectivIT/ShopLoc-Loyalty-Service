package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import fr.miage.collectivit.loyaltyservice.domain.port.in.ProcessPhysicalPaymentLoyaltyUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.PhysicalPaymentCompletedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kafka Adapter - KafkaPhysicalPaymentConsumer Unit Tests (SLE-173)")
class KafkaPhysicalPaymentConsumerTest {

  private static final UUID STORE_UID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");

  @Mock private ProcessPhysicalPaymentLoyaltyUseCase processPhysicalPaymentLoyaltyUseCase;

  @InjectMocks private KafkaPhysicalPaymentConsumer consumer;

  @Test
  @DisplayName("Should delegate valid PhysicalPaymentCompletedEvent to use case")
  void shouldDelegateValidEventToUseCase() {
    PhysicalPaymentCompletedEvent event =
        new PhysicalPaymentCompletedEvent(
            "TX-PHY-100",
            STORE_UID,
            "k7P9xQ2",
            "user-123",
            new BigDecimal("25.00"),
            "EUR",
            Instant.now());

    consumer.onPhysicalPaymentCompleted(event);

    verify(processPhysicalPaymentLoyaltyUseCase).processPhysicalPayment(event);
  }

  @Test
  @DisplayName("Should ignore event when null or transaction reference is null or blank")
  void shouldIgnoreNullOrBlankTransactionReference() {
    consumer.onPhysicalPaymentCompleted(null);

    PhysicalPaymentCompletedEvent blankRefEvent =
        new PhysicalPaymentCompletedEvent(
            "   ", STORE_UID, "k7P9xQ2", "user-123", new BigDecimal("25.00"), "EUR", Instant.now());
    consumer.onPhysicalPaymentCompleted(blankRefEvent);

    verify(processPhysicalPaymentLoyaltyUseCase, never()).processPhysicalPayment(any());
  }

  @Test
  @DisplayName("Should propagate exceptions to enable Kafka error handler retry and DLQ recovery")
  void shouldPropagateExceptionsToEnableDlqAndRetryHandling() {
    PhysicalPaymentCompletedEvent event =
        new PhysicalPaymentCompletedEvent(
            "TX-ERR-500",
            STORE_UID,
            "k7P9xQ2",
            "user-123",
            new BigDecimal("25.00"),
            "EUR",
            Instant.now());

    doThrow(new RuntimeException("Simulated error in persistence"))
        .when(processPhysicalPaymentLoyaltyUseCase)
        .processPhysicalPayment(event);

    assertThatThrownBy(() -> consumer.onPhysicalPaymentCompleted(event))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Simulated error in persistence");
  }
}
