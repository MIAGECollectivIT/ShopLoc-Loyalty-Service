package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Secondary Adapter - StoreServiceHttpAdapter Test")
class StoreServiceHttpAdapterTest {

  private MockRestServiceServer mockServer;
  private StoreServiceHttpAdapter adapter;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://store-service:8080");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    adapter = new StoreServiceHttpAdapter(builder.build());
  }

  @Test
  @DisplayName("Should return owner userId when store exists on Store Service")
  void shouldReturnOwnerIdWhenStoreExists() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"id\":10,\"name\":\"Boulangerie\",\"userId\":42}", MediaType.APPLICATION_JSON));

    Optional<Long> ownerId = adapter.findStoreOwnerId(10L);

    assertThat(ownerId).contains(42L);
    mockServer.verify();
  }

  @Test
  @DisplayName("Should return empty when Store Service returns 404 Not Found")
  void shouldReturnEmptyWhenStoreNotFound() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/999"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    Optional<Long> ownerId = adapter.findStoreOwnerId(999L);

    assertThat(ownerId).isEmpty();
    mockServer.verify();
  }

  @Test
  @DisplayName("Should gracefully handle Store Service 500 server error (fail-safe)")
  void shouldHandleServerErrorGracefully() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    Optional<Long> ownerId = adapter.findStoreOwnerId(10L);

    assertThat(ownerId).isEmpty();
    mockServer.verify();
  }
}
