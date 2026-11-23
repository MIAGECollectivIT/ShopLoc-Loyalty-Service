package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import fr.miage.collectivit.loyaltyservice.domain.port.in.ProcessOrderPaidLoyaltyUseCase;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.OrderPaidKafkaEvent;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.StoreOrderBreakdown;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Kafka Adapter - KafkaOrderPaidConsumer Unit Tests")
class KafkaOrderPaidConsumerTest {

  @Mock private ProcessOrderPaidLoyaltyUseCase processOrderPaidLoyaltyUseCase;

  @InjectMocks private KafkaOrderPaidConsumer consumer;

  @Test
  @DisplayName("Should delegate valid OrderPaidKafkaEvent to use case")
  void shouldDelegateValidEventToUseCase() {
    StoreOrderBreakdown breakdown =
        new StoreOrderBreakdown(10L, "Boulangerie", new BigDecimal("25.00"));
    OrderPaidKafkaEvent event =
        new OrderPaidKafkaEvent(
            "CMD-CC-20261118-001",
            "user-123",
            "k7P9xQ2",
            new BigDecimal("25.00"),
            "EUR",
            List.of(breakdown),
            Instant.now());

    consumer.onOrderPaid(event);

    verify(processOrderPaidLoyaltyUseCase).processOrderPaid(event);
  }

  @Test
  @DisplayName("Should ignore event when null or order reference is null or blank")
  void shouldIgnoreNullOrBlankOrderReference() {
    consumer.onOrderPaid(null);

    OrderPaidKafkaEvent blankRefEvent =
        new OrderPaidKafkaEvent(
            "   ", "user-123", "k7P9xQ2", new BigDecimal("25.00"), "EUR", List.of(), Instant.now());
    consumer.onOrderPaid(blankRefEvent);

    OrderPaidKafkaEvent nullRefEvent =
        new OrderPaidKafkaEvent(
            null, "user-123", "k7P9xQ2", new BigDecimal("25.00"), "EUR", List.of(), Instant.now());
    consumer.onOrderPaid(nullRefEvent);

    verify(processOrderPaidLoyaltyUseCase, never()).processOrderPaid(any());
  }

  @Test
  @DisplayName("Should propagate exceptions to enable Kafka error handler retry and DLQ recovery")
  void shouldPropagateExceptionsToKafkaErrorHandler() {
    StoreOrderBreakdown breakdown =
        new StoreOrderBreakdown(10L, "Boulangerie", new BigDecimal("25.00"));
    OrderPaidKafkaEvent event =
        new OrderPaidKafkaEvent(
            "CMD-CC-20261118-ERR",
            "user-123",
            "k7P9xQ2",
            new BigDecimal("25.00"),
            "EUR",
            List.of(breakdown),
            Instant.now());

    doThrow(new IllegalStateException("Database connection pool exhausted"))
        .when(processOrderPaidLoyaltyUseCase)
        .processOrderPaid(event);

    assertThatThrownBy(() -> consumer.onOrderPaid(event))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Database connection pool exhausted");
  }
}
