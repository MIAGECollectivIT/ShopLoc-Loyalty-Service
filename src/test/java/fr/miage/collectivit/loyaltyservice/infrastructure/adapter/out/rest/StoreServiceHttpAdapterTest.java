package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import java.util.UUID;
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

  private static final UUID STORE_UID = UUID.fromString("550e8400-e29b-41d4-a716-446655440010");
  private static final UUID UNKNOWN_STORE_UID =
      UUID.fromString("550e8400-e29b-41d4-a716-446655440999");
  private static final UUID OWNER_UID = UUID.fromString("550e8400-e29b-41d4-a716-446655440042");

  private MockRestServiceServer mockServer;
  private StoreServiceHttpAdapter adapter;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://store-service:8080");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    adapter = new StoreServiceHttpAdapter(builder.build());
  }

  @Test
  @DisplayName("Should return owner UUID when store exists on Store Service")
  void shouldReturnOwnerUidWhenStoreExists() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/" + STORE_UID))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"uid\":\""
                    + STORE_UID
                    + "\",\"name\":\"Boulangerie\",\"ownerUid\":\""
                    + OWNER_UID
                    + "\"}",
                MediaType.APPLICATION_JSON));

    Optional<UUID> ownerUid = adapter.findStoreOwnerUid(STORE_UID);

    assertThat(ownerUid).contains(OWNER_UID);
    mockServer.verify();
  }

  @Test
  @DisplayName("Should return store name when store exists on Store Service")
  void shouldReturnStoreNameWhenStoreExists() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/" + STORE_UID))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"uid\":\""
                    + STORE_UID
                    + "\",\"name\":\"Boulangerie\",\"category\":\"Alimentation\",\"ownerUid\":\""
                    + OWNER_UID
                    + "\"}",
                MediaType.APPLICATION_JSON));

    Optional<String> storeName = adapter.findStoreName(STORE_UID);

    assertThat(storeName).contains("Boulangerie");
    mockServer.verify();
  }

  @Test
  @DisplayName("Should return store category when store exists on Store Service")
  void shouldReturnStoreCategoryWhenStoreExists() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/" + STORE_UID))
        .andExpect(method(HttpMethod.GET))
        .andRespond(
            withSuccess(
                "{\"uid\":\""
                    + STORE_UID
                    + "\",\"name\":\"Boulangerie\",\"category\":\"Alimentation\",\"ownerUid\":\""
                    + OWNER_UID
                    + "\"}",
                MediaType.APPLICATION_JSON));

    Optional<String> category = adapter.findStoreCategory(STORE_UID);

    assertThat(category).contains("Alimentation");
    mockServer.verify();
  }

  @Test
  @DisplayName("Should return empty store name when Store Service returns 404 Not Found")
  void shouldReturnEmptyStoreNameWhenStoreNotFound() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/" + UNKNOWN_STORE_UID))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    Optional<String> storeName = adapter.findStoreName(UNKNOWN_STORE_UID);

    assertThat(storeName).isEmpty();
    mockServer.verify();
  }

  @Test
  @DisplayName("Should return empty when Store Service returns 404 Not Found")
  void shouldReturnEmptyWhenStoreNotFound() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/" + UNKNOWN_STORE_UID))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    Optional<UUID> ownerUid = adapter.findStoreOwnerUid(UNKNOWN_STORE_UID);

    assertThat(ownerUid).isEmpty();
    mockServer.verify();
  }

  @Test
  @DisplayName("Should gracefully handle Store Service 500 server error (fail-safe)")
  void shouldHandleServerErrorGracefully() {
    mockServer
        .expect(requestTo("http://store-service:8080/v1/stores/" + STORE_UID))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    Optional<UUID> ownerUid = adapter.findStoreOwnerUid(STORE_UID);

    assertThat(ownerUid).isEmpty();
    mockServer.verify();
  }
}
