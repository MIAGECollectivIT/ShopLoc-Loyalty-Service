package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import fr.miage.collectivit.loyaltyservice.domain.exception.ParkingGatewayUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.model.ParkingGatewayTicket;
import fr.miage.collectivit.loyaltyservice.domain.port.out.ParkingGatewayPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto.ParkingGatewayRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto.ParkingGatewayResponse;
import java.time.Duration;
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
 * Outbound secondary adapter calling the external parking ticket gateway (WireMock in dev/k8s) with
 * retry resilience (SLE-78 / SLE-259).
 */
@Slf4j
@Component
public class ParkingGatewayHttpAdapter implements ParkingGatewayPort {

  public static final String PARKING_SESSIONS_PATH = "/api/v1/parking/sessions";
  private static final int MAX_ATTEMPTS = 2;
  private static final int CONNECT_TIMEOUT_SECONDS = 5;
  private static final int READ_TIMEOUT_SECONDS = 10;

  @Setter private RestClient restClient;

  @Autowired
  public ParkingGatewayHttpAdapter(
      @Value("${services.parking-gateway.url:http://external-mock-service:8080}")
          String parkingGatewayUrl) {
    this(
        RestClient.builder()
            .baseUrl(parkingGatewayUrl)
            .requestFactory(createRequestFactory())
            .build());
  }

  ParkingGatewayHttpAdapter(RestClient restClient) {
    this.restClient = restClient;
  }

  private static SimpleClientHttpRequestFactory createRequestFactory() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS));
    factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));
    return factory;
  }

  @Override
  @SuppressWarnings("PMD.LawOfDemeter")
  public ParkingGatewayTicket requestParkingTicket(
      String userId, String licensePlate, int durationMinutes) {
    ParkingGatewayRequest payload =
        new ParkingGatewayRequest(userId, licensePlate, durationMinutes);
    RestClientException lastException = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        ParkingGatewayResponse response =
            restClient
                .post()
                .uri(PARKING_SESSIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve()
                .body(ParkingGatewayResponse.class);
        if (response == null) {
          throw new ParkingGatewayUnavailableException(
              "External parking gateway returned an empty payload");
        }
        return new ParkingGatewayTicket(
            response.status(),
            response.ticketReference(),
            response.licensePlate(),
            response.durationMinutes(),
            response.startedAt(),
            response.expiresAt());
      } catch (RestClientException ex) {
        lastException = ex;
        if (log.isWarnEnabled()) {
          log.warn(
              "Parking gateway call failed on attempt {}/{} for plate {}: {}",
              attempt,
              MAX_ATTEMPTS,
              licensePlate,
              ex.getMessage());
        }
      }
    }
    throw new ParkingGatewayUnavailableException(
        "Unable to reach external parking gateway after " + MAX_ATTEMPTS + " attempts",
        lastException);
  }
}
