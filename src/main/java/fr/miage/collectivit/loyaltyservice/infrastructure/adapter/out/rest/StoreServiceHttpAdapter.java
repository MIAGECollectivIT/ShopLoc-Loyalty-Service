package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto.StoreDto;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
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

  private final RestClient restClient;

  @org.springframework.beans.factory.annotation.Autowired
  public StoreServiceHttpAdapter(
      @Value("${services.store-service.url:http://store-service:8080}") String storeServiceUrl) {
    this(RestClient.builder().baseUrl(storeServiceUrl).build());
  }

  StoreServiceHttpAdapter(RestClient restClient) {
    this.restClient = restClient;
  }

  @Override
  public boolean isStoreOwnedBy(Long storeId, Long userId) {
    return fetchStore(storeId)
        .map(store -> store.userId() != null && store.userId().equals(userId))
        .orElse(false);
  }

  @Override
  public boolean storeExists(Long storeId) {
    return fetchStore(storeId).isPresent();
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
