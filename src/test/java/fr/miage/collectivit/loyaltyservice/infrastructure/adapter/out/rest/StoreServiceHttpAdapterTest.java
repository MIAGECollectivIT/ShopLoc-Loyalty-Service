package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

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
  @DisplayName("Should return true when store is owned by user")
  void shouldReturnTrueWhenStoreOwnedByUser() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"id\":10,\"name\":\"Boulangerie\",\"userId\":42}", MediaType.APPLICATION_JSON));

    boolean isOwner = adapter.isStoreOwnedBy(10L, 42L);

    assertThat(isOwner).isTrue();
    mockServer.verify();
  }

  @Test
  @DisplayName("Should return false when store is owned by different user")
  void shouldReturnFalseWhenStoreOwnedByDifferentUser() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"id\":10,\"name\":\"Boulangerie\",\"userId\":42}", MediaType.APPLICATION_JSON));

    boolean isOwner = adapter.isStoreOwnedBy(10L, 99L);

    assertThat(isOwner).isFalse();
    mockServer.verify();
  }

  @Test
  @DisplayName("Should return false when Store Service returns 404 Not Found")
  void shouldReturnFalseWhenStoreNotFound() {
    mockServer
        .expect(
            org.springframework.test.web.client.ExpectedCount.twice(),
            requestTo("http://store-service:8080/v1/stores/999"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    boolean exists = adapter.storeExists(999L);
    boolean isOwner = adapter.isStoreOwnedBy(999L, 42L);

    assertThat(exists).isFalse();
    assertThat(isOwner).isFalse();
    mockServer.verify();
  }

  @Test
  @DisplayName("Should gracefully handle Store Service 500 server error (fail-safe)")
  void shouldHandleServerErrorGracefully() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/10"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    boolean exists = adapter.storeExists(10L);

    assertThat(exists).isFalse();
    mockServer.verify();
  }
}
