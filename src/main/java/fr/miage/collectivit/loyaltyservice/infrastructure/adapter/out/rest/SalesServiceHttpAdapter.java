package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import fr.miage.collectivit.loyaltyservice.domain.exception.SalesServiceUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.model.UserTransaction;
import fr.miage.collectivit.loyaltyservice.domain.port.out.SalesTransactionPort;
import fr.miage.collectivit.loyaltyservice.security.JwtAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Outbound secondary adapter connecting to ShopLoc-Sales-Service via REST (SLE-161). Communicates
 * with the Sales payments endpoint, forwarding the caller's JWT token for secure extraction.
 */
@Slf4j
@Component
public class SalesServiceHttpAdapter implements SalesTransactionPort {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final int CONNECT_TIMEOUT_SECONDS = 2;
  private static final int READ_TIMEOUT_SECONDS = 3;

  private final RestClient restClient;
  private final fr.miage.collectivit.loyaltyservice.security.ServiceTokenProvider
      serviceTokenProvider;

  @Autowired
  public SalesServiceHttpAdapter(
      @Value("${services.sales-service.url:http://sales-service:8080}") String salesServiceUrl,
      @Autowired(required = false)
          fr.miage.collectivit.loyaltyservice.security.ServiceTokenProvider serviceTokenProvider) {
    this(
        RestClient.builder()
            .baseUrl(salesServiceUrl)
            .requestFactory(createClientHttpRequestFactory())
            .build(),
        serviceTokenProvider);
  }

  SalesServiceHttpAdapter(RestClient restClient) {
    this(restClient, null);
  }

  SalesServiceHttpAdapter(
      RestClient restClient,
      fr.miage.collectivit.loyaltyservice.security.ServiceTokenProvider serviceTokenProvider) {
    this.restClient = restClient;
    this.serviceTokenProvider = serviceTokenProvider;
  }

  @SuppressWarnings("PMD.CloseResource")
  private static ClientHttpRequestFactory createClientHttpRequestFactory() {
    HttpClient httpClient =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS))
            .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));
    return factory;
  }

  @Override
  public List<UserTransaction> getApprovedTransactions(String userId, Instant from, Instant to) {
    if (userId == null || userId.isBlank()) {
      return Collections.emptyList();
    }

    try {
      Optional<String> bearerToken = resolveCurrentBearerToken();

      List<UserTransaction> transactions =
          restClient
              .get()
              .uri(
                  uriBuilder ->
                      uriBuilder
                          .path("/v1/sales/payments/users/{userId}/transactions")
                          .queryParam("from", from.toString())
                          .queryParam("to", to.toString())
                          .build(userId))
              .headers(
                  headers ->
                      bearerToken.ifPresent(
                          token -> headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)))
              .retrieve()
              .onStatus(
                  HttpStatusCode::is4xxClientError, (req, res) -> handleClientError(res, userId))
              .onStatus(
                  HttpStatusCode::is5xxServerError, (req, res) -> handleServerError(res, userId))
              .body(new ParameterizedTypeReference<List<UserTransaction>>() {});

      return transactions != null ? transactions : Collections.emptyList();
    } catch (UserTransactionsNotFoundException ex) {
      return Collections.emptyList();
    } catch (RestClientException ex) {
      if (log.isErrorEnabled()) {
        log.error(
            "Failed to communicate with Sales Service for user {}: {}", userId, ex.getMessage());
      }
      throw new SalesServiceUnavailableException(
          "Unable to query user transactions from Sales Service: " + ex.getMessage(), ex);
    }
  }

  private static final class UserTransactionsNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }

  @SuppressWarnings("PMD.LawOfDemeter")
  private void handleClientError(ClientHttpResponse response, String userId) throws IOException {
    if (response.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
      if (log.isDebugEnabled()) {
        log.debug("No transactions found for user {} on Sales Service (404)", userId);
      }
      throw new UserTransactionsNotFoundException();
    }
    if (log.isWarnEnabled()) {
      log.warn(
          "Sales Service returned 4xx error ({}) for user {}", response.getStatusCode(), userId);
    }
    throw new SalesServiceUnavailableException(
        "Sales service rejected request with status: " + response.getStatusCode());
  }

  @SuppressWarnings("PMD.LawOfDemeter")
  private void handleServerError(ClientHttpResponse response, String userId) throws IOException {
    if (log.isErrorEnabled()) {
      log.error(
          "Sales Service server error ({}) while fetching transactions for user {}",
          response.getStatusCode(),
          userId);
    }
    throw new SalesServiceUnavailableException(
        "Sales Service is temporarily unavailable (status: " + response.getStatusCode() + ")");
  }

  @SuppressWarnings("PMD.LawOfDemeter")
  private Optional<String> resolveCurrentBearerToken() {
    ServletRequestAttributes attrs =
        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
    if (attrs != null) {
      HttpServletRequest request = attrs.getRequest();
      String rawToken = (String) request.getAttribute(JwtAuthInterceptor.ATTR_RAW_TOKEN);
      if (rawToken != null && !rawToken.isBlank()) {
        return Optional.of(rawToken);
      }
      String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
      if (authHeader != null
          && authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
        return Optional.of(authHeader.substring(BEARER_PREFIX.length()).trim());
      }
    }
    if (serviceTokenProvider != null) {
      String serviceToken = serviceTokenProvider.generateAdminServiceToken();
      if (serviceToken != null && !serviceToken.isBlank()) {
        return Optional.of(serviceToken);
      }
    }
    return Optional.empty();
  }
}
