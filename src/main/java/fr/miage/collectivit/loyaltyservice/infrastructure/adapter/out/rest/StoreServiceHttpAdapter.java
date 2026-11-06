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
 * Adaptateur hexagonal sortant (Secondary Adapter) interrogeant Store Service via HTTP REST pour
 * valider l'existence et la propriété d'un commerce.
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
                          "Commerce {} introuvable sur Store Service (statut {})",
                          storeId,
                          response.getStatusCode());
                    }
                  })
              .body(StoreDto.class);
      return Optional.ofNullable(store);
    } catch (RestClientException ex) {
      if (log.isWarnEnabled()) {
        log.warn(
            "Impossible de vérifier le commerce {} auprès de Store Service : {}",
            storeId,
            ex.getMessage());
      }
      return Optional.empty();
    }
  }
}
