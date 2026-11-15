package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransaction;
import fr.miage.collectivit.loyaltyservice.domain.model.PointsTransactionType;
import fr.miage.collectivit.loyaltyservice.domain.model.UserPointsAccount;
import fr.miage.collectivit.loyaltyservice.domain.port.out.MerchantLoyaltyRatePersistencePort;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserPointsPersistencePort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.kafka.dto.PhysicalPaymentCompletedEvent;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@EmbeddedKafka(
    partitions = 4,
    topics = {"shoploc.physical-payments", "shoploc.transactions.approved"})
@DirtiesContext
@TestPropertySource(
    properties = {
      "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
      "spring.kafka.consumer.auto-offset-reset=earliest",
      "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
      "spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.ErrorHandlingDeserializer",
      "spring.kafka.consumer.properties.spring.deserializer.value.delegate.class=org.springframework.kafka.support.serializer.JsonDeserializer",
      "spring.kafka.consumer.properties.spring.json.trusted.packages=*",
      "spring.kafka.listener.auto-startup=true",
      "spring.kafka.admin.auto-create=true",
      "application.kafka.topics.physical-payments=shoploc.physical-payments"
    })
@DisplayName("Integration Test - End-to-End Physical Payment Kafka EDA Flow (SLE-175)")
class PhysicalPaymentKafkaIntegrationTest {

  @org.springframework.boot.test.context.TestConfiguration
  static class TestKafkaProducerConfig {
    @org.springframework.context.annotation.Bean
    @SuppressWarnings("deprecation")
    public org.springframework.kafka.core.ProducerFactory<String, Object> testProducerFactory(
        org.springframework.kafka.test.EmbeddedKafkaBroker embeddedKafka) {
      java.util.Map<String, Object> configs =
          new java.util.HashMap<>(
              org.springframework.kafka.test.utils.KafkaTestUtils.producerProps(embeddedKafka));
      configs.put(
          org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
          org.apache.kafka.common.serialization.StringSerializer.class);
      configs.put(
          org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
          org.springframework.kafka.support.serializer.JsonSerializer.class);
      return new org.springframework.kafka.core.DefaultKafkaProducerFactory<>(configs);
    }

    @org.springframework.context.annotation.Bean
    public KafkaTemplate<String, Object> kafkaTemplate(
        org.springframework.kafka.core.ProducerFactory<String, Object> testProducerFactory) {
      return new KafkaTemplate<>(testProducerFactory);
    }
  }

  @Autowired private KafkaTemplate<String, Object> kafkaTemplate;
  @Autowired private UserPointsPersistencePort userPointsPersistencePort;
  @Autowired private MerchantLoyaltyRatePersistencePort merchantLoyaltyRatePersistencePort;
  @Autowired private org.springframework.kafka.config.KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    kafkaListenerEndpointRegistry.start();
    for (org.springframework.kafka.listener.MessageListenerContainer container :
        kafkaListenerEndpointRegistry.getListenerContainers()) {
      org.springframework.kafka.test.utils.ContainerTestUtils.waitForAssignment(container, 4);
    }
  }

  @Test
  @DisplayName("Should consume physical payment event, credit points with merchant rate, and handle idempotence")
  void shouldConsumeEventAndCreditPointsIdempotently() {
    Long storeId = 20L;
    merchantLoyaltyRatePersistencePort.save(
        new MerchantLoyaltyRate(storeId, new BigDecimal("2.00"), new BigDecimal("5.0")));

    String userId = "user-e2e-kafka-001";
    String clientCode = "CARD-E2E-999";
    String txRef = "TX-E2E-PHY-001";

    // 20.00 EUR * 5.0 PTS / 2.00 EUR = 50.0 PTS
    PhysicalPaymentCompletedEvent event =
        new PhysicalPaymentCompletedEvent(
            txRef,
            storeId,
            clientCode,
            userId,
            new BigDecimal("20.00"),
            "EUR",
            Instant.now());

    kafkaTemplate.send("shoploc.physical-payments", clientCode, event);

    await()
        .atMost(Duration.ofSeconds(10))
        .untilAsserted(
            () -> {
              Optional<UserPointsAccount> accountOpt =
                  userPointsPersistencePort.findAccountByUserId(userId);
              assertThat(accountOpt).isPresent();
              assertThat(accountOpt.get().balance()).isEqualByComparingTo("50.0");

              List<PointsTransaction> txs = userPointsPersistencePort.findTransactionsByUserId(userId);
              assertThat(txs).hasSize(1);
              PointsTransaction tx = txs.get(0);
              assertThat(tx.transactionReference()).isEqualTo(txRef);
              assertThat(tx.type()).isEqualTo(PointsTransactionType.EARN);
              assertThat(tx.points()).isEqualByComparingTo("50.0");
              assertThat(tx.sourceType()).isEqualTo("PHYSICAL_PAYMENT");
            });

    // Idempotence test: re-send the exact same event
    kafkaTemplate.send("shoploc.physical-payments", clientCode, event);

    // Verify after 2 seconds that balance remains 50.0 and transaction count remains 1
    await()
        .pollDelay(Duration.ofSeconds(2))
        .atMost(Duration.ofSeconds(5))
        .untilAsserted(
            () -> {
              UserPointsAccount account =
                  userPointsPersistencePort.findAccountByUserId(userId).orElseThrow();
              assertThat(account.balance()).isEqualByComparingTo("50.0");
              List<PointsTransaction> txs = userPointsPersistencePort.findTransactionsByUserId(userId);
              assertThat(txs).hasSize(1);
            });
  }
}
