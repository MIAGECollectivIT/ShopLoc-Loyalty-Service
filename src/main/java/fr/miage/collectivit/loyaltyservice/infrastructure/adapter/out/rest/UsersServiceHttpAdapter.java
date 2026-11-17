package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest;

import fr.miage.collectivit.loyaltyservice.domain.exception.UsersServiceUnavailableException;
import fr.miage.collectivit.loyaltyservice.domain.port.out.UserVehiclesPort;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.out.rest.dto.UserVehicleDto;
import fr.miage.collectivit.loyaltyservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.loyaltyservice.security.ServiceTokenProvider;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Outbound secondary adapter querying ShopLoc-Users-Service for a user's registered vehicles
 * (SLE-78 / SLE-46).
 */
@Slf4j
@Component
@SuppressWarnings("PMD.LawOfDemeter")
public class UsersServiceHttpAdapter implements UserVehiclesPort {

  public static final String USER_VEHICLES_PATH = "/v1/users/{userId}/vehicles";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final int CONNECT_TIMEOUT_SECONDS = 2;
  private static final int READ_TIMEOUT_SECONDS = 3;

  @Setter private RestClient restClient;
  private final ServiceTokenProvider serviceTokenProvider;

  @Autowired
  public UsersServiceHttpAdapter(
      @Value("${services.users.url:http://users-service:8080}") String usersServiceUrl,
      @Autowired(required = false) ServiceTokenProvider serviceTokenProvider) {
    this(
        RestClient.builder()
            .baseUrl(usersServiceUrl)
            .requestFactory(createRequestFactory())
            .build(),
        serviceTokenProvider);
  }

  UsersServiceHttpAdapter(RestClient restClient) {
    this(restClient, null);
  }

  UsersServiceHttpAdapter(RestClient restClient, ServiceTokenProvider serviceTokenProvider) {
    this.restClient = restClient;
    this.serviceTokenProvider = serviceTokenProvider;
  }

  private static SimpleClientHttpRequestFactory createRequestFactory() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(Duration.ofSeconds(CONNECT_TIMEOUT_SECONDS));
    factory.setReadTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));
    return factory;
  }

  @Override
  public List<String> getUserLicensePlates(String userId) {
    if (userId == null || userId.isBlank()) {
      return Collections.emptyList();
    }
    String cleanUserId = userId.trim();
    try {
      Optional<String> bearerToken = resolveBearerToken();
      List<UserVehicleDto> vehicles =
          restClient
              .get()
              .uri(USER_VEHICLES_PATH, cleanUserId)
              .headers(
                  headers ->
                      bearerToken.ifPresent(
                          token -> headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + token)))
              .retrieve()
              .onStatus(
                  HttpStatusCode::is4xxClientError,
                  (request, response) -> handleClientError(response, cleanUserId))
              .onStatus(
                  HttpStatusCode::is5xxServerError,
                  (request, response) -> handleServerError(response, cleanUserId))
              .body(new ParameterizedTypeReference<List<UserVehicleDto>>() {});

      if (vehicles == null) {
        return Collections.emptyList();
      }
      return vehicles.stream()
          .map(UserVehicleDto::licensePlate)
          .filter(Objects::nonNull)
          .filter(plate -> !plate.isBlank())
          .toList();
    } catch (UserVehiclesNotFoundException ex) {
      return Collections.emptyList();
    } catch (RestClientException ex) {
      if (log.isErrorEnabled()) {
        log.error(
            "Failed to communicate with Users Service for user {}: {}",
            cleanUserId,
            ex.getMessage());
      }
      throw new UsersServiceUnavailableException(
          "Unable to query user vehicles from Users Service: " + ex.getMessage(), ex);
    }
  }

  private void handleClientError(ClientHttpResponse response, String userId) throws IOException {
    if (response.getStatusCode().value() == HttpStatus.NOT_FOUND.value()) {
      if (log.isDebugEnabled()) {
        log.debug("User {} or vehicles not found on Users Service (404)", userId);
      }
      throw new UserVehiclesNotFoundException();
    }
    if (log.isWarnEnabled()) {
      log.warn(
          "Users Service returned 4xx error ({}) for user {}", response.getStatusCode(), userId);
    }
    throw new UsersServiceUnavailableException(
        "Users Service rejected vehicle lookup with status: " + response.getStatusCode());
  }

  private void handleServerError(ClientHttpResponse response, String userId) throws IOException {
    if (log.isErrorEnabled()) {
      log.error(
          "Users Service server error ({}) while fetching vehicles for user {}",
          response.getStatusCode(),
          userId);
    }
    throw new UsersServiceUnavailableException(
        "Users Service is temporarily unavailable (status: " + response.getStatusCode() + ")");
  }

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

  private static final class UserVehiclesNotFoundException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
