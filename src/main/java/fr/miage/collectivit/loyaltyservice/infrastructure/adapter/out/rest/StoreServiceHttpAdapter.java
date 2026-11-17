package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import fr.miage.collectivit.loyaltyservice.domain.port.out.StoreVerificationPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto.StoreDto;
import fr.miage.collectivit.loyaltyservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.loyaltyservice.security.ServiceTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Outbound hexagonal adapter (Secondary Adapter) querying Store Service via HTTP REST to validate
 * store existence and merchant ownership.
 */
@Slf4j
@Component
public class StoreServiceHttpAdapter implements StoreVerificationPort {

  private static final String BEARER_PREFIX = "Bearer ";
  private static final int CONNECT_TIMEOUT_SECONDS = 2;
  private static final int READ_TIMEOUT_SECONDS = 3;

  private final RestClient restClient;
  private final ServiceTokenProvider serviceTokenProvider;

  @Autowired
  public StoreServiceHttpAdapter(
      @Value("${services.store-service.url:http://store-service:8080}") String storeServiceUrl,
      @Autowired(required = false) ServiceTokenProvider serviceTokenProvider) {
    this(
        RestClient.builder()
            .baseUrl(storeServiceUrl)
            .requestFactory(createClientHttpRequestFactory())
            .build(),
        serviceTokenProvider);
  }

  StoreServiceHttpAdapter(RestClient restClient) {
    this(restClient, null);
  }

  StoreServiceHttpAdapter(RestClient restClient, ServiceTokenProvider serviceTokenProvider) {
    this.restClient = restClient;
    this.serviceTokenProvider = serviceTokenProvider;
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

  @Override
  public Optional<String> findStoreName(Long storeId) {
    return fetchStore(storeId).map(StoreDto::name);
  }

  @Override
  public Optional<String> findStoreCategory(Long storeId) {
    return fetchStore(storeId).map(StoreDto::category);
  }

  private Optional<StoreDto> fetchStore(Long storeId) {
    try {
      Optional<String> bearerToken = resolveBearerToken();
      StoreDto store =
          restClient
              .get()
              .uri("/v1/stores/{storeId}", storeId)
              .headers(
                  headers ->
                      bearerToken.ifPresent(
                          token -> headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)))
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

  @SuppressWarnings("PMD.LawOfDemeter")
  private Optional<String> resolveBearerToken() {
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
