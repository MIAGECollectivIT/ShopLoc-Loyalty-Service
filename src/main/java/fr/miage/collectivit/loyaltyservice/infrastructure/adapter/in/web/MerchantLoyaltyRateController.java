package fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web;

import fr.miage.collectivit.loyaltyservice.config.OpenApiConfig;
import fr.miage.collectivit.loyaltyservice.domain.exception.MerchantLoyaltyRateNotFoundException;
import fr.miage.collectivit.loyaltyservice.domain.model.MerchantLoyaltyRate;
import fr.miage.collectivit.loyaltyservice.domain.port.in.ConfigureMerchantLoyaltyRateUseCase;
import fr.miage.collectivit.loyaltyservice.domain.port.in.GetMerchantLoyaltyRateQuery;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.MerchantLoyaltyRateRequest;
import fr.miage.collectivit.loyaltyservice.infrastructure.adapter.in.web.dto.MerchantLoyaltyRateResponse;
import fr.miage.collectivit.loyaltyservice.security.JwtAuthInterceptor;
import fr.miage.collectivit.loyaltyservice.security.RequiresRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Primary inbound hexagonal adapter (Primary/Driving Web Adapter) exposing REST endpoints for
 * querying and configuring merchant loyalty rates.
 */
@Validated
@RestController
@RequestMapping("/v1/loyalty/merchants/{storeId}/rates")
@Tag(
    name = "Merchant Loyalty Rates",
    description = "Gestion du barème de conversion euro/points des commerces")
@RequiredArgsConstructor
public class MerchantLoyaltyRateController {

  private final ConfigureMerchantLoyaltyRateUseCase configureRateUseCase;
  private final GetMerchantLoyaltyRateQuery getRateQuery;

  @GetMapping
  @Operation(
      summary = "Consulter le barème de fidélité d'un commerce",
      description =
          "Retourne le barème de conversion euro/points configuré pour un commerce (ou le barème par défaut 1€ = 1 pt si non personnalisé). Accès public.")
  @ApiResponses(
      value = {
        @ApiResponse(responseCode = "200", description = "Barème de fidélité récupéré avec succès")
      })
  public ResponseEntity<MerchantLoyaltyRateResponse> getRate(
      @Parameter(description = "Identifiant unique du commerce", example = "10")
          @PathVariable
          @Positive(message = "Store identifier must be positive")
          Long storeId) {
    try {
      MerchantLoyaltyRate rate = getRateQuery.getRateByStoreId(storeId);
      return ResponseEntity.ok(MerchantLoyaltyRateResponse.fromDomain(rate));
    } catch (MerchantLoyaltyRateNotFoundException ex) {
      return ResponseEntity.ok(MerchantLoyaltyRateResponse.defaultRate(storeId));
    }
  }

  @PutMapping
  @RequiresRole({"MERCHANT", "ADMIN"})
  @Operation(
      summary = "Configurer le barème de fidélité d'un commerce",
      description =
          "Définit ou met à jour le taux de conversion euro/points d'un commerce. Réservé au commerçant propriétaire du commerce ou à un administrateur.",
      security = @SecurityRequirement(name = OpenApiConfig.SECURITY_SCHEME_NAME))
  @ApiResponses(
      value = {
        @ApiResponse(
            responseCode = "200",
            description = "Barème de fidélité mis à jour avec succès"),
        @ApiResponse(responseCode = "400", description = "Montant ou points invalides"),
        @ApiResponse(
            responseCode = "401",
            description = "Non authentifié (jeton Bearer manquant ou invalide)"),
        @ApiResponse(
            responseCode = "403",
            description = "Accès refusé (rôle insuffisant ou non propriétaire du commerce)"),
        @ApiResponse(responseCode = "404", description = "Commerce introuvable")
      })
  public ResponseEntity<MerchantLoyaltyRateResponse> configureRate(
      @Parameter(description = "Identifiant unique du commerce", example = "10")
          @PathVariable
          @Positive(message = "Store identifier must be positive")
          Long storeId,
      @Valid @RequestBody MerchantLoyaltyRateRequest requestDto,
      HttpServletRequest request) {
    Long callerUserId = (Long) request.getAttribute(JwtAuthInterceptor.ATTR_USER_DB_ID);
    String callerRole = (String) request.getAttribute(JwtAuthInterceptor.ATTR_USER_ROLE);

    MerchantLoyaltyRate configuredRate =
        configureRateUseCase.configureRate(
            storeId, requestDto.euroAmount(), requestDto.pointsReward(), callerUserId, callerRole);

    return ResponseEntity.ok(MerchantLoyaltyRateResponse.fromDomain(configuredRate));
  }
}
