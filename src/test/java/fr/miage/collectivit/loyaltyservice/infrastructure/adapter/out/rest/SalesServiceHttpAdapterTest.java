package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import fr.miage.collectivit.loyaltyservice.domain.exception.SalesServiceUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.model.UserTransaction;
import java.time.Instant;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@DisplayName("Secondary Adapter - SalesServiceHttpAdapter Test (SLE-161)")
class SalesServiceHttpAdapterTest {

  private MockRestServiceServer mockServer;
  private SalesServiceHttpAdapter adapter;

  @BeforeEach
  void setUp() {
    RestClient.Builder builder = RestClient.builder().baseUrl("http://sales-service:8080");
    mockServer = MockRestServiceServer.bindTo(builder).build();
    adapter = new SalesServiceHttpAdapter(builder.build());
  }

  @Test
  @DisplayName("Should return transactions when Sales Service responds with 200 OK")
  void shouldReturnTransactionsOnSuccess() {
    Instant from = Instant.parse("2026-10-30T00:00:00Z");
    Instant to = Instant.parse("2026-11-14T12:00:00Z");
    String userId = "USER-123";

    String jsonResponse =
        """
        [
          {
            "transactionId": "TXN-1",
            "reference": "ORD-1",
            "amount": 29.90,
            "currency": "EUR",
            "status": "TRANSACTION_APPROVED",
            "authorizationCode": "AUTH-1",
            "createdAt": "2026-11-05T10:00:00Z"
          },
          {
            "transactionId": "TXN-2",
            "reference": "ORD-2",
            "amount": 14.50,
            "currency": "EUR",
            "status": "TRANSACTION_APPROVED",
            "authorizationCode": "AUTH-2",
            "createdAt": "2026-11-10T15:30:00Z"
          }
        ]
        """;

    mockServer
        .expect(
            requestTo(
                Matchers.startsWith(
                    "http://sales-service:8080/v1/sales/payments/users/USER-123/transactions")))
        .andExpect(method(HttpMethod.GET))
        .andExpect(queryParam("from", from.toString()))
        .andExpect(queryParam("to", to.toString()))
        .andRespond(withSuccess(jsonResponse, MediaType.APPLICATION_JSON));

    List<UserTransaction> result = adapter.getApprovedTransactions(userId, from, to);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).transactionId()).isEqualTo("TXN-1");
    assertThat(result.get(0).status()).isEqualTo("TRANSACTION_APPROVED");
    assertThat(result.get(1).transactionId()).isEqualTo("TXN-2");
    mockServer.verify();
  }

  @Test
  @DisplayName("Should return empty list when Sales Service returns 404 Not Found")
  void shouldReturnEmptyListWhenNotFound() {
    Instant from = Instant.parse("2026-10-30T00:00:00Z");
    Instant to = Instant.parse("2026-11-14T12:00:00Z");

    mockServer
        .expect(
            requestTo(
                Matchers.startsWith(
                    "http://sales-service:8080/v1/sales/payments/users/USER-UNKNOWN/transactions")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withStatus(HttpStatus.NOT_FOUND));

    List<UserTransaction> result = adapter.getApprovedTransactions("USER-UNKNOWN", from, to);

    assertThat(result).isEmpty();
    mockServer.verify();
  }

  @Test
  @DisplayName("Should throw SalesServiceUnavailableException on 500 Server Error")
  void shouldThrowOnServerError() {
    Instant from = Instant.parse("2026-10-30T00:00:00Z");
    Instant to = Instant.parse("2026-11-14T12:00:00Z");

    mockServer
        .expect(
            requestTo(
                Matchers.startsWith(
                    "http://sales-service:8080/v1/sales/payments/users/USER-ERR/transactions")))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    assertThatThrownBy(() -> adapter.getApprovedTransactions("USER-ERR", from, to))
        .isInstanceOf(SalesServiceUnavailableException.class)
        .hasMessageContaining("Sales Service is temporarily unavailable");

    mockServer.verify();
  }

  @Test
  @DisplayName("Should return empty list directly if userId is null or empty")
  void shouldReturnEmptyWhenUserIdBlank() {
    Instant from = Instant.now();
    Instant to = Instant.now();

    assertThat(adapter.getApprovedTransactions(null, from, to)).isEmpty();
    assertThat(adapter.getApprovedTransactions("   ", from, to)).isEmpty();
  }
}
