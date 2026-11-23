package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import fr.miage.collectivit.loyaltyservice.domain.exception.UsersServiceUnavailableException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Secondary Adapter - UsersServiceHttpAdapter Test (SLE-78)")
class UsersServiceHttpAdapterTest {

  private MockRestServiceServer mockServer;
  private UsersServiceHttpAdapter adapter;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://users-service:8080");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    adapter = new UsersServiceHttpAdapter(builder.build());
  }

  @Test
  @DisplayName("Should return registered license plates when Users Service responds 200 OK")
  void shouldReturnPlatesOnSuccess() {
    String json =
        """
        [
          {"vehicleId": "AA-123-BB", "licensePlate": "AA-123-BB"},
          {"vehicleId": "CC-456-DD", "licensePlate": "CC-456-DD"}
        ]
        """;

    mockServer
        .expect(requestTo("http://users-service:8080/v1/users/user-1/vehicles"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

    List<String> plates = adapter.getUserLicensePlates("user-1");

    assertThat(plates).containsExactly("AA-123-BB", "CC-456-DD");
    mockServer.verify();
  }

  @Test
  @DisplayName("Should return empty list when Users Service responds 404")
  void shouldReturnEmptyListOn404() {
    mockServer
        .expect(requestTo("http://users-service:8080/v1/users/user-missing/vehicles"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    List<String> plates = adapter.getUserLicensePlates("user-missing");

    assertThat(plates).isEmpty();
    mockServer.verify();
  }

  @Test
  @DisplayName("Should throw UsersServiceUnavailableException on 500 Server Error")
  void shouldThrowOnServerError() {
    mockServer
        .expect(requestTo("http://users-service:8080/v1/users/user-err/vehicles"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    assertThatThrownBy(() -> adapter.getUserLicensePlates("user-err"))
        .isInstanceOf(UsersServiceUnavailableException.class);
    mockServer.verify();
  }
}
