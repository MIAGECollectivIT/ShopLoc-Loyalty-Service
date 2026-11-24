package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import fr.miage.collectivit.loyaltyservice.domain.exception.TransitGatewayUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.model.TransitGatewayTicket;
import fr.miage.collectivit.loyaltyservice.domain.port.out.TransitGatewayPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto.TransitGatewayRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto.TransitGatewayResponse;
import java.time.Duration;
import java.time.Instant;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Outbound secondary adapter calling the external transit gateway simulator with retry resilience
 * (SLE-47).
 */
@Slf4j
@Component
public class TransitGatewayHttpAdapter implements TransitGatewayPort {

  public static final String TRANSIT_TICKETS_PATH = "/api/v1/transit/tickets";
  private static final int MAX_ATTEMPTS = 2;
  private static final int CONNECT_TIMEOUT_SECONDS = 5;
  private static final int READ_TIMEOUT_SECONDS = 10;

  @Setter private RestClient restClient;

  @Autowired
  public TransitGatewayHttpAdapter(
      @Value(
              "${services.transit-gateway.url:${services.parking-gateway.url:http://external-mock-service:8080}}")
          String transitGatewayUrl) {
    this(
        RestClient.builder()
            .baseUrl(transitGatewayUrl)
            .requestFactory(createRequestFactory())
            .build());
  }

  TransitGatewayHttpAdapter(RestClient restClient) {
    this.restClient = restClient;
  }

  private static SimpleClientHttpRequestFactory createRequestFactory() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS));
    factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));
    return factory;
  }

  @Override
  public TransitGatewayTicket requestTransitTicket(String userId, int durationMinutes) {
    TransitGatewayRequest payload = new TransitGatewayRequest(userId, durationMinutes);

    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        TransitGatewayResponse response = executeCall(payload);
        return mapToGatewayTicket(response, durationMinutes);
      } catch (RestClientException ex) {
        handleCallFailure(attempt, ex);
      }
    }

    throw new TransitGatewayUnavailableException(
        "External transit gateway request exhausted retries without a valid response");
  }

  @SuppressWarnings("PMD.LawOfDemeter")
  private TransitGatewayResponse executeCall(TransitGatewayRequest payload) {
    TransitGatewayResponse response =
        restClient
            .post()
            .uri(TRANSIT_TICKETS_PATH)
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(TransitGatewayResponse.class);

    if (response == null
        || response.ticketReference() == null
        || response.ticketReference().isBlank()) {
      throw new TransitGatewayUnavailableException(
          "External transit gateway returned an empty ticket response");
    }
    return response;
  }

  private TransitGatewayTicket mapToGatewayTicket(
      TransitGatewayResponse response, int durationMinutes) {
    Instant startedAt = response.startedAt() != null ? response.startedAt() : Instant.now();
    Instant expiresAt =
        response.expiresAt() != null
            ? response.expiresAt()
            : startedAt.plus(Duration.ofMinutes(durationMinutes));

    return new TransitGatewayTicket(
        response.ticketReference(),
        response.ticketCode() != null ? response.ticketCode() : "BUS-VFP",
        response.networkName() != null ? response.networkName() : "Réseau Urbain Collectiv'IT",
        durationMinutes,
        startedAt,
        expiresAt,
        response.status() != null ? response.status() : "CONFIRMED");
  }

  private void handleCallFailure(int attempt, RestClientException ex) {
    if (log.isWarnEnabled()) {
      log.warn(
          "Call to external transit gateway failed on attempt {}/{}: {}",
          attempt,
          MAX_ATTEMPTS,
          ex.getMessage());
    }
    if (attempt == MAX_ATTEMPTS) {
      throw new TransitGatewayUnavailableException(
          "External transit gateway is unavailable: " + ex.getMessage(), ex);
    }
  }
}
