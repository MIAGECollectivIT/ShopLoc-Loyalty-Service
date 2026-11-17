package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import fr.miage.collectivit.loyaltyservice.domain.exception.ParkingGatewayUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.model.ParkingGatewayTicket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Secondary Adapter - ParkingGatewayHttpAdapter Test (SLE-78)")
class ParkingGatewayHttpAdapterTest {

  private static final String GATEWAY_URL =
      "http://external-mock-service:8080/api/v1/parking/sessions";

  private MockRestServiceServer mockServer;
  private ParkingGatewayHttpAdapter adapter;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://external-mock-service:8080");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    adapter = new ParkingGatewayHttpAdapter(builder.build());
  }

  @Test
  @DisplayName("Should return confirmed ticket when external parking gateway responds with 200 OK")
  void shouldReturnTicketOnSuccess() {
    String json =
        """
        {
          "status": "CONFIRMED",
          "ticketReference": "PARK-1234",
          "licensePlate": "AA-123-BB",
          "durationMinutes": 20,
          "startedAt": "2026-11-17T14:00:00Z",
          "expiresAt": "2026-11-17T14:20:00Z"
        }
        """;

    mockServer
        .expect(requestTo(GATEWAY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    ParkingGatewayTicket ticket = adapter.requestParkingTicket("user-1", "AA-123-BB", 20);

    assertThat(ticket.status()).isEqualTo("CONFIRMED");
    assertThat(ticket.ticketReference()).isEqualTo("PARK-1234");
    assertThat(ticket.licensePlate()).isEqualTo("AA-123-BB");
    mockServer.verify();
  }

  @Test
  @DisplayName("Should retry once on 503 and succeed on second attempt")
  void shouldRetryOnTransient503() {
    String json =
        """
        {
          "status": "CONFIRMED",
          "ticketReference": "PARK-RETRY",
          "licensePlate": "AA-123-BB",
          "durationMinutes": 20,
          "startedAt": "2026-11-17T14:00:00Z",
          "expiresAt": "2026-11-17T14:20:00Z"
        }
        """;

    mockServer
        .expect(requestTo(GATEWAY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    mockServer
        .expect(requestTo(GATEWAY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    ParkingGatewayTicket ticket = adapter.requestParkingTicket("user-1", "AA-123-BB", 20);

    assertThat(ticket.ticketReference()).isEqualTo("PARK-RETRY");
    mockServer.verify();
  }

  @Test
  @DisplayName("Should throw ParkingGatewayUnavailableException when gateway fails twice")
  void shouldThrowWhenGatewayUnavailable() {
    mockServer
        .expect(requestTo(GATEWAY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
    mockServer
        .expect(requestTo(GATEWAY_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> adapter.requestParkingTicket("user-1", "AA-123-BB", 20))
        .isInstanceOf(ParkingGatewayUnavailableException.class);
    mockServer.verify();
  }
}
