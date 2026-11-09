package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto.StoreDto;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Outbound hexagonal adapter (Secondary Adapter) querying Store Service via HTTP REST to validate
 * store existence and merchant ownership.
 */
@Slf4j
@Component
public class StoreServiceHttpAdapter implements StoreVerificationPort {

  private static final int CONNECT_TIMEOUT_SECONDS = 2;
  private static final int READ_TIMEOUT_SECONDS = 3;

  private final RestClient restClient;

  @org.springframework.beans.factory.annotation.Autowired
  public StoreServiceHttpAdapter(
      @Value("${services.store-service.url:http://store-service:8080}") String storeServiceUrl) {
    this(
        RestClient.builder()
            .baseUrl(storeServiceUrl)
            .requestFactory(createClientHttpRequestFactory())
            .build());
  }

  StoreServiceHttpAdapter(RestClient restClient) {
    this.restClient = restClient;
  }

  @SuppressWarnings("PMD.CloseResource")
  private static ClientHttpRequestFactory createClientHttpRequestFactory() {
    HttpClient httpClient =
        HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS)).build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));
    return factory;
  }

  @Override
  public Optional<Long> findStoreOwnerId(Long storeId) {
    return fetchStore(storeId).map(StoreDto::userId);
  }

  private Optional<StoreDto> fetchStore(Long storeId) {
    try {
      StoreDto store =
          restClient
              .get()
              .uri("/v1/stores/{storeId}", storeId)
              .retrieve()
              .onStatus(
                  HttpStatusCode::is4xxClientError,
                  (request, response) -> {
                    if (log.isDebugEnabled()) {
                      log.debug(
                          "Store {} not found on Store Service (status {})",
                          storeId,
                          response.getStatusCode());
                    }
                  })
              .body(StoreDto.class);
      return Optional.ofNullable(store);
    } catch (RestClientException ex) {
      if (log.isWarnEnabled()) {
        log.warn("Unable to verify store {} with Store Service: {}", storeId, ex.getMessage());
      }
      return Optional.empty();
    }
  }
}
